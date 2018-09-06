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

public class FinancialConditionApiDocumentation extends AbstractAccountingTest {

  @Rule
  public final JUnitRestDocumentation restDocumentation = new JUnitRestDocumentation("build/doc/generated-snippets/test-financial-condition");

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
  public void documentReturnFinancialCondition ( ) throws Exception {
    this.fixtures();
    this.sampleJournalEntries();

    this.mockMvc.perform(get("/financialcondition")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .accept(MediaType.ALL_VALUE))
            .andExpect(status().isOk())
            .andDo(document("document-return-financial-condition", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()),
                    responseFields(
                            fieldWithPath("date").description("String").description("Date of Financial Condition Preparation"),
                            fieldWithPath("financialConditionSections[].type").description("Type").description("Type of assets section " +
                                    " + \n" +
                                    " + \n" +
                                    " *enum* _Type_ { + \n" +
                                    "  ASSET, + \n" +
                                    "  EQUITY, + \n" +
                                    "  LIABILITY, + \n" +
                                    "}"),
                            fieldWithPath("financialConditionSections[].description").type("String").description("first section's description"),
                            fieldWithPath("financialConditionSections[].financialConditionEntries[].description").type("String").description("first entry's description"),
                            fieldWithPath("financialConditionSections[].financialConditionEntries[].value").type("BigDecimal").description("first entry's value"),
                            fieldWithPath("financialConditionSections[].financialConditionEntries[1].description").type("String").description("second entry's description"),
                            fieldWithPath("financialConditionSections[].financialConditionEntries[1].value").type("BigDecimal").description("second entry's value"),
                            fieldWithPath("financialConditionSections[].subtotal").type("BigDecimal").description("First section's subtotal"),
                            fieldWithPath("financialConditionSections[1].type").description("Type").description("Type of first section " +
                                    " + \n" +
                                    " + \n" +
                                    " *enum* _Type_ { + \n" +
                                    "  INCOME, + \n" +
                                    "  EXPENSES + \n" +
                                    "}"),
                            fieldWithPath("financialConditionSections[1].description").type("String").description("first section's description"),
                            fieldWithPath("financialConditionSections[1].financialConditionEntries[].description").type("String").description("first entry's description"),
                            fieldWithPath("financialConditionSections[1].financialConditionEntries[].value").type("BigDecimal").description("first entry's value"),
                            fieldWithPath("financialConditionSections[1].subtotal").type("BigDecimal").description("Second section's subtotal"),
                            fieldWithPath("financialConditionSections[2].type").description("Type").description("Type of Equity & liability section " +
                                    " + \n" +
                                    " + \n" +
                                    " *enum* _Type_ { + \n" +
                                    "  ASSET, + \n" +
                                    "  EQUITY, + \n" +
                                    "  LIABILITY, + \n" +
                                    "}"),
                            fieldWithPath("financialConditionSections[2].description").type("String").description("liability section's description"),
                            fieldWithPath("financialConditionSections[2].financialConditionEntries[].description").type("String").description("first entry's description"),
                            fieldWithPath("financialConditionSections[2].financialConditionEntries[].value").type("BigDecimal").description("first entry's value"),
                            fieldWithPath("financialConditionSections[2].financialConditionEntries[1].description").type("String").description("second entry's description"),
                            fieldWithPath("financialConditionSections[2].financialConditionEntries[1].value").type("BigDecimal").description("second entry's value"),
                            fieldWithPath("financialConditionSections[2].subtotal").type("BigDecimal").description("First section's subtotal"),
                            fieldWithPath("totalAssets").type("BigDecimal").description("Gross Profit"),
                            fieldWithPath("totalEquitiesAndLiabilities").type("BigDecimal").description("Total Expenses")
                    )));
  }

  private void fixtures ( ) throws Exception {
    final Ledger assetLedger = LedgerGenerator.createLedger("7000", AccountType.ASSET);
    assetLedger.setName("Assets");
    super.testSubject.createLedger(assetLedger);
    Assert.assertTrue(super.eventRecorder.wait(EventConstants.POST_LEDGER, assetLedger.getIdentifier()));

    final Ledger assetSubLedger7060 = LedgerGenerator.createLedger("7060", AccountType.ASSET);
    assetSubLedger7060.setParentLedgerIdentifier(assetLedger.getParentLedgerIdentifier());
    assetSubLedger7060.setName("Loans to Members");
    super.testSubject.addSubLedger(assetLedger.getIdentifier(), assetSubLedger7060);
    Assert.assertTrue(super.eventRecorder.wait(EventConstants.POST_LEDGER, assetSubLedger7060.getIdentifier()));

    final Ledger assetSubLedger7080 = LedgerGenerator.createLedger("7080", AccountType.ASSET);
    assetSubLedger7080.setParentLedgerIdentifier(assetLedger.getParentLedgerIdentifier());
    assetSubLedger7080.setName("Lines of Credit to Members");
    super.testSubject.addSubLedger(assetLedger.getIdentifier(), assetSubLedger7080);
    Assert.assertTrue(super.eventRecorder.wait(EventConstants.POST_LEDGER, assetSubLedger7080.getIdentifier()));

    final Account firstAssetAccount =
            AccountGenerator.createAccount(assetSubLedger7060.getIdentifier(), "7061", AccountType.ASSET);
    super.testSubject.createAccount(firstAssetAccount);
    Assert.assertTrue(super.eventRecorder.wait(EventConstants.POST_ACCOUNT, firstAssetAccount.getIdentifier()));

    final Account secondAssetAccount =
            AccountGenerator.createAccount(assetSubLedger7080.getIdentifier(), "7081", AccountType.ASSET);
    super.testSubject.createAccount(secondAssetAccount);
    Assert.assertTrue(super.eventRecorder.wait(EventConstants.POST_ACCOUNT, secondAssetAccount.getIdentifier()));

    final Ledger liabilityLedger = LedgerGenerator.createLedger("8000", AccountType.LIABILITY);
    liabilityLedger.setName("Liabilities");
    super.testSubject.createLedger(liabilityLedger);
    Assert.assertTrue(super.eventRecorder.wait(EventConstants.POST_LEDGER, liabilityLedger.getIdentifier()));

    final Ledger liabilitySubLedger8110 = LedgerGenerator.createLedger("8110", AccountType.LIABILITY);
    liabilitySubLedger8110.setParentLedgerIdentifier(liabilityLedger.getParentLedgerIdentifier());
    liabilitySubLedger8110.setName("Accounts Payable");
    super.testSubject.addSubLedger(liabilityLedger.getIdentifier(), liabilitySubLedger8110);
    Assert.assertTrue(super.eventRecorder.wait(EventConstants.POST_LEDGER, liabilitySubLedger8110.getIdentifier()));

    final Ledger liabilitySubLedger8220 = LedgerGenerator.createLedger("8220", AccountType.LIABILITY);
    liabilitySubLedger8220.setParentLedgerIdentifier(liabilityLedger.getParentLedgerIdentifier());
    liabilitySubLedger8220.setName("Interest Payable");
    super.testSubject.addSubLedger(liabilityLedger.getIdentifier(), liabilitySubLedger8220);
    Assert.assertTrue(super.eventRecorder.wait(EventConstants.POST_LEDGER, liabilitySubLedger8220.getIdentifier()));

    final Account firstLiabilityAccount =
            AccountGenerator.createAccount(liabilitySubLedger8110.getIdentifier(), "8110", AccountType.LIABILITY);
    super.testSubject.createAccount(firstLiabilityAccount);
    Assert.assertTrue(super.eventRecorder.wait(EventConstants.POST_ACCOUNT, firstLiabilityAccount.getIdentifier()));

    final Account secondLiabilityAccount =
            AccountGenerator.createAccount(liabilitySubLedger8220.getIdentifier(), "8220", AccountType.LIABILITY);
    super.testSubject.createAccount(secondLiabilityAccount);
    Assert.assertTrue(super.eventRecorder.wait(EventConstants.POST_ACCOUNT, secondLiabilityAccount.getIdentifier()));

    final Ledger equityLedger = LedgerGenerator.createLedger("9000", AccountType.EQUITY);
    equityLedger.setName("Equities");
    super.testSubject.createLedger(equityLedger);
    Assert.assertTrue(super.eventRecorder.wait(EventConstants.POST_LEDGER, equityLedger.getIdentifier()));

    final Ledger equitySubLedger9120 = LedgerGenerator.createLedger("9120", AccountType.EQUITY);
    equitySubLedger9120.setParentLedgerIdentifier(equityLedger.getParentLedgerIdentifier());
    equitySubLedger9120.setName("Member Savings");
    super.testSubject.addSubLedger(equityLedger.getIdentifier(), equitySubLedger9120);
    Assert.assertTrue(super.eventRecorder.wait(EventConstants.POST_LEDGER, equitySubLedger9120.getIdentifier()));

    final Account firstEquityAccount =
            AccountGenerator.createAccount(equitySubLedger9120.getIdentifier(), "9120", AccountType.EQUITY);
    super.testSubject.createAccount(firstEquityAccount);
    Assert.assertTrue(super.eventRecorder.wait(EventConstants.POST_ACCOUNT, firstEquityAccount.getIdentifier()));
  }

  private void sampleJournalEntries ( ) throws Exception {
    final JournalEntry firstTransaction =
            JournalEntryGenerator
                    .createRandomJournalEntry("7061", "150.00", "8110", "150.00");
    super.testSubject.createJournalEntry(firstTransaction);
    Assert.assertTrue(super.eventRecorder.wait(EventConstants.POST_JOURNAL_ENTRY, firstTransaction.getTransactionIdentifier()));

    final JournalEntry secondTransaction =
            JournalEntryGenerator
                    .createRandomJournalEntry("7081", "100.00", "8220", "100.00");
    super.testSubject.createJournalEntry(secondTransaction);
    Assert.assertTrue(super.eventRecorder.wait(EventConstants.POST_JOURNAL_ENTRY, secondTransaction.getTransactionIdentifier()));

    final JournalEntry thirdTransaction =
            JournalEntryGenerator
                    .createRandomJournalEntry("8220", "50.00", "9120", "50.00");
    super.testSubject.createJournalEntry(thirdTransaction);
    Assert.assertTrue(super.eventRecorder.wait(EventConstants.POST_JOURNAL_ENTRY, thirdTransaction.getTransactionIdentifier()));
  }

}
