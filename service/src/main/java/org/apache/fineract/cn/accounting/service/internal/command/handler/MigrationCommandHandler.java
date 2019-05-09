/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.cn.accounting.service.internal.command.handler;

import com.datastax.driver.core.DataType;
import com.datastax.driver.core.schemabuilder.SchemaBuilder;
import org.apache.fineract.cn.accounting.api.v1.EventConstants;
import org.apache.fineract.cn.accounting.service.ServiceConstants;
import org.apache.fineract.cn.accounting.service.internal.command.InitializeServiceCommand;
import org.apache.fineract.cn.accounting.service.internal.repository.AccountRepository;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import javax.sql.DataSource;
import org.apache.fineract.cn.cassandra.core.CassandraJourney;
import org.apache.fineract.cn.cassandra.core.CassandraJourneyFactory;
import org.apache.fineract.cn.cassandra.core.CassandraJourneyRoute;
import org.apache.fineract.cn.cassandra.core.CassandraSessionProvider;
import org.apache.fineract.cn.command.annotation.Aggregate;
import org.apache.fineract.cn.command.annotation.CommandHandler;
import org.apache.fineract.cn.command.annotation.CommandLogLevel;
import org.apache.fineract.cn.command.annotation.EventEmitter;
import org.apache.fineract.cn.postgresql.domain.FlywayFactoryBean;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationInfoService;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.annotation.Transactional;

@SuppressWarnings({
    "unused"
})
@Aggregate
public class MigrationCommandHandler {

  private final Logger logger;
  private final DataSource dataSource;
  private final FlywayFactoryBean flywayFactoryBean;
  private final CassandraSessionProvider cassandraSessionProvider;
  private final CassandraJourneyFactory cassandraJourneyFactory;
  private final AccountRepository accountRepository;
  private final AccountCommandHandler accountCommandHandler;

  @SuppressWarnings("SpringJavaAutowiringInspection")
  @Autowired
  public MigrationCommandHandler(@Qualifier(ServiceConstants.LOGGER_NAME) final Logger logger,
                                 final DataSource dataSource,
                                 final FlywayFactoryBean flywayFactoryBean,
                                 final CassandraSessionProvider cassandraSessionProvider,
                                 final CassandraJourneyFactory cassandraJourneyFactory,
                                 final AccountRepository accountRepository,
                                 final AccountCommandHandler accountCommandHandler) {
    super();
    this.logger = logger;
    this.dataSource = dataSource;
    this.flywayFactoryBean = flywayFactoryBean;
    this.cassandraSessionProvider = cassandraSessionProvider;
    this.cassandraJourneyFactory = cassandraJourneyFactory;
    this.accountRepository = accountRepository;
    this.accountCommandHandler = accountCommandHandler;
  }

  @Transactional
  @CommandHandler(logStart = CommandLogLevel.DEBUG, logFinish = CommandLogLevel.DEBUG)
  @EventEmitter(selectorName = EventConstants.SELECTOR_NAME, selectorValue = EventConstants.INITIALIZE)
  public String initialize(final InitializeServiceCommand initializeServiceCommand) {
    final Flyway flyway = this.flywayFactoryBean.create(this.dataSource);

    final MigrationInfoService migrationInfoService = flyway.info();
    final List<MigrationInfo> migrationInfoList = Arrays.asList(migrationInfoService.applied());
    final boolean shouldMigrateLedgerTotals = migrationInfoList
        .stream()
        .noneMatch(migrationInfo -> migrationInfo.getVersion().getVersion().equals("9"));

    flyway.migrate();

    final String versionNumber = "1";

    final CassandraJourneyRoute initialRoute = CassandraJourneyRoute
        .plan("1")
        .addWaypoint(
            SchemaBuilder
                .createType("thoth_debtor")
                .addColumn("account_number", DataType.text())
                .addColumn("amount", DataType.cdouble())
                .buildInternal())
        .addWaypoint(
            SchemaBuilder
                .createType("thoth_creditor")
                .addColumn("account_number", DataType.text())
                .addColumn("amount", DataType.cdouble())
                .buildInternal())
        .addWaypoint(
            SchemaBuilder
                .createTable("thoth_journal_entries")
                .addPartitionKey("date_bucket", DataType.text())
                .addClusteringColumn("transaction_identifier", DataType.text())
                .addColumn("transaction_date", DataType.timestamp())
                .addColumn("clerk", DataType.text())
                .addColumn("note", DataType.text())
                .addUDTSetColumn("debtors", SchemaBuilder.frozen("thoth_debtor"))
                .addUDTSetColumn("creditors", SchemaBuilder.frozen("thoth_creditor"))
                .addColumn("state", DataType.text())
                .addColumn("message", DataType.text())
                .buildInternal())
        .addWaypoint(SchemaBuilder
            .createTable("thoth_journal_entry_lookup")
            .addPartitionKey("transaction_identifier", DataType.text())
            .addColumn("date_bucket", DataType.text())
            .buildInternal())
        .build();


    final CassandraJourneyRoute updateRouteVersion2 = CassandraJourneyRoute
        .plan("2")
        .addWaypoint(
            SchemaBuilder
                .alterTable("thoth_journal_entries")
                .addColumn("transaction_type").type(DataType.text())
                .getQueryString()
        )
        .build();

    final CassandraJourneyRoute updateRouteVersion3 = CassandraJourneyRoute
        .plan("3")
        .addWaypoint(
            SchemaBuilder
                .alterTable("thoth_journal_entries")
                .addColumn("created_by").type(DataType.text())
                .getQueryString()
        )
        .addWaypoint(
            SchemaBuilder
                .alterTable("thoth_journal_entries")
                .addColumn("created_on").type(DataType.timestamp())
                .getQueryString()
        )
        .build();

    final CassandraJourney cassandraJourney = this.cassandraJourneyFactory.create(this.cassandraSessionProvider);
    cassandraJourney.start(initialRoute);
    cassandraJourney.start(updateRouteVersion2);
    cassandraJourney.start(updateRouteVersion3);

    if (shouldMigrateLedgerTotals) {
      this.migrateLedgerTotals();
    }

    return versionNumber;
  }

  public void migrateLedgerTotals() {
    this.logger.info("Start ledger total migration ...");

    this.accountRepository.findByBalanceIsNot(0.00D).forEach(accountEntity ->
      this.accountCommandHandler.adjustLedgerTotals(accountEntity.getLedger().getIdentifier(),
          BigDecimal.valueOf(accountEntity.getBalance()))
    );
  }
}