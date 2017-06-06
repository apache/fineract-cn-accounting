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
package io.mifos.accounting.util;

import io.mifos.accounting.api.v1.domain.Account;
import io.mifos.accounting.api.v1.domain.Creditor;
import io.mifos.accounting.api.v1.domain.Debtor;
import io.mifos.accounting.api.v1.domain.JournalEntry;
import org.apache.commons.lang3.RandomStringUtils;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashSet;

public class JournalEntryGenerator {

  private JournalEntryGenerator() {
    super();
  }

  public static JournalEntry createRandomJournalEntry(final Account debtorAccount,
                                                      final String debtorAmount,
                                                      final Account creditorAccount,
                                                      final String creditorAmount) {
    final JournalEntry journalEntry = new JournalEntry();
    journalEntry.setTransactionIdentifier(RandomStringUtils.randomAlphanumeric(8));
    journalEntry.setTransactionDate(ZonedDateTime.now(Clock.systemUTC()).format(DateTimeFormatter.ISO_ZONED_DATE_TIME));
    journalEntry.setTransactionType(RandomStringUtils.randomAlphabetic(4));
    journalEntry.setClerk("clark");
    final Debtor debtor = new Debtor();
    debtor.setAccountNumber(debtorAccount.getIdentifier());
    debtor.setAmount(debtorAmount);
    journalEntry.setDebtors(new HashSet<>(Collections.singletonList(debtor)));
    final Creditor creditor = new Creditor();
    creditor.setAccountNumber(creditorAccount.getIdentifier());
    creditor.setAmount(creditorAmount);
    journalEntry.setCreditors(new HashSet<>(Collections.singletonList(creditor)));
    journalEntry.setNote(RandomStringUtils.randomAlphanumeric(512));
    journalEntry.setMessage(RandomStringUtils.randomAlphanumeric(512));
    return journalEntry;
  }
}
