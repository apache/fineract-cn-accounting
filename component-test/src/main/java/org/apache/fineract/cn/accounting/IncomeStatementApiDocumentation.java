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
import org.apache.fineract.cn.accounting.util.AccountGenerator;
import org.apache.fineract.cn.accounting.util.JournalEntryGenerator;
import org.apache.fineract.cn.accounting.util.LedgerGenerator;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.restdocs.JUnitRestDocumentation;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class IncomeStatementApiDocumentation extends AbstractAccountingTest {

  @Rule
  public final JUnitRestDocumentation restDocumentation = new JUnitRestDocumentation("build/doc/generated-snippets/test-income-statement");

  @Autowired
  private WebApplicationContext context;

  private MockMvc mockMvc;

  @Before
  public void setUp ( ) {

    this.mockMvc = MockMvcBuilders.webAppContextSetup(this.context)
            .apply(documentationConfiguration(this.restDocumentation))
            .alwaysDo(document("{method-name}", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint())))
            .build();
  }

  @Test
  public void documentReturnIncomeStatement ( ) throws Exception {
    this.fixtures();
    this.sampleJournalEntries();

    this.mockMvc.perform(get("/incomestatement")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .accept(MediaType.ALL_VALUE))
            .andExpect(status().isOk())
            .andDo(document("document-return-income-statement", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()),
                    responseFields(
                            fieldWithPath("date").type("String").description("Date Of Income Statement Preparation"),
                            fieldWithPath("incomeStatementSections[].type").description("Type").description("Type of first section " +
                                    " + \n" +
                                    " + \n" +
                                    " *enum* _Type_ { + \n" +
                                    "  INCOME, + \n" +
                                    "  EXPENSES + \n" +
                                    "}"),
                            fieldWithPath("incomeStatementSections[].description").type("String").description("first section's description"),
                            fieldWithPath("incomeStatementSections[].incomeStatementEntries[].description").type("String").description("first entry's description"),
                            fieldWithPath("incomeStatementSections[].incomeStatementEntries[].value").type("BigDecimal").description("first entry's value"),
                            fieldWithPath("incomeStatementSections[].incomeStatementEntries[1].description").type("String").description("second entry's description"),
                            fieldWithPath("incomeStatementSections[].incomeStatementEntries[1].value").type("BigDecimal").description("second entry's value"),
                            fieldWithPath("incomeStatementSections[].subtotal").type("BigDecimal").description("First section's subtotal"),
                            fieldWithPath("incomeStatementSections[1].type").description("Type").description("Type of first section " +
                                    " + \n" +
                                    " + \n" +
                                    " *enum* _Type_ { + \n" +
                                    "  INCOME, + \n" +
                                    "  EXPENSES + \n" +
                                    "}"),
                            fieldWithPath("incomeStatementSections[1].description").type("String").description("first section's description"),
                            fieldWithPath("incomeStatementSections[1].incomeStatementEntries[].description").type("String").description("first entry's description"),
                            fieldWithPath("incomeStatementSections[1].incomeStatementEntries[].value").type("BigDecimal").description("first entry's value"),
                            fieldWithPath("incomeStatementSections[1].incomeStatementEntries[1].description").type("String").description("second entry's description"),
                            fieldWithPath("incomeStatementSections[1].incomeStatementEntries[1].value").type("BigDecimal").description("second entry's value"),
                            fieldWithPath("incomeStatementSections[1].subtotal").type("BigDecimal").description("First section's subtotal"),
                            fieldWithPath("grossProfit").type("BigDecimal").description("Gross Profit"),
                            fieldWithPath("totalExpenses").type("BigDecimal").description("Total Expenses"),
                            fieldWithPath("netIncome").type("BigDecimal").description("Net Income")
                    )));
  }

  private void fixtures ( ) throws Exception {
    final Ledger incomeLedger = LedgerGenerator.createLedger("1170", AccountType.REVENUE);
    incomeLedger.setName("Revenue");
    super.testSubject.createLedger(incomeLedger);
    Assert.assertTrue(super.eventRecorder.wait(EventConstants.POST_LEDGER, incomeLedger.getIdentifier()));

    final Ledger incomeSubLedger1100 = LedgerGenerator.createLedger("1070", AccountType.REVENUE);
    incomeSubLedger1100.setParentLedgerIdentifier(incomeLedger.getParentLedgerIdentifier());
    incomeSubLedger1100.setName("Revenue From Loans");
    super.testSubject.addSubLedger(incomeLedger.getIdentifier(), incomeSubLedger1100);
    Assert.assertTrue(super.eventRecorder.wait(EventConstants.POST_LEDGER, incomeSubLedger1100.getIdentifier()));

    final Ledger incomeSubLedger1300 = LedgerGenerator.createLedger("1370", AccountType.REVENUE);
    incomeSubLedger1300.setParentLedgerIdentifier(incomeLedger.getParentLedgerIdentifier());
    incomeSubLedger1300.setName("Fees");
    super.testSubject.addSubLedger(incomeLedger.getIdentifier(), incomeSubLedger1300);
    Assert.assertTrue(super.eventRecorder.wait(EventConstants.POST_LEDGER, incomeSubLedger1300.getIdentifier()));

    final Account account1170 =
            AccountGenerator.createAccount(incomeSubLedger1100.getIdentifier(), "1170", AccountType.REVENUE);
    super.testSubject.createAccount(account1170);
    Assert.assertTrue(super.eventRecorder.wait(EventConstants.POST_ACCOUNT, account1170.getIdentifier()));

    final Account account1370 =
            AccountGenerator.createAccount(incomeSubLedger1300.getIdentifier(), "1370", AccountType.REVENUE);
    super.testSubject.createAccount(account1370);
    Assert.assertTrue(super.eventRecorder.wait(EventConstants.POST_ACCOUNT, account1370.getIdentifier()));

    final Ledger expenseLedger = LedgerGenerator.createLedger("3070", AccountType.EXPENSE);
    expenseLedger.setName("Expenses");
    super.testSubject.createLedger(expenseLedger);
    Assert.assertTrue(super.eventRecorder.wait(EventConstants.POST_LEDGER, expenseLedger.getIdentifier()));

    final Ledger expenseSubLedger3570 = LedgerGenerator.createLedger("3570", AccountType.EXPENSE);
    expenseSubLedger3570.setParentLedgerIdentifier(expenseLedger.getParentLedgerIdentifier());
    expenseSubLedger3570.setName("Annual Meeting Expenses");
    super.testSubject.addSubLedger(expenseLedger.getIdentifier(), expenseSubLedger3570);
    Assert.assertTrue(super.eventRecorder.wait(EventConstants.POST_LEDGER, expenseSubLedger3570.getIdentifier()));

    final Ledger expenseSubLedger3770 = LedgerGenerator.createLedger("3770", AccountType.EXPENSE);
    expenseSubLedger3770.setParentLedgerIdentifier(expenseLedger.getParentLedgerIdentifier());
    expenseSubLedger3770.setName("Interest (Dividend) Expense");
    super.testSubject.addSubLedger(expenseLedger.getIdentifier(), expenseSubLedger3770);
    Assert.assertTrue(super.eventRecorder.wait(EventConstants.POST_LEDGER, expenseSubLedger3770.getIdentifier()));

    final Account account3570 =
            AccountGenerator.createAccount(expenseSubLedger3570.getIdentifier(), "3570", AccountType.EXPENSE);
    super.testSubject.createAccount(account3570);
    Assert.assertTrue(super.eventRecorder.wait(EventConstants.POST_ACCOUNT, account3570.getIdentifier()));

    final Account account3770 =
            AccountGenerator.createAccount(expenseSubLedger3770.getIdentifier(), "3770", AccountType.EXPENSE);
    super.testSubject.createAccount(account3770);
    Assert.assertTrue(super.eventRecorder.wait(EventConstants.POST_ACCOUNT, account3770.getIdentifier()));

    final Ledger assetLedger = LedgerGenerator.createLedger("7070", AccountType.ASSET);
    super.testSubject.createLedger(assetLedger);
    Assert.assertTrue(super.eventRecorder.wait(EventConstants.POST_LEDGER, assetLedger.getIdentifier()));

    final Account assetAccount =
            AccountGenerator.createAccount(assetLedger.getIdentifier(), "7077", AccountType.ASSET);
    super.testSubject.createAccount(assetAccount);
    Assert.assertTrue(super.eventRecorder.wait(EventConstants.POST_ACCOUNT, assetAccount.getIdentifier()));

    final Ledger liabilityLedger = LedgerGenerator.createLedger("8070", AccountType.LIABILITY);
    super.testSubject.createLedger(liabilityLedger);
    Assert.assertTrue(super.eventRecorder.wait(EventConstants.POST_LEDGER, liabilityLedger.getIdentifier()));

    final Account liabilityAccount =
            AccountGenerator.createAccount(liabilityLedger.getIdentifier(), "8077", AccountType.LIABILITY);
    super.testSubject.createAccount(liabilityAccount);
    Assert.assertTrue(super.eventRecorder.wait(EventConstants.POST_ACCOUNT, liabilityAccount.getIdentifier()));
  }

  private void sampleJournalEntries ( ) throws Exception {
    final JournalEntry firstTransaction =
            JournalEntryGenerator
                    .createRandomJournalEntry("7077", "150.00", "1170", "150.00");
    super.testSubject.createJournalEntry(firstTransaction);
    Assert.assertTrue(super.eventRecorder.wait(EventConstants.POST_JOURNAL_ENTRY, firstTransaction.getTransactionIdentifier()));

    final JournalEntry secondTransaction =
            JournalEntryGenerator
                    .createRandomJournalEntry("7077", "200.00", "1370", "200.00");
    super.testSubject.createJournalEntry(secondTransaction);
    Assert.assertTrue(super.eventRecorder.wait(EventConstants.POST_JOURNAL_ENTRY, secondTransaction.getTransactionIdentifier()));

    final JournalEntry thirdTransaction =
            JournalEntryGenerator
                    .createRandomJournalEntry("3570", "50.00", "8077", "50.00");
    super.testSubject.createJournalEntry(thirdTransaction);
    Assert.assertTrue(super.eventRecorder.wait(EventConstants.POST_JOURNAL_ENTRY, thirdTransaction.getTransactionIdentifier()));

    final JournalEntry fourthTransaction =
            JournalEntryGenerator
                    .createRandomJournalEntry("3770", "75.00", "8077", "75.00");
    super.testSubject.createJournalEntry(fourthTransaction);
    Assert.assertTrue(super.eventRecorder.wait(EventConstants.POST_JOURNAL_ENTRY, fourthTransaction.getTransactionIdentifier()));
  }
}
