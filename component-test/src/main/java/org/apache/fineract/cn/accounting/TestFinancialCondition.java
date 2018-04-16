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
import org.apache.fineract.cn.accounting.api.v1.domain.financial.statement.FinancialCondition;
import org.apache.fineract.cn.accounting.util.AccountGenerator;
import org.apache.fineract.cn.accounting.util.JournalEntryGenerator;
import org.apache.fineract.cn.accounting.util.LedgerGenerator;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;

public class TestFinancialCondition extends AbstractAccountingTest {

  public TestFinancialCondition() {
    super();
  }

  @Test
  public void shouldReturnFinancialCondition() throws Exception {
    this.fixtures();
    this.sampleJournalEntries();

    final BigDecimal totalAssets = BigDecimal.valueOf(250.00D);
    final BigDecimal expectedEquitiesAndLiabilities = BigDecimal.valueOf(250.00D);

    final FinancialCondition financialCondition = super.testSubject.getFinancialCondition();
    Assert.assertTrue(financialCondition.getTotalAssets().compareTo(totalAssets) == 0);
    Assert.assertTrue(financialCondition.getTotalEquitiesAndLiabilities().compareTo(expectedEquitiesAndLiabilities) == 0);
  }

  private void fixtures() throws Exception {
    final Ledger assetLedger = LedgerGenerator.createLedger("7000", AccountType.ASSET);
    assetLedger.setName("Assets");
    super.testSubject.createLedger(assetLedger);
    Assert.assertTrue(super.eventRecorder.wait(EventConstants.POST_LEDGER, assetLedger.getIdentifier()));

    final Ledger assetSubLedger7010 = LedgerGenerator.createLedger("7010", AccountType.ASSET);
    assetSubLedger7010.setParentLedgerIdentifier(assetLedger.getParentLedgerIdentifier());
    assetSubLedger7010.setName("Loans to Members");
    super.testSubject.addSubLedger(assetLedger.getIdentifier(), assetSubLedger7010);
    Assert.assertTrue(super.eventRecorder.wait(EventConstants.POST_LEDGER, assetSubLedger7010.getIdentifier()));

    final Ledger assetSubLedger7020 = LedgerGenerator.createLedger("7020", AccountType.ASSET);
    assetSubLedger7020.setParentLedgerIdentifier(assetLedger.getParentLedgerIdentifier());
    assetSubLedger7020.setName("Lines of Credit to Members");
    super.testSubject.addSubLedger(assetLedger.getIdentifier(), assetSubLedger7020);
    Assert.assertTrue(super.eventRecorder.wait(EventConstants.POST_LEDGER, assetSubLedger7020.getIdentifier()));

    final Account firstAssetAccount =
        AccountGenerator.createAccount(assetSubLedger7010.getIdentifier(), "7011", AccountType.ASSET);
    super.testSubject.createAccount(firstAssetAccount);
    Assert.assertTrue(super.eventRecorder.wait(EventConstants.POST_ACCOUNT, firstAssetAccount.getIdentifier()));

    final Account secondAssetAccount =
        AccountGenerator.createAccount(assetSubLedger7020.getIdentifier(), "7021", AccountType.ASSET);
    super.testSubject.createAccount(secondAssetAccount);
    Assert.assertTrue(super.eventRecorder.wait(EventConstants.POST_ACCOUNT, secondAssetAccount.getIdentifier()));

    final Ledger liabilityLedger = LedgerGenerator.createLedger("8000", AccountType.LIABILITY);
    liabilityLedger.setName("Liabilities");
    super.testSubject.createLedger(liabilityLedger);
    Assert.assertTrue(super.eventRecorder.wait(EventConstants.POST_LEDGER, liabilityLedger.getIdentifier()));

    final Ledger liabilitySubLedger8100 = LedgerGenerator.createLedger("8100", AccountType.LIABILITY);
    liabilitySubLedger8100.setParentLedgerIdentifier(liabilityLedger.getParentLedgerIdentifier());
    liabilitySubLedger8100.setName("Accounts Payable");
    super.testSubject.addSubLedger(liabilityLedger.getIdentifier(), liabilitySubLedger8100);
    Assert.assertTrue(super.eventRecorder.wait(EventConstants.POST_LEDGER, liabilitySubLedger8100.getIdentifier()));

    final Ledger liabilitySubLedger8200 = LedgerGenerator.createLedger("8200", AccountType.LIABILITY);
    liabilitySubLedger8200.setParentLedgerIdentifier(liabilityLedger.getParentLedgerIdentifier());
    liabilitySubLedger8200.setName("Interest Payable");
    super.testSubject.addSubLedger(liabilityLedger.getIdentifier(), liabilitySubLedger8200);
    Assert.assertTrue(super.eventRecorder.wait(EventConstants.POST_LEDGER, liabilitySubLedger8200.getIdentifier()));

    final Account firstLiabilityAccount =
        AccountGenerator.createAccount(liabilitySubLedger8100.getIdentifier(), "8110", AccountType.LIABILITY);
    super.testSubject.createAccount(firstLiabilityAccount);
    Assert.assertTrue(super.eventRecorder.wait(EventConstants.POST_ACCOUNT, firstLiabilityAccount.getIdentifier()));

    final Account secondLiabilityAccount =
        AccountGenerator.createAccount(liabilitySubLedger8200.getIdentifier(), "8210", AccountType.LIABILITY);
    super.testSubject.createAccount(secondLiabilityAccount);
    Assert.assertTrue(super.eventRecorder.wait(EventConstants.POST_ACCOUNT, secondLiabilityAccount.getIdentifier()));

    final Ledger equityLedger = LedgerGenerator.createLedger("9000", AccountType.EQUITY);
    equityLedger.setName("Equities");
    super.testSubject.createLedger(equityLedger);
    Assert.assertTrue(super.eventRecorder.wait(EventConstants.POST_LEDGER, equityLedger.getIdentifier()));

    final Ledger equitySubLedger9100 = LedgerGenerator.createLedger("9100", AccountType.EQUITY);
    equitySubLedger9100.setParentLedgerIdentifier(equityLedger.getParentLedgerIdentifier());
    equitySubLedger9100.setName("Member Savings");
    super.testSubject.addSubLedger(equityLedger.getIdentifier(), equitySubLedger9100);
    Assert.assertTrue(super.eventRecorder.wait(EventConstants.POST_LEDGER, equitySubLedger9100.getIdentifier()));

    final Account firstEquityAccount =
        AccountGenerator.createAccount(equitySubLedger9100.getIdentifier(), "9110", AccountType.EQUITY);
    super.testSubject.createAccount(firstEquityAccount);
    Assert.assertTrue(super.eventRecorder.wait(EventConstants.POST_ACCOUNT, firstEquityAccount.getIdentifier()));
  }

  private void sampleJournalEntries() throws Exception {
    final JournalEntry firstTransaction =
        JournalEntryGenerator
            .createRandomJournalEntry("7011", "150.00", "8110", "150.00");
    super.testSubject.createJournalEntry(firstTransaction);
    Assert.assertTrue(super.eventRecorder.wait(EventConstants.POST_JOURNAL_ENTRY, firstTransaction.getTransactionIdentifier()));

    final JournalEntry secondTransaction =
        JournalEntryGenerator
            .createRandomJournalEntry("7021", "100.00", "8210", "100.00");
    super.testSubject.createJournalEntry(secondTransaction);
    Assert.assertTrue(super.eventRecorder.wait(EventConstants.POST_JOURNAL_ENTRY, secondTransaction.getTransactionIdentifier()));

    final JournalEntry thirdTransaction =
        JournalEntryGenerator
            .createRandomJournalEntry("8210", "50.00", "9110", "50.00");
    super.testSubject.createJournalEntry(thirdTransaction);
    Assert.assertTrue(super.eventRecorder.wait(EventConstants.POST_JOURNAL_ENTRY, thirdTransaction.getTransactionIdentifier()));
  }
}
