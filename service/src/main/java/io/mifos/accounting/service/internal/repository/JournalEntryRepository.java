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
package io.mifos.accounting.service.internal.repository;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.mapping.Mapper;
import com.datastax.driver.mapping.Result;
import io.mifos.core.cassandra.core.CassandraSessionProvider;
import io.mifos.core.cassandra.core.TenantAwareCassandraMapperProvider;
import io.mifos.core.cassandra.core.TenantAwareEntityTemplate;
import io.mifos.core.lang.DateConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@SuppressWarnings({"unused"})
@Repository
public class JournalEntryRepository {

  private final CassandraSessionProvider cassandraSessionProvider;
  private final TenantAwareCassandraMapperProvider tenantAwareCassandraMapperProvider;
  private final TenantAwareEntityTemplate tenantAwareEntityTemplate;

  @Autowired
  public JournalEntryRepository(final CassandraSessionProvider cassandraSessionProvider,
                                final TenantAwareCassandraMapperProvider tenantAwareCassandraMapperProvider,
                                final TenantAwareEntityTemplate tenantAwareEntityTemplate) {
    super();
    this.cassandraSessionProvider = cassandraSessionProvider;
    this.tenantAwareCassandraMapperProvider = tenantAwareCassandraMapperProvider;
    this.tenantAwareEntityTemplate = tenantAwareEntityTemplate;
  }

  public void saveJournalEntry(final JournalEntryEntity journalEntryEntity) {
    this.tenantAwareEntityTemplate.save(journalEntryEntity);

    final JournalEntryLookup journalEntryLookup = new JournalEntryLookup();
    journalEntryLookup.setTransactionIdentifier(journalEntryEntity.getTransactionIdentifier());
    journalEntryLookup.setDateBucket(journalEntryEntity.getDateBucket());
    this.tenantAwareEntityTemplate.save(journalEntryLookup);
  }

  public List<JournalEntryEntity> fetchJournalEntries(final String dateBucketFrom, final String dateBucketTo) {
    final Session tenantSession = this.cassandraSessionProvider.getTenantSession();

    LocalDate start = LocalDate.parse(dateBucketFrom);
    final LocalDate end = LocalDate.parse(dateBucketTo);
    final List<String> datesInBetweenRange = new ArrayList<>(Math.toIntExact(ChronoUnit.DAYS.between(start, end)));

    while (!start.isAfter(end)) {
      datesInBetweenRange.add(DateConverter.toIsoString(start));
      start = start.plusDays(1);
    }

    final ResultSet resultSet = tenantSession.execute(QueryBuilder
            .select()
            .all()
            .from("thoth_journal_entries")
            .where(QueryBuilder.in("date_bucket", datesInBetweenRange))
            .getQueryString(), datesInBetweenRange.toArray()
    );

    final Mapper<JournalEntryEntity> mapper = this.tenantAwareCassandraMapperProvider.getMapper(JournalEntryEntity.class);
    final Result<JournalEntryEntity> journalEntryEntities = mapper.map(resultSet);
    return journalEntryEntities.all();
  }

  public Optional<JournalEntryEntity> findJournalEntry(final String transactionIdentifier) {
    final Optional<JournalEntryLookup> optionalJournalEntryLookup = this.tenantAwareEntityTemplate.findById(JournalEntryLookup.class, transactionIdentifier);
    if (optionalJournalEntryLookup.isPresent()) {
      final JournalEntryLookup journalEntryLookup = optionalJournalEntryLookup.get();
      final List<JournalEntryEntity> journalEntryEntities = this.tenantAwareEntityTemplate.fetchByKeys(JournalEntryEntity.class, journalEntryLookup.getDateBucket(),
          journalEntryLookup.getTransactionIdentifier());
      return Optional.of(journalEntryEntities.get(0));
    } else {
      return Optional.empty();
    }
  }
}
