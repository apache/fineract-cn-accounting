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
import org.apache.fineract.cn.accounting.api.v1.domain.Account;
import org.apache.fineract.cn.accounting.api.v1.domain.AccountType;
import org.apache.fineract.cn.accounting.api.v1.domain.JournalEntry;
import org.apache.fineract.cn.accounting.api.v1.domain.Ledger;
import org.apache.fineract.cn.accounting.api.v1.domain.financial.statement.TrialBalance;
import org.apache.fineract.cn.accounting.util.AccountGenerator;
import org.apache.fineract.cn.accounting.util.JournalEntryGenerator;
import org.apache.fineract.cn.accounting.util.LedgerGenerator;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;

public class TestTrialBalance extends AbstractAccountingTest {
  @Test
  public void shouldGenerateTrialBalance() throws Exception {
    final Ledger assetLedger = LedgerGenerator.createRandomLedger();
    this.testSubject.createLedger(assetLedger);
    this.eventRecorder.wait(EventConstants.POST_LEDGER, assetLedger.getIdentifier());

    final Ledger assetSubLedgerOne = LedgerGenerator.createRandomLedger();
    this.testSubject.addSubLedger(assetLedger.getIdentifier(), assetSubLedgerOne);
    this.eventRecorder.wait(EventConstants.POST_LEDGER, assetSubLedgerOne.getIdentifier());

    final Ledger assetSubLedgerTwo = LedgerGenerator.createRandomLedger();
    this.testSubject.addSubLedger(assetLedger.getIdentifier(), assetSubLedgerTwo);
    this.eventRecorder.wait(EventConstants.POST_LEDGER, assetSubLedgerTwo.getIdentifier());

    final Ledger liabilityLedger = LedgerGenerator.createRandomLedger();
    liabilityLedger.setType(AccountType.LIABILITY.name());
    this.testSubject.createLedger(liabilityLedger);
    this.eventRecorder.wait(EventConstants.POST_LEDGER, liabilityLedger.getIdentifier());

    final Ledger liabilitySubLedger = LedgerGenerator.createRandomLedger();
    liabilitySubLedger.setType(AccountType.LIABILITY.name());
    this.testSubject.addSubLedger(liabilityLedger.getIdentifier(), liabilitySubLedger);
    this.eventRecorder.wait(EventConstants.POST_LEDGER, liabilitySubLedger.getIdentifier());

    final Account account4ledgerOne = AccountGenerator.createRandomAccount(assetSubLedgerOne.getIdentifier());
    this.testSubject.createAccount(account4ledgerOne);
    this.eventRecorder.wait(EventConstants.POST_ACCOUNT, account4ledgerOne.getIdentifier());

    final Account secondAccount4ledgerOne = AccountGenerator.createRandomAccount(assetSubLedgerOne.getIdentifier());
    this.testSubject.createAccount(secondAccount4ledgerOne);
    this.eventRecorder.wait(EventConstants.POST_ACCOUNT, secondAccount4ledgerOne.getIdentifier());

    final Account account4subLedgerOne = AccountGenerator.createRandomAccount(assetSubLedgerTwo.getIdentifier());
    this.testSubject.createAccount(account4subLedgerOne);
    this.eventRecorder.wait(EventConstants.POST_ACCOUNT, account4subLedgerOne.getIdentifier());

    final Account account4ledgerTwo = AccountGenerator.createRandomAccount(liabilitySubLedger.getIdentifier());
    account4ledgerTwo.setType(AccountType.LIABILITY.name());
    this.testSubject.createAccount(account4ledgerTwo);
    this.eventRecorder.wait(EventConstants.POST_ACCOUNT, account4ledgerTwo.getIdentifier());

    final JournalEntry firstBooking =
        JournalEntryGenerator.createRandomJournalEntry(secondAccount4ledgerOne, "50.00", account4ledgerTwo, "50.00");
    this.testSubject.createJournalEntry(firstBooking);
    this.eventRecorder.wait(EventConstants.RELEASE_JOURNAL_ENTRY, firstBooking.getTransactionIdentifier());

    final JournalEntry secondBooking =
        JournalEntryGenerator.createRandomJournalEntry(secondAccount4ledgerOne, "50.00", account4ledgerOne, "50.00");
    this.testSubject.createJournalEntry(secondBooking);
    this.eventRecorder.wait(EventConstants.RELEASE_JOURNAL_ENTRY, secondBooking.getTransactionIdentifier());

    final JournalEntry thirdBooking =
        JournalEntryGenerator.createRandomJournalEntry(account4subLedgerOne, "50.00", account4ledgerTwo, "50.00");
    this.testSubject.createJournalEntry(thirdBooking);
    this.eventRecorder.wait(EventConstants.RELEASE_JOURNAL_ENTRY, thirdBooking.getTransactionIdentifier());

    final TrialBalance trialBalance = this.testSubject.getTrialBalance(true);
    Assert.assertNotNull(trialBalance);
    Assert.assertEquals(3, trialBalance.getTrialBalanceEntries().size());
    final BigDecimal expectedValue = BigDecimal.valueOf(100.00D);
    Assert.assertTrue(trialBalance.getDebitTotal().compareTo(expectedValue) == 0);
    Assert.assertTrue(trialBalance.getCreditTotal().compareTo(expectedValue) == 0);
  }
}
