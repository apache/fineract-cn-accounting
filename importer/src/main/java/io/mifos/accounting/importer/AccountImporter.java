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

import io.mifos.accounting.api.v1.client.AccountAlreadyExistsException;
import io.mifos.accounting.api.v1.client.LedgerManager;
import io.mifos.accounting.api.v1.domain.Account;
import io.mifos.accounting.api.v1.domain.Ledger;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * @author Myrle Krantz
 */
@SuppressWarnings("unused")
public class AccountImporter {
  private static final String TYPE_COLUMN = "type";
  private static final String IDENTIFIER_COLUMN = "identifier";
  private static final String NAME_COLUMN = "name";
  private static final String PARENT_IDENTIFIER_COLUMN = "parentIdentifier";
  private static final String HOLDERS_COLUMN = "holders";
  private static final String AUTHORITIES_COLUMN = "authorities";
  private static final String BALANCE_COLUMN = "balance";

  private final LedgerManager ledgerManager;
  private final Logger logger;

  public AccountImporter(final LedgerManager ledgerManager, final Logger logger) {
    this.ledgerManager = ledgerManager;
    this.logger = logger;
  }

  public void importCSV(final URL toImport) throws IOException {
    final CSVParser parser = CSVParser.parse(toImport, StandardCharsets.UTF_8, CSVFormat.RFC4180.withHeader());
    final List<RecordFromLineNumber<Account>> ledgerList = StreamSupport.stream(parser.spliterator(), false)
            .map(this::toAccount)
            .collect(Collectors.toList()); //File should fully parse, correctly, before we begin creating ledgers/accounts.

    ledgerList.forEach(this::createAccount);
  }

  private void createAccount(final RecordFromLineNumber<Account> toCreate) {
    try {
      ledgerManager.createAccount(toCreate.getRecord());
    }
    catch (final AccountAlreadyExistsException ignored) {
      final Account account = ledgerManager.findAccount(toCreate.getRecord().getIdentifier());
      if ((!Objects.equals(account.getBalance(), toCreate.getRecord().getBalance())) ||
              (!Objects.equals(account.getIdentifier(), toCreate.getRecord().getIdentifier())) ||
              (!Objects.equals(account.getHolders(), toCreate.getRecord().getHolders())) ||
              (!Objects.equals(account.getLedger(), toCreate.getRecord().getLedger())) ||
              (!Objects.equals(account.getName(), toCreate.getRecord().getName())) ||
              (!Objects.equals(account.getSignatureAuthorities(), toCreate.getRecord().getSignatureAuthorities())) ||
              (!Objects.equals(account.getType(), toCreate.getRecord().getType())))
      {
        logger.error("Creation of account {} failed, because an account with the same identifier but different properties already exists {}", toCreate.getRecord(), account);
      }
    }
  }

  private RecordFromLineNumber<Account> toAccount(final CSVRecord csvRecord) {
    try {
      final String ledgerIdentifier = csvRecord.get(PARENT_IDENTIFIER_COLUMN);
      String type;
      try {
        type = csvRecord.get(TYPE_COLUMN);
      }
      catch (final IllegalArgumentException e) {
        final Ledger ledger = ledgerManager.findLedger(ledgerIdentifier);
        type = ledger.getType();
      }
      final String identifier = csvRecord.get(IDENTIFIER_COLUMN);
      String name;
      try {
        name = csvRecord.get(NAME_COLUMN);
      }
      catch (final IllegalArgumentException e) {
        name = identifier;
      }
      Set<String> holders;
      try {
        holders = new HashSet<>(Arrays.asList(csvRecord.get(HOLDERS_COLUMN).split("/")));
      }
      catch (final IllegalArgumentException e) {
        holders = Collections.emptySet();
      }
      Set<String> authorities;
      try {
        authorities = new HashSet<>(Arrays.asList(csvRecord.get(AUTHORITIES_COLUMN).split("/")));
      }
      catch (final IllegalArgumentException e) {
        authorities = Collections.emptySet();
      }
      Double balance;
      try {
        balance = Double.valueOf(csvRecord.get(BALANCE_COLUMN));
      }
      catch (final IllegalArgumentException e) {
        balance = 0.0;
      }

      final Account account = new Account();

      account.setType(type);
      account.setIdentifier(identifier);
      account.setName(name);
      account.setHolders(holders);
      account.setSignatureAuthorities(authorities);
      account.setLedger(ledgerIdentifier);
      account.setBalance(balance);

      return new RecordFromLineNumber<>(csvRecord.getRecordNumber(), account);
    }
    catch (final NumberFormatException e) {
      logger.warn("Number parsing failed on record {}", csvRecord.getRecordNumber());
      throw e;
    }
    catch (final IllegalArgumentException e) {
      logger.warn("Parsing failed on record {}", csvRecord.getRecordNumber());
      throw e;
    }
  }
}
