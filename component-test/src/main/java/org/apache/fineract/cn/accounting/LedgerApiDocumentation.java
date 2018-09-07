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

import com.google.gson.Gson;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.fineract.cn.accounting.api.v1.EventConstants;
import org.apache.fineract.cn.accounting.api.v1.domain.*;
import org.apache.fineract.cn.accounting.util.AccountGenerator;
import org.apache.fineract.cn.accounting.util.LedgerGenerator;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.restdocs.JUnitRestDocumentation;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class LedgerApiDocumentation extends AbstractAccountingTest {

  @Rule
  public final JUnitRestDocumentation restDocumentation = new JUnitRestDocumentation("build/doc/generated-snippets/test-ledger");

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
  public void documentCreateLedger ( ) throws Exception {

    final Ledger ledger = LedgerGenerator.createRandomLedger();

    ledger.setType(AccountType.ASSET.name());
    ledger.setIdentifier("1000");
    ledger.setName("Cash");
    ledger.setDescription("Cash Ledger");
    ledger.setShowAccountsInChart(Boolean.TRUE);

    Gson gson = new Gson();
    this.mockMvc.perform(post("/ledgers")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .content(gson.toJson(ledger)))
            .andExpect(status().isAccepted())
            .andDo(document("document-create-ledger", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()),
                    requestFields(
                            fieldWithPath("type").description("Type of ledger " +
                                    " + \n" +
                                    " + \n" +
                                    " *enum* _AccountType_ { + \n" +
                                    "  ASSET, + \n" +
                                    "  LIABILITY, + \n" +
                                    "  EQUITY, + \n" +
                                    "  REVENUE, + \n" +
                                    "  EXPENSE + \n" +
                                    "}"),
                            fieldWithPath("identifier").description("Account identifier"),
                            fieldWithPath("name").description("Name of account"),
                            fieldWithPath("description").description("Description of account"),
                            fieldWithPath("showAccountsInChart").type("Boolean").description("Should account be shown in charts ?")
                    )));
  }

  @Test
  public void documentFetchLedgers ( ) throws Exception {

    final Ledger ledgerOne = LedgerGenerator.createRandomLedger();
    ledgerOne.setIdentifier("1021");
    ledgerOne.setName("Name of " + ledgerOne.getIdentifier());
    ledgerOne.setDescription("Description of " + ledgerOne.getIdentifier());
    ledgerOne.setShowAccountsInChart(Boolean.TRUE);

    final Ledger ledgerTwo = LedgerGenerator.createRandomLedger();
    ledgerTwo.setIdentifier("1022");
    ledgerTwo.setName("Name of " + ledgerTwo.getIdentifier());
    ledgerTwo.setDescription("Description of " + ledgerTwo.getIdentifier());
    ledgerTwo.setShowAccountsInChart(Boolean.FALSE);

    final Ledger ledgerThree = LedgerGenerator.createRandomLedger();
    ledgerThree.setIdentifier("1023");
    ledgerThree.setName("Name of " + ledgerThree.getIdentifier());
    ledgerThree.setDescription("Description of " + ledgerThree.getIdentifier());
    ledgerThree.setShowAccountsInChart(Boolean.TRUE);

    List <Ledger> ledgerList = new ArrayList <>();
    Stream.of(ledgerOne, ledgerTwo, ledgerThree).forEach(ledger -> {
      ledgerList.add(ledger);
    });

    ledgerList.stream()
            .forEach(ledger -> {
              this.testSubject.createLedger(ledger);
              try {
                this.eventRecorder.wait(EventConstants.POST_LEDGER, ledger.getIdentifier());
              } catch (InterruptedException e) {
                e.printStackTrace();
              }
            });

    this.mockMvc.perform(get("/ledgers")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .accept(MediaType.ALL_VALUE))
            .andExpect(status().isOk())
            .andDo(document("document-fetch-ledgers", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()),
                    responseFields(
                            fieldWithPath("ledgers").type("List<Ledger>").description("List of Ledgers"),
                            fieldWithPath("ledgers[].type").description("AccountType").description("Type of first ledger " +
                                    " + \n" +
                                    " + \n" +
                                    " *enum* _AccountType_ { + \n" +
                                    "  ASSET, + \n" +
                                    "  LIABILITY, + \n" +
                                    "  EQUITY, + \n" +
                                    "  REVENUE, + \n" +
                                    "  EXPENSE + \n" +
                                    "}"),
                            fieldWithPath("ledgers[].identifier").type("String").description("first ledger identifier"),
                            fieldWithPath("ledgers[].name").type("String").description("first ledger name"),
                            fieldWithPath("ledgers[].description").type("String").description("description of first ledger"),
                            fieldWithPath("ledgers[].parentLedgerIdentifier").description("first ledger's parent "),
                            fieldWithPath("ledgers[].totalValue").type("String").description("Total Value of first ledger"),
                            fieldWithPath("ledgers[].createdOn").type("String").description("date first ledger was created"),
                            fieldWithPath("ledgers[].createdBy").type("String").description("employee who created first ledger"),
                            fieldWithPath("ledgers[].lastModifiedOn").type("String").description("date first ledger was modified"),
                            fieldWithPath("ledgers[].lastModifiedBy").type("String").description("employee who last modified first ledger"),
                            fieldWithPath("ledgers[].showAccountsInChart").type("Boolean").description("Should ledger be shown in charts ?"),
                            fieldWithPath("ledgers[1].type").description("AccountType").description("Type of second ledger " +
                                    " + \n" +
                                    " + \n" +
                                    " *enum* _AccountType_ { + \n" +
                                    "  ASSET, + \n" +
                                    "  LIABILITY, + \n" +
                                    "  EQUITY, + \n" +
                                    "  REVENUE, + \n" +
                                    "  EXPENSE + \n" +
                                    "}"),
                            fieldWithPath("ledgers[1].identifier").type("String").description("second ledger identifier"),
                            fieldWithPath("ledgers[1].name").type("String").description("second ledger name"),
                            fieldWithPath("ledgers[1].description").type("String").description("description of second ledger"),
                            fieldWithPath("ledgers[1].parentLedgerIdentifier").description("second ledger's parent "),
                            fieldWithPath("ledgers[1].totalValue").type("String").description("Total Value of second ledger"),
                            fieldWithPath("ledgers[1].createdOn").type("String").description("date second ledger was created"),
                            fieldWithPath("ledgers[1].createdBy").type("String").description("employee who created second ledger"),
                            fieldWithPath("ledgers[1].lastModifiedOn").type("String").description("date second ledger was modified"),
                            fieldWithPath("ledgers[1].lastModifiedBy").type("String").description("employee who last modified second ledger"),
                            fieldWithPath("ledgers[1].showAccountsInChart").type("Boolean").description("Should ledger be shown in charts ?"),
                            fieldWithPath("ledgers[2].type").description("AccountType").description("Type of third ledger " +
                                    " + \n" +
                                    " + \n" +
                                    " *enum* _AccountType_ { + \n" +
                                    "  ASSET, + \n" +
                                    "  LIABILITY, + \n" +
                                    "  EQUITY, + \n" +
                                    "  REVENUE, + \n" +
                                    "  EXPENSE + \n" +
                                    "}"),
                            fieldWithPath("ledgers[2].identifier").type("String").description("third ledger identifier"),
                            fieldWithPath("ledgers[2].name").type("String").description("third ledger name"),
                            fieldWithPath("ledgers[2].description").type("String").description("description of third ledger"),
                            fieldWithPath("ledgers[2].parentLedgerIdentifier").description("third ledger's parent "),
                            fieldWithPath("ledgers[2].totalValue").type("String").description("Total Value of third ledger"),
                            fieldWithPath("ledgers[2].createdOn").type("String").description("date third ledger was created"),
                            fieldWithPath("ledgers[2].createdBy").type("String").description("employee who created third ledger"),
                            fieldWithPath("ledgers[2].lastModifiedOn").type("String").description("date second ledger was modified"),
                            fieldWithPath("ledgers[2].lastModifiedBy").type("String").description("employee who last modified third ledger"),
                            fieldWithPath("ledgers[2].showAccountsInChart").type("Boolean").description("Should ledger be shown in charts ?"),
                            fieldWithPath("totalPages").type("Integer").description("Total number of pages"),
                            fieldWithPath("totalElements").type("String").description("Total number of elements")
                    )));
  }

  @Test
  public void documentFindLedger ( ) throws Exception {

    final Ledger ledger = LedgerGenerator.createRandomLedger();
    ledger.setIdentifier("7200");
    ledger.setName("Name of" + ledger.getIdentifier());
    ledger.setDescription("Description of " + ledger.getIdentifier());
    this.testSubject.createLedger(ledger);
    this.eventRecorder.wait(EventConstants.POST_LEDGER, ledger.getIdentifier());

    Gson gson = new Gson();
    this.mockMvc.perform(get("/ledgers/" + ledger.getIdentifier())
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .accept(MediaType.ALL_VALUE))
            .andExpect(status().isOk())
            .andDo(document("document-find-ledger", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()),
                    responseFields(
                            fieldWithPath("type").description("AccountType").description("Type of first ledger " +
                                    " + \n" +
                                    " + \n" +
                                    " *enum* _AccountType_ { + \n" +
                                    "  ASSET, + \n" +
                                    "  LIABILITY, + \n" +
                                    "  EQUITY, + \n" +
                                    "  REVENUE, + \n" +
                                    "  EXPENSE + \n" +
                                    "}"),
                            fieldWithPath("identifier").type("String").description("first ledger identifier"),
                            fieldWithPath("name").type("String").description("first ledger name"),
                            fieldWithPath("description").type("String").description("description of first ledger"),
                            fieldWithPath("subLedgers").type("List<Ledger>").description("list of sub ledgers"),
                            fieldWithPath(".parentLedgerIdentifier").description("first ledger's parent "),
                            fieldWithPath("totalValue").type("String").description("Total Value of first ledger"),
                            fieldWithPath("createdOn").type("String").description("date first ledger was created"),
                            fieldWithPath("createdBy").type("String").description("employee who created first ledger"),
                            fieldWithPath("lastModifiedOn").type("String").description("date first ledger was modified"),
                            fieldWithPath("lastModifiedBy").type("String").description("employee who last modified first ledger"),
                            fieldWithPath("showAccountsInChart").type("Boolean").description("Should ledger be shown in charts ?")
                    )));
  }

  @Test
  public void documentAddSubLedger ( ) throws Exception {

    final Ledger parentLedger = LedgerGenerator.createRandomLedger();
    parentLedger.setIdentifier("6220");
    parentLedger.setName("Name of" + parentLedger.getIdentifier());
    parentLedger.setDescription("Description of " + parentLedger.getIdentifier());
    this.testSubject.createLedger(parentLedger);
    this.eventRecorder.wait(EventConstants.POST_LEDGER, parentLedger.getIdentifier());

    final Ledger subLedger = LedgerGenerator.createRandomLedger();
    subLedger.setIdentifier("6221");
    subLedger.setName("SubLedger One of " + parentLedger.getIdentifier());
    subLedger.setDescription("First Sub Ledger of " + parentLedger.getIdentifier());

    Gson gson = new Gson();
    this.mockMvc.perform(post("/ledgers/" + parentLedger.getIdentifier())
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .content(gson.toJson(subLedger)))
            .andExpect(status().isAccepted())
            .andDo(document("document-add-sub-ledger", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()),
                    requestFields(
                            fieldWithPath("type").description("Type of Ledger " +
                                    " + \n" +
                                    " + \n" +
                                    " *enum* _AccountType_ { + \n" +
                                    "  ASSET, + \n" +
                                    "  LIABILITY, + \n" +
                                    "  EQUITY, + \n" +
                                    "  REVENUE, + \n" +
                                    "  EXPENSE + \n" +
                                    "}"),
                            fieldWithPath("identifier").description("Sub Ledger identifier"),
                            fieldWithPath("name").description("Name of sub ledger"),
                            fieldWithPath("description").description("Description of sub ledger"),
                            fieldWithPath("showAccountsInChart").type("Boolean").description("Should ledger be shown in charts ?")
                    )));
  }

  @Test
  public void documentModifyLedger ( ) throws Exception {

    final Ledger ledger = LedgerGenerator.createRandomLedger();
    ledger.setIdentifier("6210");
    ledger.setName("Old Name Of" + ledger.getIdentifier());
    ledger.setDescription("Old Description Of " + ledger.getIdentifier());
    this.testSubject.createLedger(ledger);
    this.eventRecorder.wait(EventConstants.POST_LEDGER, ledger.getIdentifier());

    ledger.setName("New Name Of " + ledger.getIdentifier());
    ledger.setDescription("New Description Of " + ledger.getIdentifier());
    ledger.setShowAccountsInChart(Boolean.TRUE);

    Gson gson = new Gson();
    this.mockMvc.perform(put("/ledgers/" + ledger.getIdentifier())
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .content(gson.toJson(ledger)))
            .andExpect(status().isAccepted())
            .andDo(document("document-modify-ledger", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()),
                    requestFields(
                            fieldWithPath("type").description("Type of Ledger " +
                                    " + \n" +
                                    " + \n" +
                                    " *enum* _AccountType_ { + \n" +
                                    "  ASSET, + \n" +
                                    "  LIABILITY, + \n" +
                                    "  EQUITY, + \n" +
                                    "  REVENUE, + \n" +
                                    "  EXPENSE + \n" +
                                    "}"),
                            fieldWithPath("identifier").description("Sub Ledger identifier"),
                            fieldWithPath("name").description("Name of sub ledger"),
                            fieldWithPath("description").description("Description of sub ledger"),
                            fieldWithPath("showAccountsInChart").type("Boolean").description("Should ledger be shown in charts ?")
                    )));
  }

  @Test
  public void documentDeleteLedger ( ) throws Exception {

    final Ledger ledger = LedgerGenerator.createRandomLedger();
    ledger.setIdentifier("6200");
    ledger.setName("Old Name Of" + ledger.getIdentifier());
    ledger.setDescription("Old Description Of " + ledger.getIdentifier());
    this.testSubject.createLedger(ledger);
    this.eventRecorder.wait(EventConstants.POST_LEDGER, ledger.getIdentifier());

    Gson gson = new Gson();
    this.mockMvc.perform(delete("/ledgers/" + ledger.getIdentifier())
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .accept(MediaType.ALL_VALUE))
            .andExpect(status().isAccepted())
            .andDo(document("document-delete-ledger", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint())));
  }

  @Test
  public void documentFetchAccountsForLedger ( ) throws Exception {

    Set <String> holdersListOne = new HashSet <>();
    String holderOne = "Holder One";
    holdersListOne.add(holderOne);

    Set <String> signatoriesOne = new HashSet <>();
    String signatureOne = "Signatory One";
    signatoriesOne.add(signatureOne);

    final Ledger liabilityLedger = LedgerGenerator.createRandomLedger();
    liabilityLedger.setType(AccountType.LIABILITY.name());
    liabilityLedger.setIdentifier("6100");
    this.testSubject.createLedger(liabilityLedger);
    this.eventRecorder.wait(EventConstants.POST_LEDGER, liabilityLedger.getIdentifier());

    final List <Account> createdLiabilityAccounts = Stream.generate(( ) -> AccountGenerator.createRandomAccount(liabilityLedger.getIdentifier())).limit(1)
            .peek(account -> {
              account.setType(AccountType.LIABILITY.name());
              account.setIdentifier("6100.10");
              account.setName("First Account Of " + liabilityLedger.getIdentifier());
              account.setHolders(holdersListOne);
              account.setSignatureAuthorities(signatoriesOne);
              account.setBalance(1234.0);
              account.setLedger(liabilityLedger.getIdentifier());
              this.testSubject.createAccount(account);
            })
            .collect(Collectors.toList());

    createdLiabilityAccounts.stream().forEach(
            account -> {
              try {
                this.eventRecorder.wait(EventConstants.POST_ACCOUNT, account.getIdentifier());
              } catch (InterruptedException e) {
                e.printStackTrace();
              }
            });

    final AccountPage accountPage =
            this.testSubject.fetchAccounts(true, liabilityLedger.getIdentifier(), null, true,
                    0, createdLiabilityAccounts.size(), null, null);
    accountPage.setAccounts(createdLiabilityAccounts);
    accountPage.setTotalElements(new Long(createdLiabilityAccounts.size()));
    accountPage.setTotalPages(1);

    Gson gson = new Gson();
    this.mockMvc.perform(get("/ledgers/" + liabilityLedger.getIdentifier() + "/accounts")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .accept(MediaType.ALL_VALUE)
            .content(gson.toJson(accountPage)))
            .andExpect(status().isOk())
            .andDo(document("document-fetch-accounts-for-ledger", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()),
                    requestFields(
                            fieldWithPath("accounts").type("List<Account>").description("List of Accounts"),
                            fieldWithPath("accounts[].type").description("AccountType").description("Type of first Account " +
                                    " + \n" +
                                    " + \n" +
                                    " *enum* _AccountType_ { + \n" +
                                    "  ASSET, + \n" +
                                    "  LIABILITY, + \n" +
                                    "  EQUITY, + \n" +
                                    "  REVENUE, + \n" +
                                    "  EXPENSE + \n" +
                                    "}"),
                            fieldWithPath("accounts[].identifier").type("String").description("first account identifier"),
                            fieldWithPath("accounts[].name").type("String").description("first account name"),
                            fieldWithPath("accounts[].holders").type("Set<String>").description("Set of account holders"),
                            fieldWithPath("accounts[].signatureAuthorities").type("Set<String>").description("Set of signatories to account"),
                            fieldWithPath("accounts[].balance").type("Double").description("first account's balance"),
                            fieldWithPath("accounts[].ledger").type("String").description("Associated ledger"),
                            fieldWithPath("totalPages").type("Integer").description("Total pages"),
                            fieldWithPath("totalElements").type("Integer").description("Total accounts in page")
                    ),
                    responseFields(
                            fieldWithPath("accounts").type("List<Account>").description("List of Accounts"),
                            fieldWithPath("totalPages").type("Integer").description("Total number of pages"),
                            fieldWithPath("totalElements").type("String").description("Total number of elements")
                    )));
  }
}
