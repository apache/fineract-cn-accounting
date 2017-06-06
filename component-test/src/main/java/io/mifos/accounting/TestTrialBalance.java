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
import io.mifos.accounting.api.v1.domain.TrialBalance;
import io.mifos.accounting.util.AccountGenerator;
import io.mifos.accounting.util.JournalEntryGenerator;
import io.mifos.accounting.util.LedgerGenerator;
import org.junit.Assert;
import org.junit.Test;

public class TestTrialBalance extends AbstractAccountingTest {
  @Test
  public void shouldGenerateTrialBalance() throws Exception {
    final Ledger ledgerOne = LedgerGenerator.createRandomLedger();
    this.testSubject.createLedger(ledgerOne);
    this.eventRecorder.wait(EventConstants.POST_LEDGER, ledgerOne.getIdentifier());

    final Ledger subLedgerOne = LedgerGenerator.createRandomLedger();
    this.testSubject.addSubLedger(ledgerOne.getIdentifier(), subLedgerOne);
    this.eventRecorder.wait(EventConstants.POST_LEDGER, subLedgerOne.getIdentifier());

    final Ledger ledgerTwo = LedgerGenerator.createRandomLedger();
    ledgerTwo.setType(AccountType.LIABILITY.name());
    this.testSubject.createLedger(ledgerTwo);
    this.eventRecorder.wait(EventConstants.POST_LEDGER, ledgerTwo.getIdentifier());

    final Account account4ledgerOne = AccountGenerator.createRandomAccount(ledgerOne.getIdentifier());
    this.testSubject.createAccount(account4ledgerOne);
    this.eventRecorder.wait(EventConstants.POST_ACCOUNT, account4ledgerOne.getIdentifier());

    final Account account4subLedgerOne = AccountGenerator.createRandomAccount(subLedgerOne.getIdentifier());
    this.testSubject.createAccount(account4subLedgerOne);
    this.eventRecorder.wait(EventConstants.POST_ACCOUNT, account4subLedgerOne.getIdentifier());

    final Account account4ledgerTwo = AccountGenerator.createRandomAccount(ledgerTwo.getIdentifier());
    account4ledgerTwo.setType(AccountType.LIABILITY.name());
    this.testSubject.createAccount(account4ledgerTwo);
    this.eventRecorder.wait(EventConstants.POST_ACCOUNT, account4ledgerTwo.getIdentifier());

    final JournalEntry firstBooking =
        JournalEntryGenerator.createRandomJournalEntry(account4ledgerOne, "50.00", account4ledgerTwo, "50.00");
    this.testSubject.createJournalEntry(firstBooking);
    this.eventRecorder.wait(EventConstants.RELEASE_JOURNAL_ENTRY, firstBooking.getTransactionIdentifier());

    final JournalEntry secondBooking =
        JournalEntryGenerator.createRandomJournalEntry(account4subLedgerOne, "50.00", account4ledgerTwo, "50.00");
    this.testSubject.createJournalEntry(secondBooking);
    this.eventRecorder.wait(EventConstants.RELEASE_JOURNAL_ENTRY, secondBooking.getTransactionIdentifier());

    final TrialBalance trialBalance = this.testSubject.getTrialBalance(true);
    Assert.assertNotNull(trialBalance);
    Assert.assertEquals(3, trialBalance.getTrialBalanceEntries().size());
    Assert.assertEquals(100.00D, trialBalance.getDebitTotal(), 0.00D);
    Assert.assertEquals(100.00D, trialBalance.getCreditTotal(), 0.00D);
  }
}
