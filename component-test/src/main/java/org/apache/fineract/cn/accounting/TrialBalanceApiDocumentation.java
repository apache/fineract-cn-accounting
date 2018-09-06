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

public class TrialBalanceApiDocumentation extends AbstractAccountingTest {

  @Rule
  public final JUnitRestDocumentation restDocumentation = new JUnitRestDocumentation("build/doc/generated-snippets/test-trial-balance");

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
  public void documentGenerateTrialBalance ( ) throws Exception {
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

    this.mockMvc.perform(get("/trialbalance")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .accept(MediaType.ALL_VALUE))
            .andExpect(status().isOk())
            .andDo(document("document-show-chart-of-accounts", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()),
                    responseFields(
                            fieldWithPath("trialBalanceEntries[].ledger.identifier").description("String").description("First Trial Balance Entry Identifier"),
                            fieldWithPath("trialBalanceEntries[].ledger.type").description("Type").description("Type of trial balance " +
                                    " + \n" +
                                    " + \n" +
                                    " *enum* _Type_ { + \n" +
                                    "  DEBIT, + \n" +
                                    "  CREDIT, + \n" +
                                    "}"),
                            fieldWithPath("trialBalanceEntries[].ledger.name").description("String").description("Ledger Name"),
                            fieldWithPath("trialBalanceEntries[].ledger.description").description("String").description("Description of Ledger"),
                            fieldWithPath("trialBalanceEntries[].ledger.parentLedgerIdentifier").description("String").description("Parent Ledger"),
                            fieldWithPath("trialBalanceEntries[].ledger.subLedgers").description("String").description("Name of Subledger"),
                            fieldWithPath("trialBalanceEntries[].ledger.totalValue").description("BigDecimal").description("Total Value"),
                            fieldWithPath("trialBalanceEntries[].ledger.createdOn").description("String").description("Creation Date"),
                            fieldWithPath("trialBalanceEntries[].ledger.createdBy").description("String").description("Employee Who Created"),
                            fieldWithPath("trialBalanceEntries[].ledger.lastModifiedOn").description("String").description("Last Modified Date"),
                            fieldWithPath("trialBalanceEntries[].ledger.lastModifiedBy").description("String").description("Empoyee Who Last Modified"),
                            fieldWithPath("trialBalanceEntries[].ledger.showAccountsInChart").description("String").description("Should We Show Chart of Accounts"),
                            fieldWithPath("trialBalanceEntries[].type").description("Type").description("Type of trial balance entry " +
                                    " + \n" +
                                    " + \n" +
                                    " *enum* _Type_ { + \n" +
                                    "  DEBIT, + \n" +
                                    "  CREDIT, + \n" +
                                    "}"),
                            fieldWithPath("trialBalanceEntries[].amount").description("BigDecimal").description("Amount"),
                            fieldWithPath("trialBalanceEntries[1].ledger.identifier").description("String").description("Second Trial Balance Entry Identifier"),
                            fieldWithPath("trialBalanceEntries[1].ledger.type").description("Type").description("Type of trial balance " +
                                    " + \n" +
                                    " + \n" +
                                    " *enum* _Type_ { + \n" +
                                    "  DEBIT, + \n" +
                                    "  CREDIT, + \n" +
                                    "}"),
                            fieldWithPath("trialBalanceEntries[1].ledger.name").description("String").description("Ledger Name"),
                            fieldWithPath("trialBalanceEntries[1].ledger.description").description("String").description("Description of Ledger"),
                            fieldWithPath("trialBalanceEntries[1].ledger.parentLedgerIdentifier").description("String").description("Parent Ledger"),
                            fieldWithPath("trialBalanceEntries[1].ledger.subLedgers").description("String").description("Name of Subledger"),
                            fieldWithPath("trialBalanceEntries[1].ledger.totalValue").description("BigDecimal").description("Total Value"),
                            fieldWithPath("trialBalanceEntries[1].ledger.createdOn").description("String").description("Creation Date"),
                            fieldWithPath("trialBalanceEntries[1].ledger.createdBy").description("String").description("Employee Who Created"),
                            fieldWithPath("trialBalanceEntries[1].ledger.lastModifiedOn").description("String").description("Last Modified Date"),
                            fieldWithPath("trialBalanceEntries[1].ledger.lastModifiedBy").description("String").description("Empoyee Who Last Modified"),
                            fieldWithPath("trialBalanceEntries[1].ledger.showAccountsInChart").description("String").description("Should We Show Chart of Accounts"),
                            fieldWithPath("trialBalanceEntries[1].type").description("Type").description("Type of trial balance entry " +
                                    " + \n" +
                                    " + \n" +
                                    " *enum* _Type_ { + \n" +
                                    "  DEBIT, + \n" +
                                    "  CREDIT, + \n" +
                                    "}"),
                            fieldWithPath("trialBalanceEntries[1].amount").description("BigDecimal").description("Amount"),
                            fieldWithPath("trialBalanceEntries[1].ledger.identifier").description("String").description("Third Trial Balance Entry Identifier"),
                            fieldWithPath("trialBalanceEntries[2].ledger.type").description("Type").description("Type of trial balance " +
                                    " + \n" +
                                    " + \n" +
                                    " *enum* _Type_ { + \n" +
                                    "  DEBIT, + \n" +
                                    "  CREDIT, + \n" +
                                    "}"),
                            fieldWithPath("trialBalanceEntries[2].ledger.name").description("String").description("Ledger Name"),
                            fieldWithPath("trialBalanceEntries[2].ledger.description").description("String").description("Description of Ledger"),
                            fieldWithPath("trialBalanceEntries[2].ledger.parentLedgerIdentifier").description("String").description("Parent Ledger"),
                            fieldWithPath("trialBalanceEntries[2].ledger.subLedgers").description("String").description("Name of Subledger"),
                            fieldWithPath("trialBalanceEntries[2].ledger.totalValue").description("BigDecimal").description("Total Value"),
                            fieldWithPath("trialBalanceEntries[2].ledger.createdOn").description("String").description("Creation Date"),
                            fieldWithPath("trialBalanceEntries[2].ledger.createdBy").description("String").description("Employee Who Created"),
                            fieldWithPath("trialBalanceEntries[2].ledger.lastModifiedOn").description("String").description("Last Modified Date"),
                            fieldWithPath("trialBalanceEntries[2].ledger.lastModifiedBy").description("String").description("Empoyee Who Last Modified"),
                            fieldWithPath("trialBalanceEntries[2].ledger.showAccountsInChart").description("String").description("Should We Show Chart of Accounts"),
                            fieldWithPath("trialBalanceEntries[2].type").description("Type").description("Type of trial balance entry " +
                                    " + \n" +
                                    " + \n" +
                                    " *enum* _Type_ { + \n" +
                                    "  DEBIT, + \n" +
                                    "  CREDIT, + \n" +
                                    "}"),
                            fieldWithPath("trialBalanceEntries[2].amount").description("BigDecimal").description("Amount"),
                            fieldWithPath("debitTotal").type("BigDecimal").description("Total Debit"),
                            fieldWithPath("creditTotal").type("BigDecimal").description("Total Credit")
                    )));
  }
}
