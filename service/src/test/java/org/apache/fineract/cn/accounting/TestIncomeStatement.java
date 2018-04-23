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
import org.apache.fineract.cn.accounting.api.v1.domain.financial.statement.IncomeStatement;
import org.apache.fineract.cn.accounting.util.AccountGenerator;
import org.apache.fineract.cn.accounting.util.JournalEntryGenerator;
import org.apache.fineract.cn.accounting.util.LedgerGenerator;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;

public class TestIncomeStatement extends AbstractAccountingTest {

  public TestIncomeStatement() {
    super();
  }

  @Test
  public void shouldReturnIncomeStatement() throws Exception {
    this.fixtures();
    this.sampleJournalEntries();

    final BigDecimal expectedGrossProfit = BigDecimal.valueOf(350.00D);
    final BigDecimal expectedTotalExpenses = BigDecimal.valueOf(125.00D);
    final BigDecimal expectedNetIncome = expectedGrossProfit.subtract(expectedTotalExpenses);

    final IncomeStatement incomeStatement = super.testSubject.getIncomeStatement();
    Assert.assertTrue(incomeStatement.getGrossProfit().compareTo(expectedGrossProfit) == 0);
    Assert.assertTrue(incomeStatement.getTotalExpenses().compareTo(expectedTotalExpenses) == 0);
    Assert.assertTrue(incomeStatement.getNetIncome().compareTo(expectedNetIncome) == 0);
  }

  private void fixtures() throws Exception {
    final Ledger incomeLedger = LedgerGenerator.createLedger("1000", AccountType.REVENUE);
    incomeLedger.setName("Income");
    super.testSubject.createLedger(incomeLedger);
    Assert.assertTrue(super.eventRecorder.wait(EventConstants.POST_LEDGER, incomeLedger.getIdentifier()));

    final Ledger incomeSubLedger1100 = LedgerGenerator.createLedger("1100", AccountType.REVENUE);
    incomeSubLedger1100.setParentLedgerIdentifier(incomeLedger.getParentLedgerIdentifier());
    incomeSubLedger1100.setName("Income From Loans");
    super.testSubject.addSubLedger(incomeLedger.getIdentifier(), incomeSubLedger1100);
    Assert.assertTrue(super.eventRecorder.wait(EventConstants.POST_LEDGER, incomeSubLedger1100.getIdentifier()));

    final Ledger incomeSubLedger1300 = LedgerGenerator.createLedger("1300", AccountType.REVENUE);
    incomeSubLedger1300.setParentLedgerIdentifier(incomeLedger.getParentLedgerIdentifier());
    incomeSubLedger1300.setName("Fees and Charges");
    super.testSubject.addSubLedger(incomeLedger.getIdentifier(), incomeSubLedger1300);
    Assert.assertTrue(super.eventRecorder.wait(EventConstants.POST_LEDGER, incomeSubLedger1300.getIdentifier()));

    final Account account1110 =
        AccountGenerator.createAccount(incomeSubLedger1100.getIdentifier(), "1110", AccountType.REVENUE);
    super.testSubject.createAccount(account1110);
    Assert.assertTrue(super.eventRecorder.wait(EventConstants.POST_ACCOUNT, account1110.getIdentifier()));

    final Account account1310 =
        AccountGenerator.createAccount(incomeSubLedger1300.getIdentifier(), "1310", AccountType.REVENUE);
    super.testSubject.createAccount(account1310);
    Assert.assertTrue(super.eventRecorder.wait(EventConstants.POST_ACCOUNT, account1310.getIdentifier()));

    final Ledger expenseLedger = LedgerGenerator.createLedger("3000", AccountType.EXPENSE);
    expenseLedger.setName("Expenses");
    super.testSubject.createLedger(expenseLedger);
    Assert.assertTrue(super.eventRecorder.wait(EventConstants.POST_LEDGER, expenseLedger.getIdentifier()));

    final Ledger expenseSubLedger3500 = LedgerGenerator.createLedger("3500", AccountType.EXPENSE);
    expenseSubLedger3500.setParentLedgerIdentifier(expenseLedger.getParentLedgerIdentifier());
    expenseSubLedger3500.setName("Annual Meeting Expenses");
    super.testSubject.addSubLedger(expenseLedger.getIdentifier(), expenseSubLedger3500);
    Assert.assertTrue(super.eventRecorder.wait(EventConstants.POST_LEDGER, expenseSubLedger3500.getIdentifier()));

    final Ledger expenseSubLedger3700 = LedgerGenerator.createLedger("3700", AccountType.EXPENSE);
    expenseSubLedger3700.setParentLedgerIdentifier(expenseLedger.getParentLedgerIdentifier());
    expenseSubLedger3700.setName("Interest (Dividend) Expense");
    super.testSubject.addSubLedger(expenseLedger.getIdentifier(), expenseSubLedger3700);
    Assert.assertTrue(super.eventRecorder.wait(EventConstants.POST_LEDGER, expenseSubLedger3700.getIdentifier()));

    final Account account3510 =
        AccountGenerator.createAccount(expenseSubLedger3500.getIdentifier(), "3510", AccountType.EXPENSE);
    super.testSubject.createAccount(account3510);
    Assert.assertTrue(super.eventRecorder.wait(EventConstants.POST_ACCOUNT, account3510.getIdentifier()));

    final Account account3710 =
        AccountGenerator.createAccount(expenseSubLedger3700.getIdentifier(), "3710", AccountType.EXPENSE);
    super.testSubject.createAccount(account3710);
    Assert.assertTrue(super.eventRecorder.wait(EventConstants.POST_ACCOUNT, account3710.getIdentifier()));

    final Ledger assetLedger = LedgerGenerator.createLedger("7000", AccountType.ASSET);
    super.testSubject.createLedger(assetLedger);
    Assert.assertTrue(super.eventRecorder.wait(EventConstants.POST_LEDGER, assetLedger.getIdentifier()));

    final Account assetAccount =
        AccountGenerator.createAccount(assetLedger.getIdentifier(), "7010", AccountType.ASSET);
    super.testSubject.createAccount(assetAccount);
    Assert.assertTrue(super.eventRecorder.wait(EventConstants.POST_ACCOUNT, assetAccount.getIdentifier()));

    final Ledger liabilityLedger = LedgerGenerator.createLedger("8000", AccountType.LIABILITY);
    super.testSubject.createLedger(liabilityLedger);
    Assert.assertTrue(super.eventRecorder.wait(EventConstants.POST_LEDGER, liabilityLedger.getIdentifier()));

    final Account liabilityAccount =
        AccountGenerator.createAccount(liabilityLedger.getIdentifier(), "8010", AccountType.LIABILITY);
    super.testSubject.createAccount(liabilityAccount);
    Assert.assertTrue(super.eventRecorder.wait(EventConstants.POST_ACCOUNT, liabilityAccount.getIdentifier()));
  }

  private void sampleJournalEntries() throws Exception {
    final JournalEntry firstTransaction =
        JournalEntryGenerator
            .createRandomJournalEntry("7010", "150.00", "1110", "150.00");
    super.testSubject.createJournalEntry(firstTransaction);
    Assert.assertTrue(super.eventRecorder.wait(EventConstants.POST_JOURNAL_ENTRY, firstTransaction.getTransactionIdentifier()));

    final JournalEntry secondTransaction =
        JournalEntryGenerator
            .createRandomJournalEntry("7010", "200.00", "1310", "200.00");
    super.testSubject.createJournalEntry(secondTransaction);
    Assert.assertTrue(super.eventRecorder.wait(EventConstants.POST_JOURNAL_ENTRY, secondTransaction.getTransactionIdentifier()));

    final JournalEntry thirdTransaction =
        JournalEntryGenerator
            .createRandomJournalEntry("3510", "50.00", "8010", "50.00");
    super.testSubject.createJournalEntry(thirdTransaction);
    Assert.assertTrue(super.eventRecorder.wait(EventConstants.POST_JOURNAL_ENTRY, thirdTransaction.getTransactionIdentifier()));

    final JournalEntry fourthTransaction =
        JournalEntryGenerator
            .createRandomJournalEntry("3710", "75.00", "8010", "75.00");
    super.testSubject.createJournalEntry(fourthTransaction);
    Assert.assertTrue(super.eventRecorder.wait(EventConstants.POST_JOURNAL_ENTRY, fourthTransaction.getTransactionIdentifier()));
  }
}
