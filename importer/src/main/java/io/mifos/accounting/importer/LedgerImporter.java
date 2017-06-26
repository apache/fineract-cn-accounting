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
package io.mifos.accounting.importer;

import io.mifos.accounting.api.v1.client.LedgerAlreadyExistsException;
import io.mifos.accounting.api.v1.client.LedgerManager;
import io.mifos.accounting.api.v1.domain.Ledger;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * @author Myrle Krantz
 */
@SuppressWarnings("unused")
public class LedgerImporter {
  private static final String IDENTIFIER_COLUMN = "identifier";
  private static final String PARENT_IDENTIFIER_COLUMN = "parentIdentifier";
  private static final String TYPE_COLUMN = "type";
  private static final String NAME_COLUMN = "name";
  private static final String SHOW_ACCOUNTS_IN_CHART_COLUMN = "show";
  private static final String DESCRIPTION_COLUMN = "description";

  private final LedgerManager ledgerManager;
  private final Logger logger;

  public LedgerImporter(final LedgerManager ledgerManager, final Logger logger) {
    this.ledgerManager = ledgerManager;
    this.logger = logger;
  }

  public void importCSV(final URL toImport) throws IOException {
    final CSVParser parser = CSVParser.parse(toImport, StandardCharsets.UTF_8, CSVFormat.RFC4180.withHeader());
    final List<RecordFromLineNumber<Ledger>> ledgerList = StreamSupport.stream(parser.spliterator(), false)
            .map(this::toLedger)
            .collect(Collectors.toList()); //File should fully parse, correctly, before we begin creating ledgers/accounts.

    ledgerList.forEach(this::createLedger);
  }

  private void createLedger(final RecordFromLineNumber<Ledger> toCreate) {
    try {
      final Ledger ledger = toCreate.getRecord();
      if (ledger.getParentLedgerIdentifier() == null) {
        ledgerManager.createLedger(toCreate.getRecord());
      } else {
        ledgerManager.addSubLedger(ledger.getParentLedgerIdentifier(), ledger);
      }
      try {
        Thread.sleep(1000l);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    catch (final LedgerAlreadyExistsException ignored) {
      final Ledger ledger = ledgerManager.findLedger(toCreate.getRecord().getIdentifier());
      if ((!Objects.equals(ledger.getIdentifier(), toCreate.getRecord().getIdentifier())) ||
              (!Objects.equals(ledger.getName(), toCreate.getRecord().getName())) ||
              (!Objects.equals(ledger.getType(), toCreate.getRecord().getType())) ||
              (!Objects.equals(ledger.getDescription(), toCreate.getRecord().getDescription())) ||
              (!Objects.equals(ledger.getParentLedgerIdentifier(), toCreate.getRecord().getParentLedgerIdentifier())) ||
              (!Objects.equals(ledger.getShowAccountsInChart(), toCreate.getRecord().getShowAccountsInChart())))
      {
        logger.error("Creation of ledger {} failed, because a ledger with the same identifier but different properties already exists {}", toCreate.getRecord(), ledger);
      }
    }
  }

  private RecordFromLineNumber<Ledger> toLedger(final CSVRecord csvRecord) {
    try {
      final String identifier = csvRecord.get(IDENTIFIER_COLUMN);
      final String parentLedger = csvRecord.get(PARENT_IDENTIFIER_COLUMN);
      final String type = csvRecord.get(TYPE_COLUMN);
      String name;
      try {
        name = csvRecord.get(NAME_COLUMN);
      }
      catch (final IllegalArgumentException e) {
        name = identifier;
      }
      final boolean show = Boolean.valueOf(csvRecord.get(SHOW_ACCOUNTS_IN_CHART_COLUMN));
      final String description = csvRecord.get(DESCRIPTION_COLUMN);

      final Ledger ledger = new Ledger();
      ledger.setIdentifier(identifier);
      ledger.setType(type);
      ledger.setName(name);
      ledger.setShowAccountsInChart(show);
      ledger.setDescription(description);
      if (parentLedger != null && !parentLedger.isEmpty())
        ledger.setParentLedgerIdentifier(parentLedger);

      return new RecordFromLineNumber<>(csvRecord.getRecordNumber(), ledger);
    }
    catch (final IllegalArgumentException e) {
      logger.warn("Parsing failed on record {}", csvRecord.getRecordNumber());
      throw e;
    }
  }
}
