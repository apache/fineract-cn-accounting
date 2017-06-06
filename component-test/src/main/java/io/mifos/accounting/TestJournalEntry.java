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
package io.mifos.accounting;

import io.mifos.accounting.api.v1.EventConstants;
import io.mifos.accounting.api.v1.domain.Account;
import io.mifos.accounting.api.v1.domain.AccountType;
import io.mifos.accounting.api.v1.domain.JournalEntry;
import io.mifos.accounting.api.v1.domain.Ledger;
import io.mifos.accounting.util.AccountGenerator;
import io.mifos.accounting.util.JournalEntryGenerator;
import io.mifos.accounting.util.LedgerGenerator;
import org.junit.Assert;
import org.junit.Test;

import java.text.MessageFormat;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class TestJournalEntry extends AbstractAccountingTest {
  @Test
  public void shouldCreateJournalEntry() throws Exception {
    final Ledger assetLedger = LedgerGenerator.createRandomLedger();
    assetLedger.setType(AccountType.ASSET.name());
    this.testSubject.createLedger(assetLedger);
    this.eventRecorder.wait(EventConstants.POST_LEDGER, assetLedger.getIdentifier());

    final Account debtorAccount = AccountGenerator.createRandomAccount(assetLedger.getIdentifier());
    debtorAccount.setType(AccountType.ASSET.name());
    debtorAccount.setBalance(100.00D);
    this.testSubject.createAccount(debtorAccount);
    this.eventRecorder.wait(EventConstants.POST_ACCOUNT, debtorAccount.getIdentifier());

    final Ledger liabilityLedger = LedgerGenerator.createRandomLedger();
    liabilityLedger.setType(AccountType.LIABILITY.name());
    this.testSubject.createLedger(liabilityLedger);
    this.eventRecorder.wait(EventConstants.POST_LEDGER, liabilityLedger.getIdentifier());

    final Account creditorAccount = AccountGenerator.createRandomAccount(liabilityLedger.getIdentifier());
    creditorAccount.setType(AccountType.LIABILITY.name());
    creditorAccount.setBalance(100.00D);
    this.testSubject.createAccount(creditorAccount);
    this.eventRecorder.wait(EventConstants.POST_ACCOUNT, creditorAccount.getIdentifier());

    final JournalEntry journalEntry = JournalEntryGenerator.createRandomJournalEntry(debtorAccount, "50.00",
        creditorAccount, "50.00");
    this.testSubject.createJournalEntry(journalEntry);
    this.eventRecorder.wait(EventConstants.POST_JOURNAL_ENTRY, journalEntry.getTransactionIdentifier());

    this.eventRecorder.wait(EventConstants.RELEASE_JOURNAL_ENTRY, journalEntry.getTransactionIdentifier());

    final JournalEntry foundJournalEntry = this.testSubject.findJournalEntry(journalEntry.getTransactionIdentifier());
    Assert.assertNotNull(foundJournalEntry);
    Assert.assertEquals(JournalEntry.State.PROCESSED.name(), foundJournalEntry.getState());
    Assert.assertEquals(journalEntry.getTransactionType(), foundJournalEntry.getTransactionType());

    final Account modifiedDebtorAccount = this.testSubject.findAccount(debtorAccount.getIdentifier());
    Assert.assertNotNull(modifiedDebtorAccount);
    Assert.assertEquals(150.0D, modifiedDebtorAccount.getBalance(), 0.0D);

    final Account modifiedCreditorAccount = this.testSubject.findAccount(creditorAccount.getIdentifier());
    Assert.assertNotNull(modifiedCreditorAccount);
    Assert.assertEquals(150.0d, modifiedCreditorAccount.getBalance(), 0.0D);
  }

  @Test
  public void shouldFetchJournalEntriesWithDateRange() throws Exception{
    final Ledger assetLedger = LedgerGenerator.createRandomLedger();
    assetLedger.setType(AccountType.ASSET.name());
    this.testSubject.createLedger(assetLedger);
    this.eventRecorder.wait(EventConstants.POST_LEDGER, assetLedger.getIdentifier());

    final Account debtorAccount = AccountGenerator.createRandomAccount(assetLedger.getIdentifier());
    debtorAccount.setType(AccountType.ASSET.name());
    debtorAccount.setBalance(100.00D);
    this.testSubject.createAccount(debtorAccount);
    this.eventRecorder.wait(EventConstants.POST_ACCOUNT, debtorAccount.getIdentifier());

    final Ledger liabilityLedger = LedgerGenerator.createRandomLedger();
    liabilityLedger.setType(AccountType.LIABILITY.name());
    this.testSubject.createLedger(liabilityLedger);
    this.eventRecorder.wait(EventConstants.POST_LEDGER, liabilityLedger.getIdentifier());

    final Account creditorAccount = AccountGenerator.createRandomAccount(liabilityLedger.getIdentifier());
    creditorAccount.setType(AccountType.LIABILITY.name());
    creditorAccount.setBalance(100.00D);
    this.testSubject.createAccount(creditorAccount);
    this.eventRecorder.wait(EventConstants.POST_ACCOUNT, creditorAccount.getIdentifier());

    final JournalEntry journalEntryOne = JournalEntryGenerator.createRandomJournalEntry(debtorAccount, "50.00",
            creditorAccount, "50.00");
    final OffsetDateTime start = OffsetDateTime.of(1982, 6, 24, 1, 0, 0, 0, ZoneOffset.UTC);
    journalEntryOne.setTransactionDate(start.format(DateTimeFormatter.ISO_ZONED_DATE_TIME));

    this.testSubject.createJournalEntry(journalEntryOne);
    this.eventRecorder.wait(EventConstants.POST_JOURNAL_ENTRY, journalEntryOne.getTransactionIdentifier());
    this.eventRecorder.wait(EventConstants.RELEASE_JOURNAL_ENTRY, journalEntryOne.getTransactionIdentifier());

    final JournalEntry journalEntryTwo = JournalEntryGenerator.createRandomJournalEntry(debtorAccount, "50.00",
            creditorAccount, "50.00");
    final OffsetDateTime end = OffsetDateTime.of(1982, 6, 26, 1, 0, 0, 0, ZoneOffset.UTC);
    journalEntryTwo.setTransactionDate(end.format(DateTimeFormatter.ISO_ZONED_DATE_TIME));

    this.testSubject.createJournalEntry(journalEntryTwo);
    this.eventRecorder.wait(EventConstants.POST_JOURNAL_ENTRY, journalEntryTwo.getTransactionIdentifier());
    this.eventRecorder.wait(EventConstants.RELEASE_JOURNAL_ENTRY, journalEntryTwo.getTransactionIdentifier());

    final List<JournalEntry> journalEntries = this.testSubject.fetchJournalEntries(MessageFormat.format("{0}..{1}", "1982-06-24", "1982-06-26"));

    Assert.assertEquals(2, journalEntries.size());
  }
}
