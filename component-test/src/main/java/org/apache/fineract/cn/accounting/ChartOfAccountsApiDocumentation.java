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
import org.apache.fineract.cn.accounting.api.v1.domain.Ledger;
import org.apache.fineract.cn.accounting.util.AccountGenerator;
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

public class ChartOfAccountsApiDocumentation extends AbstractAccountingTest {

  @Rule
  public final JUnitRestDocumentation restDocumentation = new JUnitRestDocumentation("build/doc/generated-snippets/test-chart-of-accounts");

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
  public void documentShowChartOfAccounts ( ) throws Exception {
    final Ledger parentRevenueLedger = LedgerGenerator.createLedger("10000", AccountType.REVENUE);
    parentRevenueLedger.setName("Parent Revenue");
    parentRevenueLedger.setDescription("Parent Revenue Ledger");
    this.testSubject.createLedger(parentRevenueLedger);
    this.eventRecorder.wait(EventConstants.POST_LEDGER, parentRevenueLedger.getIdentifier());

    final Ledger interestRevenueLedger = LedgerGenerator.createLedger("11000", AccountType.REVENUE);
    interestRevenueLedger.setName("Interest Revenue");
    interestRevenueLedger.setDescription("Interest Revenue Ledger");
    this.testSubject.addSubLedger(parentRevenueLedger.getIdentifier(), interestRevenueLedger);
    this.eventRecorder.wait(EventConstants.POST_LEDGER, interestRevenueLedger.getIdentifier());

    final Account consumerInterestRevenueAccount =
            AccountGenerator.createAccount(interestRevenueLedger.getIdentifier(), "11100", AccountType.REVENUE);
    consumerInterestRevenueAccount.setName("Consumer Interest");
    this.testSubject.createAccount(consumerInterestRevenueAccount);
    this.eventRecorder.wait(EventConstants.POST_ACCOUNT, consumerInterestRevenueAccount.getIdentifier());

    final Ledger feeRevenueLedger = LedgerGenerator.createLedger("12000", AccountType.REVENUE);
    feeRevenueLedger.setName("Fee Revenue");
    feeRevenueLedger.setDescription("Fee Revenue Ledger");
    this.testSubject.addSubLedger(parentRevenueLedger.getIdentifier(), feeRevenueLedger);
    this.eventRecorder.wait(EventConstants.POST_LEDGER, feeRevenueLedger.getIdentifier());

    final Ledger specialFeeRevenueLedger = LedgerGenerator.createLedger("12100", AccountType.REVENUE);
    specialFeeRevenueLedger.setName("Special Fee Revenue");
    specialFeeRevenueLedger.setDescription("Special Fee Revenue");
    this.testSubject.addSubLedger(feeRevenueLedger.getIdentifier(), specialFeeRevenueLedger);
    this.eventRecorder.wait(EventConstants.POST_LEDGER, specialFeeRevenueLedger.getIdentifier());

    final Ledger parentAssetLedger = LedgerGenerator.createLedger("70000", AccountType.ASSET);
    parentAssetLedger.setName("Parent Asset");
    parentAssetLedger.setDescription("Parent Asset Ledger");
    this.testSubject.createLedger(parentAssetLedger);
    this.eventRecorder.wait(EventConstants.POST_LEDGER, parentAssetLedger.getIdentifier());

    final Ledger consumerLoanAssetLedger = LedgerGenerator.createLedger("73000", AccountType.ASSET);
    consumerInterestRevenueAccount.setName("Consumer Loan");
    consumerLoanAssetLedger.setDescription("Consumer Loan Asset Ledger");
    consumerLoanAssetLedger.setShowAccountsInChart(Boolean.FALSE);
    this.testSubject.addSubLedger(parentAssetLedger.getIdentifier(), consumerLoanAssetLedger);
    this.eventRecorder.wait(EventConstants.POST_LEDGER, consumerLoanAssetLedger.getIdentifier());

    for (int i = 1; i < 100; i++) {
      final String identifier = Integer.valueOf(73000 + i).toString();
      final Account consumerLoanAccount =
              AccountGenerator.createAccount(consumerLoanAssetLedger.getIdentifier(), identifier, AccountType.ASSET);
      this.testSubject.createAccount(consumerLoanAccount);
      this.eventRecorder.wait(EventConstants.POST_ACCOUNT, identifier);
    }

    this.mockMvc.perform(get("/chartofaccounts")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .accept(MediaType.ALL_VALUE))
            .andExpect(status().isOk())
            .andDo(document("document-show-chart-of-accounts", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()),
                    responseFields(
                            fieldWithPath("[].code").description("String").description("Date of Financial Condition Preparation"),
                            fieldWithPath("[].name").description("String").description("Type of assets section " +
                                    " + \n" +
                                    " + \n" +
                                    " *enum* _Type_ { + \n" +
                                    "  ASSET, + \n" +
                                    "  EQUITY, + \n" +
                                    "  LIABILITY, + \n" +
                                    "}"),
                            fieldWithPath("[].description").type("String").description("first section's description"),
                            fieldWithPath("[].type").type("String").description("first entry's description"),
                            fieldWithPath("[].level").type("Integer").description("first entry's value"),
                            fieldWithPath("[1].code").description("String").description("Date of Financial Condition Preparation"),
                            fieldWithPath("[1].name").description("String").description("Type of assets section " +
                                    " + \n" +
                                    " + \n" +
                                    " *enum* _Type_ { + \n" +
                                    "  ASSET, + \n" +
                                    "  EQUITY, + \n" +
                                    "  LIABILITY, + \n" +
                                    "}"),
                            fieldWithPath("[1].type").type("String").description("first entry's description"),
                            fieldWithPath("[1].level").type("Integer").description("first entry's value"),
                            fieldWithPath("[2].code").description("String").description("Date of Financial Condition Preparation"),
                            fieldWithPath("[2].name").description("String").description("Type of assets section " +
                                    " + \n" +
                                    " + \n" +
                                    " *enum* _Type_ { + \n" +
                                    "  ASSET, + \n" +
                                    "  EQUITY, + \n" +
                                    "  LIABILITY, + \n" +
                                    "}"),
                            fieldWithPath("[2].type").type("String").description("first entry's description"),
                            fieldWithPath("[2].level").type("Integer").description("first entry's value"),
                            fieldWithPath("[3].code").description("String").description("Date of Financial Condition Preparation"),
                            fieldWithPath("[3].name").description("String").description("Type of assets section " +
                                    " + \n" +
                                    " + \n" +
                                    " *enum* _Type_ { + \n" +
                                    "  ASSET, + \n" +
                                    "  EQUITY, + \n" +
                                    "  LIABILITY, + \n" +
                                    "}"),
                            fieldWithPath("[3].type").type("String").description("first entry's description"),
                            fieldWithPath("[3].level").type("Integer").description("first entry's value"),
                            fieldWithPath("[4].code").description("String").description("Date of Financial Condition Preparation"),
                            fieldWithPath("[4].name").description("String").description("Type of assets section " +
                                    " + \n" +
                                    " + \n" +
                                    " *enum* _Type_ { + \n" +
                                    "  ASSET, + \n" +
                                    "  EQUITY, + \n" +
                                    "  LIABILITY, + \n" +
                                    "}"),
                            fieldWithPath("[4].type").type("String").description("first entry's description"),
                            fieldWithPath("[4].level").type("Integer").description("first entry's value"),
                            fieldWithPath("[5].code").description("String").description("Date of Financial Condition Preparation"),
                            fieldWithPath("[5].name").description("String").description("Type of assets section " +
                                    " + \n" +
                                    " + \n" +
                                    " *enum* _Type_ { + \n" +
                                    "  ASSET, + \n" +
                                    "  EQUITY, + \n" +
                                    "  LIABILITY, + \n" +
                                    "}"),
                            fieldWithPath("[5].description").type("String").description("first section's description"),
                            fieldWithPath("[5].type").type("String").description("first entry's description"),
                            fieldWithPath("[5].level").type("Integer").description("first entry's value"),
                            fieldWithPath("[6].code").description("String").description("Date of Financial Condition Preparation"),
                            fieldWithPath("[6].name").description("String").description("Type of assets section " +
                                    " + \n" +
                                    " + \n" +
                                    " *enum* _Type_ { + \n" +
                                    "  ASSET, + \n" +
                                    "  EQUITY, + \n" +
                                    "  LIABILITY, + \n" +
                                    "}"),
                            fieldWithPath("[6].type").type("String").description("first entry's description"),
                            fieldWithPath("[6].level").type("Integer").description("first entry's value")
                    )));
  }
}
