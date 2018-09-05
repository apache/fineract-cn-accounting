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
import org.apache.fineract.cn.accounting.api.v1.EventConstants;
import org.apache.fineract.cn.accounting.api.v1.domain.*;
import org.apache.fineract.cn.accounting.util.AccountGenerator;
import org.apache.fineract.cn.accounting.util.JournalEntryGenerator;
import org.apache.fineract.cn.accounting.util.LedgerGenerator;
import org.apache.fineract.cn.lang.DateRange;
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
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;

public class AccountApiDocumentation extends AbstractAccountingTest {

  @Rule
  public final JUnitRestDocumentation restDocumentation = new JUnitRestDocumentation("build/doc/generated-snippets/test-account");

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
  public void documentCreateAccount ( ) throws Exception {

    final Ledger ledger = LedgerGenerator.createRandomLedger();

    ledger.setType(AccountType.ASSET.name());
    ledger.setIdentifier("1111");
    ledger.setName("Receivables");
    ledger.setDescription("Receivables Account");

    Ledger rentsReceivable = LedgerGenerator.createLedger("10011", AccountType.ASSET);
    Ledger tradeReceivables = LedgerGenerator.createLedger("10012", AccountType.ASSET);
    List <Ledger> subLedgers = new ArrayList <>();
    subLedgers.add(rentsReceivable);
    subLedgers.add(tradeReceivables);

    BigDecimal value = new BigDecimal(1000000);
    ledger.setTotalValue(value);
    ledger.setCreatedOn(LocalDate.ofYearDay(2017, 17).toString());
    ledger.setCreatedBy("Epie E.");
    ledger.setLastModifiedOn(LocalDate.ofYearDay(2018, 160).toString());
    ledger.setLastModifiedBy("Epie Ngome");
    ledger.setShowAccountsInChart(Boolean.TRUE);

    this.testSubject.createLedger(ledger);
    this.eventRecorder.wait(EventConstants.POST_LEDGER, ledger.getIdentifier());

    final Account account = AccountGenerator.createRandomAccount(ledger.getIdentifier());

    Set <String> holdersList = new HashSet <>();
    String holderOne = "First Holder";
    String holderTwo = "Second Holder";
    holdersList.add(holderOne);
    holdersList.add(holderTwo);

    Set <String> signatories = new HashSet <>();
    String signatureOne = "First To Sign";
    String signatureTwo = "Second To Sign";
    signatories.add(signatureOne);
    signatories.add(signatureTwo);

    Double bal = 105.0;

    account.setType(AccountType.ASSET.name());
    account.setIdentifier("10013");
    account.setName("Interest Receivables");
    account.setHolders(holdersList);
    account.setSignatureAuthorities(signatories);
    account.setBalance(bal);
    account.setLedger(ledger.getIdentifier());

    Gson gson = new Gson();
    this.mockMvc.perform(post("/accounts")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .content(gson.toJson(account)))
            .andExpect(status().isAccepted())
            .andDo(document("document-create-account", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()),
                    requestFields(
                            fieldWithPath("type").description("Type of Account " +
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
                            fieldWithPath("holders").type("Set<String>").description("Account Holders"),
                            fieldWithPath("signatureAuthorities").type("Set<String>").description("Account signatories"),
                            fieldWithPath("balance").type("Double").description("Account balance"),
                            fieldWithPath("ledger").description("Associated ledger")
                    )));
  }

  @Test
  public void documentFindAccount ( ) throws Exception {

    final Ledger ledger = LedgerGenerator.createRandomLedger();
    ledger.setIdentifier("1001");

    this.testSubject.createLedger(ledger);
    this.eventRecorder.wait(EventConstants.POST_LEDGER, ledger.getIdentifier());

    final Account referenceAccount = AccountGenerator.createRandomAccount(ledger.getIdentifier());
    referenceAccount.setIdentifier("1000");
    this.testSubject.createAccount(referenceAccount);
    this.eventRecorder.wait(EventConstants.POST_ACCOUNT, referenceAccount.getIdentifier());

    final Account account = AccountGenerator.createRandomAccount(ledger.getIdentifier());

    Set <String> holdersList = new HashSet <>();
    String holderOne = "Holder One";
    String holderTwo = "Holder Two";
    holdersList.add(holderOne);
    holdersList.add(holderTwo);

    Set <String> signatories = new HashSet <>();
    String signatureOne = "Signatory One";
    String signatureTwo = "Signatory Two";
    signatories.add(signatureOne);
    signatories.add(signatureTwo);

    Double bal = 906.4;

    account.setType(AccountType.ASSET.name());
    account.setIdentifier("1001");
    account.setName("Receivables");
    account.setHolders(holdersList);
    account.setSignatureAuthorities(signatories);
    account.setBalance(bal);
    account.setLedger(ledger.getIdentifier());

    account.setReferenceAccount(referenceAccount.getIdentifier());
    this.testSubject.createAccount(account);
    this.eventRecorder.wait(EventConstants.POST_ACCOUNT, account.getIdentifier());

    Gson gson = new Gson();
    this.mockMvc.perform(get("/accounts/" + account.getIdentifier())
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .accept(MediaType.ALL_VALUE)
            .content(gson.toJson(account)))
            .andExpect(status().isOk())
            .andDo(document("document-find-account", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()),
                    responseFields(
                            fieldWithPath("type").description("Type of Account " +
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
                            fieldWithPath("holders").type("Set<String>").description("Account Holders"),
                            fieldWithPath("signatureAuthorities").type("Set<String>").description("Account signatories"),
                            fieldWithPath("balance").type("Double").description("Account balance"),
                            fieldWithPath("ledger").description("Associated ledger"),
                            fieldWithPath("referenceAccount").description("Reference Account"),
                            fieldWithPath("state").description("State of account " +
                                    " + \n" +
                                    " + \n" +
                                    "*enum* _State_ {\n" +
                                    "    OPEN, + \n" +
                                    "    LOCKED, + \n" +
                                    "    CLOSED + \n" +
                                    "  }"),
                            fieldWithPath("alternativeAccountNumber").type("String").description("Alternative account"),
                            fieldWithPath("createdOn").description("Date account was created"),
                            fieldWithPath("createdBy").description("Account creator"),
                            fieldWithPath("lastModifiedOn").type("String").description("Date when account was last modified"),
                            fieldWithPath("lastModifiedBy").type("String").description("Employee who last modified account")
                    )));
  }

  @Test
  public void documentFetchAccounts ( ) throws Exception {

    Set <String> holdersListOne = new HashSet <>();
    String holderOne = "Holder One";
    holdersListOne.add(holderOne);

    Set <String> holdersListTwo = new HashSet <>();
    String holderTwo = "Holder Two";
    holdersListTwo.add(holderTwo);

    Set <String> signatoriesOne = new HashSet <>();
    String signatureOne = "Signatory One";
    signatoriesOne.add(signatureOne);

    Set <String> signatoriesTwo = new HashSet <>();
    String signatureTwo = "Signatory Two";
    signatoriesTwo.add(signatureTwo);

    final Account salesAccountOne = AccountGenerator.createAccount(
            "Organic Sales", "1111", AccountType.EXPENSE);
    salesAccountOne.setName("Organic Maize");
    salesAccountOne.setHolders(holdersListOne);
    salesAccountOne.setSignatureAuthorities(signatoriesOne);
    salesAccountOne.setBalance(225.0);

    final Account salesAccountTwo = AccountGenerator.createAccount(
            "Inorganic Sales", "1112", AccountType.EXPENSE);
    salesAccountTwo.setName("Organic Beans");
    salesAccountTwo.setHolders(holdersListTwo);
    salesAccountTwo.setSignatureAuthorities(signatoriesTwo);
    salesAccountTwo.setBalance(895.0);

    List <Account> accountList = new ArrayList <>();
    Stream.of(salesAccountOne, salesAccountTwo).forEach(account -> {
      accountList.add(account);
    });

    final Ledger ledger = LedgerGenerator.createRandomLedger();
    ledger.setIdentifier("11021");
    this.testSubject.createLedger(ledger);
    this.eventRecorder.wait(EventConstants.POST_LEDGER, ledger.getIdentifier());

    final AccountPage accountPage =
            this.testSubject.fetchAccounts(true, null, null, true,
                    0, 3, null, null);
    accountPage.setAccounts(accountList);
    accountPage.setTotalElements(new Long(accountList.size()));
    accountPage.setTotalPages(1);

    Gson gson = new Gson();
    this.mockMvc.perform(get("/accounts")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .accept(MediaType.ALL_VALUE)
            .content(gson.toJson(accountPage)))
            .andExpect(status().isOk())
            .andDo(document("document-fetch-accounts", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()),
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
                            fieldWithPath("accounts[1].type").description("AccountType").description("Type of second Account " +
                                    " + \n" +
                                    " + \n" +
                                    " *enum* _AccountType_ { + \n" +
                                    "  ASSET, + \n" +
                                    "  LIABILITY, + \n" +
                                    "  EQUITY, + \n" +
                                    "  REVENUE, + \n" +
                                    "  EXPENSE + \n" +
                                    "}"),
                            fieldWithPath("accounts[1].identifier").type("String").description("second account identifier"),
                            fieldWithPath("accounts[1].name").type("String").description("second account's name"),
                            fieldWithPath("accounts[1].holders").type("Set<String>").description("Set of account holders"),
                            fieldWithPath("accounts[1].signatureAuthorities").type("Set<String>").description("Set of signatories to account"),
                            fieldWithPath("accounts[1].balance").type("Double").description("second account balance"),
                            fieldWithPath("accounts[1].ledger").type("String").description("Associated ledger"),
                            fieldWithPath("totalPages").type("Integer").description("Total number of pages"),
                            fieldWithPath("totalElements").type("String").description("Total number of elements")
                    ),
                    responseFields(
                            fieldWithPath("accounts").type("List<Account>").description("List of Accounts"),
                            fieldWithPath("totalPages").type("Integer").description("Total number of pages"),
                            fieldWithPath("totalElements").type("String").description("Total number of elements")
                    )));
  }

  @Test
  public void documentFetchAccountsForTerm ( ) throws Exception {

    Set <String> holdersListOne = new HashSet <>();
    String holderOne = "Holder One";
    holdersListOne.add(holderOne);

    Set <String> holdersListTwo = new HashSet <>();
    String holderTwo = "Holder Two";
    holdersListTwo.add(holderTwo);

    Set <String> signatoriesOne = new HashSet <>();
    String signatureOne = "Signatory One";
    signatoriesOne.add(signatureOne);

    Set <String> signatoriesTwo = new HashSet <>();
    String signatureTwo = "Signatory Two";
    signatoriesTwo.add(signatureTwo);

    final Ledger ledger = LedgerGenerator.createRandomLedger();
    ledger.setIdentifier("2100");
    this.testSubject.createLedger(ledger);
    this.eventRecorder.wait(EventConstants.POST_LEDGER, ledger.getIdentifier());

    final Account salesAccountOne = AccountGenerator.createAccount(
            "Organic Sales", "2100.1", AccountType.REVENUE);
    salesAccountOne.setName("Organic Maize");
    salesAccountOne.setHolders(holdersListOne);
    salesAccountOne.setSignatureAuthorities(signatoriesOne);
    salesAccountOne.setBalance(225.0);

    final Account salesAccountTwo = AccountGenerator.createAccount(
            "Inorganic Sales", "100", AccountType.REVENUE);
    salesAccountTwo.setName("Organic Beans");
    salesAccountTwo.setHolders(holdersListTwo);
    salesAccountTwo.setSignatureAuthorities(signatoriesTwo);
    salesAccountTwo.setBalance(895.0);

    List <Account> accountList = new ArrayList <>();
    Stream.of(salesAccountOne, salesAccountTwo).forEach(account -> {
      accountList.add(account);
    });

    List <Account> accountListForTerm = new ArrayList <>();
    Stream.of(salesAccountOne, salesAccountTwo).
            forEach(acc -> {
              if (acc.getIdentifier().contains(ledger.getIdentifier()))
                accountListForTerm.add(acc);
            });
    Assert.assertTrue(accountListForTerm.size() == 1);

    final AccountPage accountPage =
            this.testSubject.fetchAccounts(true, ledger.getIdentifier(), null, true,
                    0, 3, null, null);
    accountPage.setAccounts(accountListForTerm);
    accountPage.setTotalElements(new Long(accountListForTerm.size()));
    accountPage.setTotalPages(1);

    Gson gson = new Gson();
    this.mockMvc.perform(get("/accounts")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .accept(MediaType.ALL_VALUE)
            .content(gson.toJson(accountPage)))
            .andExpect(status().isOk())
            .andDo(document("document-fetch-accounts-for-term", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()),
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

  @Test
  public void documentFetchActiveAccounts ( ) throws Exception {

    Set <String> holdersListOne = new HashSet <>();
    String holderOne = "Holder One";
    holdersListOne.add(holderOne);

    Set <String> holdersListTwo = new HashSet <>();
    String holderTwo = "Holder Two";
    holdersListTwo.add(holderTwo);

    Set <String> holdersListThree = new HashSet <>();
    String holderThree = "Holder Three";
    holdersListThree.add(holderThree);

    Set <String> holdersListFour = new HashSet <>();
    String holderFour = "Holder Four";
    holdersListFour.add(holderFour);

    Set <String> signatoriesOne = new HashSet <>();
    String signatureOne = "Signatory One";
    signatoriesOne.add(signatureOne);

    Set <String> signatoriesTwo = new HashSet <>();
    String signatureTwo = "Signatory Two";
    signatoriesTwo.add(signatureTwo);

    Set <String> signatoriesThree = new HashSet <>();
    String signatureThree = "Signatory Three";
    signatoriesThree.add(signatureThree);

    Set <String> signatoriesFour = new HashSet <>();
    String signatureFour = "Signatory Four";
    signatoriesFour.add(signatureFour);

    final Ledger ledger = LedgerGenerator.createRandomLedger();
    ledger.setIdentifier("3100");
    this.testSubject.createLedger(ledger);
    this.eventRecorder.wait(EventConstants.POST_LEDGER, ledger.getIdentifier());

    final Account salesAccountOne = AccountGenerator.createAccount(
            "Organic Sales", "3100.10", AccountType.REVENUE);
    salesAccountOne.setState(Account.State.OPEN.name());
    salesAccountOne.setName("Organic Maize");
    salesAccountOne.setHolders(holdersListOne);
    salesAccountOne.setSignatureAuthorities(signatoriesOne);
    salesAccountOne.setBalance(225.0);
    salesAccountOne.setState(Account.State.CLOSED.name());

    final Account salesAccountTwo = AccountGenerator.createAccount(
            "Inorganic Sales", "3100.20", AccountType.REVENUE);
    salesAccountTwo.setState(Account.State.OPEN.name());
    salesAccountTwo.setName("Organic Pens");
    salesAccountTwo.setHolders(holdersListTwo);
    salesAccountTwo.setSignatureAuthorities(signatoriesTwo);
    salesAccountTwo.setBalance(895.0);

    final Account salesAccountThree = AccountGenerator.createAccount(
            "Organic Sales", "3100.30", AccountType.REVENUE);
    salesAccountThree.setState(Account.State.OPEN.name());
    salesAccountThree.setName("Organic Peas");
    salesAccountThree.setHolders(holdersListThree);
    salesAccountThree.setSignatureAuthorities(signatoriesThree);
    salesAccountThree.setBalance(953.0);
    salesAccountOne.setState(Account.State.CLOSED.name());

    final Account salesAccountFour = AccountGenerator.createAccount(
            "Inorganic Sales", "3100.40", AccountType.REVENUE);
    salesAccountFour.setState(Account.State.OPEN.name());
    salesAccountFour.setName("Organic Pencils");
    salesAccountFour.setHolders(holdersListFour);
    salesAccountFour.setSignatureAuthorities(signatoriesFour);
    salesAccountFour.setBalance(345.0);

    List <Account> accountList = new ArrayList <>();
    Stream.of(salesAccountOne, salesAccountTwo, salesAccountThree, salesAccountFour).forEach(account -> {
      accountList.add(account);
    });

    List <Account> listOfActiveAccounts = new ArrayList <>();
    Stream.of(salesAccountOne, salesAccountTwo, salesAccountThree, salesAccountFour).
            forEach(acc -> {
              if (acc.getState() == Account.State.OPEN.name())
                listOfActiveAccounts.add(acc);
            });

    final AccountPage accountPage =
            this.testSubject.fetchAccounts(true, ledger.getIdentifier(), null, true,
                    0, 3, null, null);
    accountPage.setAccounts(listOfActiveAccounts);
    accountPage.setTotalElements(new Long(listOfActiveAccounts.size()));
    accountPage.setTotalPages(1);

    Gson gson = new Gson();
    this.mockMvc.perform(get("/accounts")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .accept(MediaType.ALL_VALUE)
            .content(gson.toJson(accountPage)))
            .andExpect(status().isOk())
            .andDo(document("document-fetch-active-accounts", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()),
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

  @Test
  public void documentFindAccountWithAlternativeAccountNumber ( ) throws Exception {

    Set <String> holdersList = new HashSet <>();
    String holderOne = "Only Holder";
    holdersList.add(holderOne);

    Set <String> signatories = new HashSet <>();
    String signatureOne = "Only Signatory";
    signatories.add(signatureOne);

    final Ledger ledger = LedgerGenerator.createRandomLedger();
    ledger.setIdentifier("7100");
    ledger.setType(AccountType.REVENUE.name());
    this.testSubject.createLedger(ledger);
    this.eventRecorder.wait(EventConstants.POST_LEDGER, ledger.getIdentifier());

    final String altNumber = "7-1-0-0-.-1-0";

    final Account salesAccount = AccountGenerator.createAccount(
            "7100", "7100.10", AccountType.REVENUE);
    salesAccount.setState(Account.State.OPEN.name());
    salesAccount.setName("Organic Maize");
    salesAccount.setHolders(holdersList);
    salesAccount.setSignatureAuthorities(signatories);
    salesAccount.setBalance(3435.0);
    salesAccount.setState(Account.State.CLOSED.name());
    salesAccount.setAlternativeAccountNumber(altNumber);

    this.testSubject.createAccount(salesAccount);
    this.eventRecorder.wait(EventConstants.POST_ACCOUNT, salesAccount.getIdentifier());

    final AccountPage accountPage =
            this.testSubject.fetchAccounts(true, altNumber, null, true,
                    0, 3, null, null);

    Gson gson = new Gson();
    this.mockMvc.perform(get("/accounts/" + salesAccount.getIdentifier())
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .accept(MediaType.ALL_VALUE)
            /*.content(gson.toJson())*/)
            .andExpect(status().isOk())
            .andDo(document("document-find-account-with-alternative-account-number", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()),
                    responseFields(
                            fieldWithPath("type").description("AccountType").description("Type of Account " +
                                    " + \n" +
                                    " + \n" +
                                    " *enum* _AccountType_ { + \n" +
                                    "  ASSET, + \n" +
                                    "  LIABILITY, + \n" +
                                    "  EQUITY, + \n" +
                                    "  REVENUE, + \n" +
                                    "  EXPENSE + \n" +
                                    "}"),
                            fieldWithPath("identifier").type("String").description("Account identifier"),
                            fieldWithPath("name").type("String").description("Account name"),
                            fieldWithPath("holders").type("Set<String>").description("Set of account holders"),
                            fieldWithPath("signatureAuthorities").type("Set<String>").description("Set of signatories to account"),
                            fieldWithPath("balance").type("Double").description("account's balance"),
                            fieldWithPath("referenceAccount").type("String").description("Reference Account"),
                            fieldWithPath("ledger").type("String").description("Associated ledger"),
                            fieldWithPath("state").type("State").description("State of Account " +
                                    " + \n" +
                                    " + \n" +
                                    " *enum* _State_ { + \n" +
                                    "  OPEN, + \n" +
                                    "  LOCKED, + \n" +
                                    "  CLOSED, + \n" +
                                    "}"),
                            fieldWithPath("alternativeAccountNumber").type("Integer").description("Total accounts in page"),
                            fieldWithPath("createdOn").type("List<Account>").description("List of Accounts"),
                            fieldWithPath("createdBy").type("Integer").description("Total number of pages"),
                            fieldWithPath("lastModifiedOn").type("String").description("Total number of elements"),
                            fieldWithPath("lastModifiedBy").type("String").description("Total number of elements")
                    )));
  }

  @Test
  public void documentModifyAccount ( ) throws Exception {

    final Ledger ledger = LedgerGenerator.createRandomLedger();
    ledger.setIdentifier("2000");
    this.testSubject.createLedger(ledger);
    this.eventRecorder.wait(EventConstants.POST_LEDGER, ledger.getIdentifier());

    final Account account = AccountGenerator.createRandomAccount(ledger.getIdentifier());

    Set <String> holdersList = new HashSet <>();
    String holderOne = "Holder First";
    String holderTwo = "Holder Second";
    holdersList.add(holderOne);
    holdersList.add(holderTwo);

    Set <String> signatories = new HashSet <>();
    String signatureOne = "First Signatory";
    String signatureTwo = "Second Signatory";
    signatories.add(signatureOne);
    signatories.add(signatureTwo);

    Double bal = 342.0;

    account.setType(AccountType.ASSET.name());
    account.setIdentifier("2001");
    account.setName("Payables");
    account.setHolders(holdersList);
    account.setSignatureAuthorities(signatories);
    account.setBalance(bal);
    account.setLedger(ledger.getIdentifier());

    this.testSubject.createAccount(account);
    this.eventRecorder.wait(EventConstants.POST_ACCOUNT, account.getIdentifier());

    Gson gson = new Gson();
    this.mockMvc.perform(put("/accounts/" + account.getIdentifier())
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .content(gson.toJson(account)))
            .andExpect(status().isAccepted())
            .andDo(document("document-modify-account", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()),
                    requestFields(
                            fieldWithPath("type").description("Type of Account " +
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
                            fieldWithPath("holders").type("Set<String>").description("Account Holders"),
                            fieldWithPath("signatureAuthorities").type("Set<String>").description("Account signatories"),
                            fieldWithPath("balance").type("Double").description("Account balance"),
                            fieldWithPath("ledger").description("Associated ledger")
                    )));
  }

  @Test
  public void documentCloseAccount ( ) throws Exception {

    final Ledger randomLedger = LedgerGenerator.createRandomLedger();
    randomLedger.setIdentifier("3000");
    this.testSubject.createLedger(randomLedger);
    this.eventRecorder.wait(EventConstants.POST_LEDGER, randomLedger.getIdentifier());

    final Account randomAccount = AccountGenerator.createRandomAccount(randomLedger.getIdentifier());
    randomAccount.setIdentifier("3001");
    randomAccount.setState(Account.State.OPEN.name());
    this.testSubject.createAccount(randomAccount);
    this.eventRecorder.wait(EventConstants.POST_ACCOUNT, randomAccount.getIdentifier());

    final AccountCommand closeCommand = new AccountCommand();
    closeCommand.setAction(AccountCommand.Action.CLOSE.name());
    closeCommand.setComment("Close Account");
    this.testSubject.accountCommand(randomAccount.getIdentifier(), closeCommand);

    Gson gson = new Gson();
    this.mockMvc.perform(post("/accounts/" + randomAccount.getIdentifier() + "/commands")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .content(gson.toJson(closeCommand)))
            .andExpect(status().isAccepted())
            .andDo(document("document-close-account", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()),
                    requestFields(
                            fieldWithPath("action").description("Action CLOSE " +
                                    " +\n" +
                                    " *enum* _Action_ { +\n" +
                                    "    LOCK, +\n" +
                                    "    UNLOCK, +\n" +
                                    "    CLOSE, +\n" +
                                    "    REOPEN +\n" +
                                    "  }"),
                            fieldWithPath("comment").description("Close comment"))));
  }

  @Test
  public void documentLockAccount ( ) throws Exception {

    final Ledger ledger = LedgerGenerator.createRandomLedger();
    ledger.setIdentifier("4000");
    this.testSubject.createLedger(ledger);
    this.eventRecorder.wait(EventConstants.POST_LEDGER, ledger.getIdentifier());

    final Account rAccount = AccountGenerator.createRandomAccount(ledger.getIdentifier());
    rAccount.setIdentifier("4001");
    rAccount.setState(Account.State.OPEN.name());
    this.testSubject.createAccount(rAccount);
    this.eventRecorder.wait(EventConstants.POST_ACCOUNT, rAccount.getIdentifier());

    final AccountCommand lockCommand = new AccountCommand();
    lockCommand.setAction(AccountCommand.Action.LOCK.name());
    lockCommand.setComment("Lock Account");
    this.testSubject.accountCommand(rAccount.getIdentifier(), lockCommand);

    Gson gson = new Gson();
    this.mockMvc.perform(post("/accounts/" + rAccount.getIdentifier() + "/commands")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .content(gson.toJson(lockCommand)))
            .andExpect(status().isAccepted())
            .andDo(document("document-lock-account", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()),
                    requestFields(
                            fieldWithPath("action").description("Action LOCK " +
                                    " +\n" +
                                    " *enum* _Action_ { +\n" +
                                    "    LOCK, +\n" +
                                    "    UNLOCK, +\n" +
                                    "    CLOSE, +\n" +
                                    "    REOPEN +\n" +
                                    "  }"),
                            fieldWithPath("comment").description("Lock comment"))));
  }

  @Test
  public void documentUnlockAccount ( ) throws Exception {

    final Ledger ledger = LedgerGenerator.createRandomLedger();
    ledger.setIdentifier("5100");

    this.testSubject.createLedger(ledger);
    this.eventRecorder.wait(EventConstants.POST_LEDGER, ledger.getIdentifier());

    final Account ulAccount = AccountGenerator.createRandomAccount(ledger.getIdentifier());
    ulAccount.setIdentifier("5001");
    ulAccount.setState(Account.State.LOCKED.name());

    this.testSubject.createAccount(ulAccount);
    this.eventRecorder.wait(EventConstants.POST_ACCOUNT, ulAccount.getIdentifier());

    final AccountCommand unlockCommand = new AccountCommand();
    unlockCommand.setAction(AccountCommand.Action.UNLOCK.name());
    unlockCommand.setComment("Unlock Account");
    this.testSubject.accountCommand(ulAccount.getIdentifier(), unlockCommand);

    Gson gson = new Gson();
    this.mockMvc.perform(post("/accounts/" + ulAccount.getIdentifier() + "/commands")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .content(gson.toJson(unlockCommand)))
            .andExpect(status().isAccepted())
            .andDo(document("document-unlock-account", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()),
                    requestFields(
                            fieldWithPath("action").description("Action UNLOCK " +
                                    " +\n" +
                                    " *enum* _Action_ { +\n" +
                                    "    LOCK, +\n" +
                                    "    UNLOCK, +\n" +
                                    "    CLOSE, +\n" +
                                    "    REOPEN +\n" +
                                    "  }"),
                            fieldWithPath("comment").description("Unlock comment"))));
  }

  @Test
  public void documentReopenAccount ( ) throws Exception {

    final Ledger ledger = LedgerGenerator.createRandomLedger();
    ledger.setIdentifier("6000");

    this.testSubject.createLedger(ledger);
    this.eventRecorder.wait(EventConstants.POST_LEDGER, ledger.getIdentifier());

    final Account roAccount = AccountGenerator.createRandomAccount(ledger.getIdentifier());
    roAccount.setIdentifier("6001");
    roAccount.setState(Account.State.CLOSED.name());

    this.testSubject.createAccount(roAccount);
    this.eventRecorder.wait(EventConstants.POST_ACCOUNT, roAccount.getIdentifier());

    final AccountCommand reopenCommand = new AccountCommand();
    reopenCommand.setAction(AccountCommand.Action.REOPEN.name());
    reopenCommand.setComment("Reopen Account");
    this.testSubject.accountCommand(roAccount.getIdentifier(), reopenCommand);

    Gson gson = new Gson();
    this.mockMvc.perform(post("/accounts/" + roAccount.getIdentifier() + "/commands")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .content(gson.toJson(reopenCommand)))
            .andExpect(status().isAccepted())
            .andDo(document("document-reopen-account", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()),
                    requestFields(
                            fieldWithPath("action").description("Action REOPEN " +
                                    " +\n" +
                                    " *enum* _Action_ { +\n" +
                                    "    LOCK, +\n" +
                                    "    UNLOCK, +\n" +
                                    "    CLOSE, +\n" +
                                    "    REOPEN +\n" +
                                    "  }"),
                            fieldWithPath("comment").description("Reopen comment"))));
  }

  @Test
  public void documentDeleteAccount ( ) throws Exception {

    final Ledger randomLedger = LedgerGenerator.createRandomLedger();
    randomLedger.setIdentifier("5000");
    this.testSubject.createLedger(randomLedger);
    this.eventRecorder.wait(EventConstants.POST_LEDGER, randomLedger.getIdentifier());

    final Account randomAccount = AccountGenerator.createRandomAccount(randomLedger.getIdentifier());
    randomAccount.setIdentifier("5002");
    this.testSubject.createAccount(randomAccount);
    this.eventRecorder.wait(EventConstants.POST_ACCOUNT, randomAccount.getIdentifier());

    final AccountCommand closeCommand = new AccountCommand();
    closeCommand.setAction(AccountCommand.Action.CLOSE.name());
    closeCommand.setComment("Close Account!");
    this.testSubject.accountCommand(randomAccount.getIdentifier(), closeCommand);
    this.eventRecorder.wait(EventConstants.CLOSE_ACCOUNT, randomAccount.getIdentifier());

    this.mockMvc.perform(delete("/accounts/" + randomAccount.getIdentifier())
            .accept(MediaType.ALL_VALUE)
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isAccepted())
            .andDo(document("document-delete-account", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint())));
  }

  @Test
  public void documentFetchActions ( ) throws Exception {

    final Ledger randomLedger = LedgerGenerator.createRandomLedger();
    randomLedger.setIdentifier("5200");
    this.testSubject.createLedger(randomLedger);
    this.eventRecorder.wait(EventConstants.POST_LEDGER, randomLedger.getIdentifier());

    final Account randomAccount = AccountGenerator.createRandomAccount(randomLedger.getIdentifier());
    randomAccount.setIdentifier("5002");
    randomAccount.setState(Account.State.OPEN.name());
    this.testSubject.createAccount(randomAccount);
    this.eventRecorder.wait(EventConstants.POST_ACCOUNT, randomAccount.getIdentifier());

    final List <AccountCommand> accountCommands = super.testSubject.fetchActions(randomAccount.getIdentifier());
    accountCommands.get(0).setComment("Lock Account");
    accountCommands.get(0).setCreatedBy("setna");
    accountCommands.get(0).setCreatedOn(LocalDate.now().toString());

    accountCommands.get(1).setComment("Close Account");
    accountCommands.get(1).setCreatedBy("setna");
    accountCommands.get(1).setCreatedOn(LocalDate.now().toString());
    Assert.assertEquals(2, accountCommands.size());

    this.mockMvc.perform(get("/accounts/" + randomAccount.getIdentifier() + "/actions")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .accept(MediaType.ALL_VALUE))
            .andExpect(status().isOk())
            .andDo(document("document-fetch-actions", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()),
                    responseFields(
                            fieldWithPath("[].action").description("Action LOCK " +
                                    " +\n" +
                                    " *enum* _Action_ { +\n" +
                                    "    LOCK, +\n" +
                                    "    UNLOCK, +\n" +
                                    "    CLOSE, +\n" +
                                    "    REOPEN +\n" +
                                    "  }"),
                            fieldWithPath("[].comment").description("Reopen comment"),
                            fieldWithPath("[].createdOn").description("Date when action was carried out"),
                            fieldWithPath("[].createdBy").description("Employee who acted on account"),
                            fieldWithPath("[1].action").description("Action CLOSE " +
                                    " +\n" +
                                    " *enum* _Action_ { +\n" +
                                    "    LOCK, +\n" +
                                    "    UNLOCK, +\n" +
                                    "    CLOSE, +\n" +
                                    "    REOPEN +\n" +
                                    "  }"),
                            fieldWithPath("[1].comment").description("Reopen comment"),
                            fieldWithPath("[1].createdOn").description("Date when action was carried out"),
                            fieldWithPath("[1].createdBy").description("Employee who acted on account"))));
  }

  @Test
  public void documentFetchAccountEntries ( ) throws Exception {

    final Ledger ledger = LedgerGenerator.createRandomLedger();
    ledger.setIdentifier("1500");
    this.testSubject.createLedger(ledger);
    this.eventRecorder.wait(EventConstants.POST_LEDGER, ledger.getIdentifier());

    final Account accountToDebit = AccountGenerator.createRandomAccount(ledger.getIdentifier());
    accountToDebit.setIdentifier("1501");
    this.testSubject.createAccount(accountToDebit);
    this.eventRecorder.wait(EventConstants.POST_ACCOUNT, accountToDebit.getIdentifier());

    final Account accountToCredit = AccountGenerator.createRandomAccount(ledger.getIdentifier());
    accountToCredit.setIdentifier("1601");
    this.testSubject.createAccount(accountToCredit);
    this.eventRecorder.wait(EventConstants.POST_ACCOUNT, accountToCredit.getIdentifier());

    final int journalEntryCount = 3;
    final List <JournalEntry> journalEntries = Stream.generate(( ) -> JournalEntryGenerator.createRandomJournalEntry(accountToDebit, "5.0", accountToCredit, "5.0"))
            .limit(journalEntryCount)
            .collect(Collectors.toList());

    journalEntries.stream()
            .forEach(entry -> {
              entry.setMessage("Message " + journalEntries.indexOf(entry));
            });

    journalEntries.stream()
            .map(jEntry -> {
              this.testSubject.createJournalEntry(jEntry);
              return jEntry.getTransactionIdentifier();
            })
            .forEach(transactionId -> {
              try {
                this.eventRecorder.wait(EventConstants.POST_JOURNAL_ENTRY, transactionId);
                this.eventRecorder.wait(EventConstants.RELEASE_JOURNAL_ENTRY, transactionId);
              } catch (final InterruptedException e) {
                throw new RuntimeException(e);
              }
            });

    Thread.sleep(20L); // Short pause to make sure it really is last.

    final LocalDate today = LocalDate.now(Clock.systemUTC());
    final String todayDateRange = new DateRange(today, today).toString();

    final AccountEntryPage accountEntriesPage =
            this.testSubject.fetchAccountEntries(accountToCredit.getIdentifier(), todayDateRange, "Entries Fetched", 0,
                    1, "ASC", Sort.Direction.ASC.name());

    Gson gson = new Gson();
    this.mockMvc.perform(get("/accounts/" + accountToCredit.getIdentifier() + "/entries")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            /*.content(gson.toJson(accountEntriesPage))*/
            .accept(MediaType.ALL_VALUE))
            .andExpect(status().isOk())
            .andDo(document("document-fetch-account-entries", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()),
                    responseFields(
                            fieldWithPath("accountEntries").type("List<AccountEntry>").description("List of account entries"),
                            fieldWithPath("accountEntries[].type").description("Type of entry DEBIT or CREDIT "),
                            fieldWithPath("accountEntries[].transactionDate").description("Date of transaction"),
                            fieldWithPath("accountEntries[].message").description("Transaction message"),
                            fieldWithPath("accountEntries[].amount").description("Transaction amount"),
                            fieldWithPath("accountEntries[].balance").description("Transaction balance"),
                            fieldWithPath("accountEntries[1].type").description("Type of entry DEBIT or CREDIT "),
                            fieldWithPath("accountEntries[1].transactionDate").description("Date of transaction"),
                            fieldWithPath("accountEntries[1].message").description("Transaction message"),
                            fieldWithPath("accountEntries[1].amount").description("Transaction amount"),
                            fieldWithPath("accountEntries[1].balance").description("Transaction balance"),
                            fieldWithPath("accountEntries[2].type").description("Type of entry DEBIT or CREDIT "),
                            fieldWithPath("accountEntries[2].transactionDate").description("Date of transaction"),
                            fieldWithPath("accountEntries[2].message").description("Transaction message"),
                            fieldWithPath("accountEntries[2].amount").description("Transaction amount"),
                            fieldWithPath("accountEntries[2].balance").description("Transaction balance"),
                            fieldWithPath("totalPages").type("List<AccountEntry>").description("Reopen comment"),
                            fieldWithPath("totalElements").type("List<AccountEntry>").description("Reopen comment"))));
  }
}