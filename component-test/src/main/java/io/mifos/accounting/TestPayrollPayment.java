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

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import io.mifos.accounting.api.v1.EventConstants;
import io.mifos.accounting.api.v1.domain.Account;
import io.mifos.accounting.api.v1.domain.AccountType;
import io.mifos.accounting.api.v1.domain.Ledger;
import io.mifos.accounting.api.v1.domain.PayrollCollectionHistory;
import io.mifos.accounting.api.v1.domain.PayrollCollectionSheet;
import io.mifos.accounting.api.v1.domain.PayrollPayment;
import io.mifos.accounting.api.v1.domain.PayrollPaymentPage;
import io.mifos.accounting.service.internal.service.helper.CustomerAdapter;
import io.mifos.customer.api.v1.domain.PayrollAllocation;
import io.mifos.customer.api.v1.domain.PayrollDistribution;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class TestPayrollPayment extends AbstractAccountingTest {

  @MockBean
  private CustomerAdapter customerAdapterSpy;

  public TestPayrollPayment() {
    super();
  }

  @Test
  public void shouldProcessPayrollPayment() throws Exception {
    super.eventRecorder.clear();

    final String postfix = RandomStringUtils.randomAlphanumeric(8);
    this.setupResources(postfix);

    final String customerIdentifier = RandomStringUtils.randomAlphanumeric(32);

    final PayrollCollectionSheet payrollCollectionSheet = new PayrollCollectionSheet();
    payrollCollectionSheet.setSourceAccountNumber("7250." + postfix);

    final PayrollPayment payrollPayment = new PayrollPayment();
    payrollPayment.setCustomerIdentifier(customerIdentifier);
    payrollPayment.setEmployer("ACME, Inc.");
    payrollPayment.setSalary(BigDecimal.valueOf(1000.00D));
    payrollCollectionSheet.setPayrollPayments(Lists.newArrayList(payrollPayment));

    Mockito
        .doAnswer(invocation -> this.createPayrollDistribution(postfix))
        .when(this.customerAdapterSpy).findPayrollDistribution(customerIdentifier);

    super.testSubject.postPayrollPayments(payrollCollectionSheet);
    Assert.assertTrue(super.eventRecorder.wait(EventConstants.POST_PAYROLL_PAYMENT, payrollCollectionSheet.getSourceAccountNumber()));
    Assert.assertTrue(super.eventRecorder.waitForMatch(EventConstants.RELEASE_JOURNAL_ENTRY, Objects::nonNull));

    final Account payrollDeductionReceivableAccount = super.testSubject.findAccount(payrollCollectionSheet.getSourceAccountNumber());
    Assert.assertTrue(BigDecimal.valueOf(3000.00D).compareTo(BigDecimal.valueOf(payrollDeductionReceivableAccount.getBalance())) == 0);

    final Account memberChequingAccount = super.testSubject.findAccount("9140." + postfix);
    Assert.assertTrue(BigDecimal.valueOf(826.55D).compareTo(BigDecimal.valueOf(memberChequingAccount.getBalance())) == 0);

    final Account memberLoanAccount = super.testSubject.findAccount("7010." + postfix);
    Assert.assertTrue(BigDecimal.valueOf(1876.55D).compareTo(BigDecimal.valueOf(memberLoanAccount.getBalance())) == 0);

    final Account memberSavingsAccount = super.testSubject.findAccount("9110." + postfix);
    Assert.assertTrue(BigDecimal.valueOf(275.00D).compareTo(BigDecimal.valueOf(memberSavingsAccount.getBalance())) == 0);

    final List<PayrollCollectionHistory> payrollCollectionHistories = super.testSubject.getPayrollCollectionHistory();
    Assert.assertFalse(payrollCollectionHistories.isEmpty());

    final PayrollCollectionHistory payrollCollectionHistory = payrollCollectionHistories.get(0);
    Assert.assertEquals(payrollCollectionSheet.getSourceAccountNumber(), payrollCollectionHistory.getSourceAccountNumber());
    Assert.assertEquals(AbstractAccountingTest.TEST_USER, payrollCollectionHistory.getCreatedBy());

    final PayrollPaymentPage payrollPaymentPage =
        super.testSubject.getPayrollPaymentHistory(payrollCollectionHistory.getIdentifier(), 0, 20, null, null);
    Assert.assertEquals(Long.valueOf(1L), payrollPaymentPage.getTotalElements());
    final PayrollPayment fetchedPayrollPayment = payrollPaymentPage.getPayrollPayments().get(0);
    Assert.assertEquals(payrollPayment.getCustomerIdentifier(), fetchedPayrollPayment.getCustomerIdentifier());
    Assert.assertEquals(payrollPayment.getEmployer(), fetchedPayrollPayment.getEmployer());
    Assert.assertTrue(payrollPayment.getSalary().compareTo(fetchedPayrollPayment.getSalary()) == 0);

    super.eventRecorder.clear();
  }

  private Optional<PayrollDistribution> createPayrollDistribution(final String postfix) {
    final PayrollDistribution payrollDistribution = new PayrollDistribution();
    payrollDistribution.setMainAccountNumber("9140." + postfix);

    final PayrollAllocation loanAllocation = new PayrollAllocation();
    loanAllocation.setAccountNumber("7010." + postfix);
    loanAllocation.setAmount(BigDecimal.valueOf(123.45D));

    final PayrollAllocation savingsAllocation = new PayrollAllocation();
    savingsAllocation.setAccountNumber("9110." + postfix);
    savingsAllocation.setAmount(BigDecimal.valueOf(5.00D));
    savingsAllocation.setProportional(Boolean.TRUE);

    payrollDistribution.setPayrollAllocations(Sets.newHashSet(loanAllocation, savingsAllocation));

    return Optional.of(payrollDistribution);
  }

  private void setupResources(final String postfix) throws Exception {
    super.eventRecorder.clear();

    final Ledger assetLedger = new Ledger();
    assetLedger.setIdentifier("7000." + postfix);
    assetLedger.setType(AccountType.ASSET.name());
    assetLedger.setName("Asset Subledger");
    assetLedger.setShowAccountsInChart(Boolean.TRUE);

    super.testSubject.createLedger(assetLedger);
    Assert.assertTrue(super.eventRecorder.wait(EventConstants.POST_LEDGER, assetLedger.getIdentifier()));

    final Ledger equityLedger = new Ledger();
    equityLedger.setIdentifier("9000." + postfix);
    equityLedger.setType(AccountType.EQUITY.name());
    equityLedger.setName("Equity Subledger");
    equityLedger.setShowAccountsInChart(Boolean.TRUE);

    super.testSubject.createLedger(equityLedger);
    Assert.assertTrue(super.eventRecorder.wait(EventConstants.POST_LEDGER, equityLedger.getIdentifier()));

    final Account payrollDeductionReceivableAccount = new Account();
    payrollDeductionReceivableAccount.setIdentifier("7250." + postfix);
    payrollDeductionReceivableAccount.setType(AccountType.ASSET.name());
    payrollDeductionReceivableAccount.setLedger(assetLedger.getIdentifier());
    payrollDeductionReceivableAccount.setName("Payroll Deduction Receivable");
    payrollDeductionReceivableAccount.setBalance(2000.00D);

    super.testSubject.createAccount(payrollDeductionReceivableAccount);
    Assert.assertTrue(super.eventRecorder.wait(EventConstants.POST_ACCOUNT, payrollDeductionReceivableAccount.getIdentifier()));

    final Account memberLoanAccount = new Account();
    memberLoanAccount.setIdentifier("7010." + postfix);
    memberLoanAccount.setType(AccountType.ASSET.name());
    memberLoanAccount.setLedger(assetLedger.getIdentifier());
    memberLoanAccount.setName("Member Loan");
    memberLoanAccount.setBalance(2000.00D);

    super.testSubject.createAccount(memberLoanAccount);
    Assert.assertTrue(super.eventRecorder.wait(EventConstants.POST_ACCOUNT, memberLoanAccount.getIdentifier()));

    final Account memberChequingAccount = new Account();
    memberChequingAccount.setIdentifier("9140." + postfix);
    memberChequingAccount.setType(AccountType.EQUITY.name());
    memberChequingAccount.setLedger(equityLedger.getIdentifier());
    memberChequingAccount.setName("Member Chequing");
    memberChequingAccount.setBalance(0.00D);

    super.testSubject.createAccount(memberChequingAccount);
    Assert.assertTrue(super.eventRecorder.wait(EventConstants.POST_ACCOUNT, memberChequingAccount.getIdentifier()));

    final Account memberSavingsAccount = new Account();
    memberSavingsAccount.setIdentifier("9110." + postfix);
    memberSavingsAccount.setType(AccountType.EQUITY.name());
    memberSavingsAccount.setLedger(equityLedger.getIdentifier());
    memberSavingsAccount.setName("Member Savings");
    memberSavingsAccount.setBalance(225.00D);

    super.testSubject.createAccount(memberSavingsAccount);
    Assert.assertTrue(super.eventRecorder.wait(EventConstants.POST_ACCOUNT, memberSavingsAccount.getIdentifier()));

    super.eventRecorder.clear();
  }
}
