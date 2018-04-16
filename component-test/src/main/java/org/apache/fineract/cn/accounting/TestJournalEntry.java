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
package org.apache.fineract.cn.accounting;

import org.apache.fineract.cn.accounting.api.v1.EventConstants;
import org.apache.fineract.cn.accounting.api.v1.client.JournalEntryAlreadyExistsException;
import org.apache.fineract.cn.accounting.api.v1.client.JournalEntryValidationException;
import org.apache.fineract.cn.accounting.api.v1.domain.Account;
import org.apache.fineract.cn.accounting.api.v1.domain.AccountEntryPage;
import org.apache.fineract.cn.accounting.api.v1.domain.AccountType;
import org.apache.fineract.cn.accounting.api.v1.domain.JournalEntry;
import org.apache.fineract.cn.accounting.api.v1.domain.Ledger;
import org.apache.fineract.cn.accounting.util.AccountGenerator;
import org.apache.fineract.cn.accounting.util.JournalEntryGenerator;
import org.apache.fineract.cn.accounting.util.LedgerGenerator;
import java.math.BigDecimal;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.fineract.cn.lang.DateConverter;
import org.junit.Assert;
import org.junit.Test;

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

    final LocalDate beginDate = LocalDate.of(1982, 6, 24);
    final LocalDate endDate = LocalDate.of(1982, 6, 26);
    final String dateRange = MessageFormat.format("{0}..{1}",
        DateConverter.toIsoString(beginDate),
        DateConverter.toIsoString(endDate));

    final List<JournalEntry> journalEntries = this.testSubject.fetchJournalEntries(dateRange, null, null);

    Assert.assertTrue(journalEntries.size() >= 2);

    checkAccountEntries(debtorAccount, creditorAccount, journalEntryOne, journalEntryTwo, dateRange);
  }

  @Test
  public void shouldFetchJournalEntriesWithDateRangeAndAccount() throws Exception{
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

    final LocalDate beginDate = LocalDate.of(1982, 6, 24);
    final LocalDate endDate = LocalDate.of(1982, 6, 26);
    final String dateRange = MessageFormat.format("{0}..{1}",
        DateConverter.toIsoString(beginDate),
        DateConverter.toIsoString(endDate));

    final List<JournalEntry> journalEntries = this.testSubject.fetchJournalEntries(dateRange, debtorAccount.getIdentifier(), null);

    Assert.assertEquals(2, journalEntries.size());

    checkAccountEntries(debtorAccount, creditorAccount, journalEntryOne, journalEntryTwo, dateRange);
  }

  @Test
  public void shouldFetchJournalEntriesWithDateRangeAndAmount() throws Exception{
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

    final LocalDate beginDate = LocalDate.of(1982, 6, 24);
    final LocalDate endDate = LocalDate.of(1982, 6, 26);
    final String dateRange = MessageFormat.format("{0}..{1}",
        DateConverter.toIsoString(beginDate),
        DateConverter.toIsoString(endDate));

    final List<JournalEntry> journalEntries = this.testSubject.fetchJournalEntries(dateRange, null, BigDecimal.valueOf(50.00D));

    Assert.assertEquals(2, journalEntries.size());

    checkAccountEntries(debtorAccount, creditorAccount, journalEntryOne, journalEntryTwo, dateRange);
  }

  @Test
  public void shouldFetchJournalEntriesWithDateRangeAndAccountAndAmount() throws Exception{
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

    final LocalDate beginDate = LocalDate.of(1982, 6, 24);
    final LocalDate endDate = LocalDate.of(1982, 6, 26);
    final String dateRange = MessageFormat.format("{0}..{1}",
        DateConverter.toIsoString(beginDate),
        DateConverter.toIsoString(endDate));

    final List<JournalEntry> journalEntries = this.testSubject.fetchJournalEntries(dateRange, creditorAccount.getIdentifier(), BigDecimal.valueOf(50.00D));

    Assert.assertEquals(2, journalEntries.size());

    checkAccountEntries(debtorAccount, creditorAccount, journalEntryOne, journalEntryTwo, dateRange);
  }

  @Test(expected = JournalEntryValidationException.class)
  public void shouldNotCreateJournalEntryMissingDebtors() throws Exception {
    final Ledger liabilityLedger = LedgerGenerator.createRandomLedger();
    liabilityLedger.setType(AccountType.LIABILITY.name());
    this.testSubject.createLedger(liabilityLedger);
    this.eventRecorder.wait(EventConstants.POST_LEDGER, liabilityLedger.getIdentifier());

    final Account creditorAccount = AccountGenerator.createRandomAccount(liabilityLedger.getIdentifier());
    creditorAccount.setType(AccountType.LIABILITY.name());
    creditorAccount.setBalance(100.00D);
    this.testSubject.createAccount(creditorAccount);
    this.eventRecorder.wait(EventConstants.POST_ACCOUNT, creditorAccount.getIdentifier());

    final JournalEntry journalEntry = JournalEntryGenerator.createRandomJournalEntry(null, null,
        creditorAccount, "50.00");
    this.testSubject.createJournalEntry(journalEntry);
  }

  @Test(expected = JournalEntryAlreadyExistsException.class)
  public void shouldNotCreateJournalAlreadyExists() throws Exception {
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

    final JournalEntry journalEntry = JournalEntryGenerator.createRandomJournalEntry(creditorAccount, "50.00",
        creditorAccount, "50.00");
    this.testSubject.createJournalEntry(journalEntry);
    this.eventRecorder.wait(EventConstants.POST_JOURNAL_ENTRY, journalEntry.getTransactionIdentifier());

    this.testSubject.createJournalEntry(journalEntry);
  }

  @Test(expected = JournalEntryValidationException.class)
  public void shouldNotCreateJournalEntryMissingCreditors() throws Exception {
    final Ledger assetLedger = LedgerGenerator.createRandomLedger();
    assetLedger.setType(AccountType.ASSET.name());
    this.testSubject.createLedger(assetLedger);
    this.eventRecorder.wait(EventConstants.POST_LEDGER, assetLedger.getIdentifier());

    final Account debtorAccount = AccountGenerator.createRandomAccount(assetLedger.getIdentifier());
    debtorAccount.setType(AccountType.ASSET.name());
    debtorAccount.setBalance(100.00D);
    this.testSubject.createAccount(debtorAccount);
    this.eventRecorder.wait(EventConstants.POST_ACCOUNT, debtorAccount.getIdentifier());

    final JournalEntry journalEntry = JournalEntryGenerator.createRandomJournalEntry(debtorAccount, "50.00",
        null, null);
    this.testSubject.createJournalEntry(journalEntry);
  }

  private void checkAccountEntries(
      final Account debtorAccount,
      final Account creditorAccount,
      final JournalEntry journalEntryOne,
      final JournalEntry journalEntryTwo,
      final String dateRange) {
    final AccountEntryPage creditorAccountEntries = this.testSubject.fetchAccountEntries(
        creditorAccount.getIdentifier(),
        dateRange,
        null,
        null,
        null,
        null,
        "ASC");
    Assert.assertEquals(Long.valueOf(2L), creditorAccountEntries.getTotalElements());
    Assert.assertEquals("Sort order check for ascending.",
        journalEntryOne.getMessage(),
        creditorAccountEntries.getAccountEntries().get(0).getMessage());

    final AccountEntryPage creditorAccountEntriesWithMessage1 = this.testSubject.fetchAccountEntries(
        creditorAccount.getIdentifier(),
        dateRange,
        journalEntryOne.getMessage(),
        null,
        null,
        null,
        null);
    Assert.assertEquals(Long.valueOf(1L), creditorAccountEntriesWithMessage1.getTotalElements());
    Assert.assertEquals("Correct entry returned.",
        journalEntryOne.getMessage(),
        creditorAccountEntriesWithMessage1.getAccountEntries().get(0).getMessage());

    final AccountEntryPage debtorAccountEntries = this.testSubject.fetchAccountEntries(
        debtorAccount.getIdentifier(),
        dateRange,
        null,
        null,
        null,
        null,
        "DESC");
    Assert.assertEquals(Long.valueOf(2L), debtorAccountEntries.getTotalElements());
    Assert.assertEquals("Sort order check for descending.",
        journalEntryTwo.getMessage(),
        debtorAccountEntries.getAccountEntries().get(0).getMessage());

    final AccountEntryPage debtorAccountEntriesWithMessage2 = this.testSubject.fetchAccountEntries(
        creditorAccount.getIdentifier(),
        dateRange,
        journalEntryTwo.getMessage(),
        null,
        null,
        null,
        null);
    Assert.assertEquals(Long.valueOf(1L), debtorAccountEntriesWithMessage2.getTotalElements());
    Assert.assertEquals("Correct entry returned.",
        journalEntryTwo.getMessage(),
        debtorAccountEntriesWithMessage2.getAccountEntries().get(0).getMessage());

    final AccountEntryPage debtorAccountEntriesWithRandomMessage = this.testSubject.fetchAccountEntries(
        creditorAccount.getIdentifier(),
        dateRange,
        RandomStringUtils.randomAlphanumeric(20),
        null,
        null,
        null,
        null);
    Assert.assertEquals(Long.valueOf(0L), debtorAccountEntriesWithRandomMessage.getTotalElements());
  }
}
