/*
 * Copyright 2017 The Mifos Initiative.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.mifos.accounting.service.internal.command.handler;

import com.datastax.driver.core.DataType;
import com.datastax.driver.core.schemabuilder.SchemaBuilder;
import io.mifos.accounting.api.v1.EventConstants;
import io.mifos.accounting.service.internal.command.InitializeServiceCommand;
import io.mifos.core.cassandra.core.CassandraJourney;
import io.mifos.core.cassandra.core.CassandraJourneyFactory;
import io.mifos.core.cassandra.core.CassandraJourneyRoute;
import io.mifos.core.cassandra.core.CassandraSessionProvider;
import io.mifos.core.command.annotation.Aggregate;
import io.mifos.core.command.annotation.CommandHandler;
import io.mifos.core.command.annotation.CommandLogLevel;
import io.mifos.core.command.annotation.EventEmitter;
import io.mifos.core.mariadb.domain.FlywayFactoryBean;
import org.springframework.beans.factory.annotation.Autowired;

import javax.sql.DataSource;

@SuppressWarnings({
    "unused"
})
@Aggregate
public class MigrationCommandHandler {

  private final DataSource dataSource;
  private final FlywayFactoryBean flywayFactoryBean;
  private final CassandraSessionProvider cassandraSessionProvider;
  private final CassandraJourneyFactory cassandraJourneyFactory;

  @SuppressWarnings("SpringJavaAutowiringInspection")
  @Autowired
  public MigrationCommandHandler(final DataSource dataSource,
                                 final FlywayFactoryBean flywayFactoryBean,
                                 final CassandraSessionProvider cassandraSessionProvider,
                                 final CassandraJourneyFactory cassandraJourneyFactory) {
    super();
    this.dataSource = dataSource;
    this.flywayFactoryBean = flywayFactoryBean;
    this.cassandraSessionProvider = cassandraSessionProvider;
    this.cassandraJourneyFactory = cassandraJourneyFactory;
  }

  @CommandHandler(logStart = CommandLogLevel.DEBUG, logFinish = CommandLogLevel.DEBUG)
  @EventEmitter(selectorName = EventConstants.SELECTOR_NAME, selectorValue = EventConstants.INITIALIZE)
  public String initialize(final InitializeServiceCommand initializeServiceCommand) {
    this.flywayFactoryBean.create(this.dataSource).migrate();

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

    final CassandraJourney cassandraJourney = this.cassandraJourneyFactory.create(this.cassandraSessionProvider);
    cassandraJourney.start(initialRoute);
    cassandraJourney.start(updateRouteVersion2);

    return versionNumber;
  }
}