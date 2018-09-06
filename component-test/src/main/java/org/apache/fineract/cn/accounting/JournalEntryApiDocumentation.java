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
import org.apache.fineract.cn.lang.DateConverter;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.restdocs.JUnitRestDocumentation;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class JournalEntryApiDocumentation extends AbstractAccountingTest {

  @Rule
  public final JUnitRestDocumentation restDocumentation = new JUnitRestDocumentation("build/doc/generated-snippets/test-journal-entry");

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
  public void documentCreateJournalEntry ( ) throws Exception {

    final Ledger assetLedger = LedgerGenerator.createRandomLedger();
    assetLedger.setType(AccountType.ASSET.name());
    this.testSubject.createLedger(assetLedger);
    this.eventRecorder.wait(EventConstants.POST_LEDGER, assetLedger.getIdentifier());

    final Account debtorAccount = AccountGenerator.createRandomAccount(assetLedger.getIdentifier());
    debtorAccount.setIdentifier("7100");
    debtorAccount.setType(AccountType.ASSET.name());
    debtorAccount.setBalance(100.00D);
    this.testSubject.createAccount(debtorAccount);
    this.eventRecorder.wait(EventConstants.POST_ACCOUNT, debtorAccount.getIdentifier());

    final Ledger liabilityLedger = LedgerGenerator.createRandomLedger();
    liabilityLedger.setIdentifier("8120");
    liabilityLedger.setType(AccountType.LIABILITY.name());
    this.testSubject.createLedger(liabilityLedger);
    this.eventRecorder.wait(EventConstants.POST_LEDGER, liabilityLedger.getIdentifier());

    final Account creditorAccount = AccountGenerator.createRandomAccount(liabilityLedger.getIdentifier());
    creditorAccount.setType(AccountType.LIABILITY.name());
    creditorAccount.setBalance(100.00D);
    this.testSubject.createAccount(creditorAccount);
    this.eventRecorder.wait(EventConstants.POST_ACCOUNT, creditorAccount.getIdentifier());

    final JournalEntry journalEntry = JournalEntryGenerator.createRandomJournalEntry(debtorAccount, "50.00",
            creditorAccount, "50.00");
    journalEntry.setTransactionIdentifier("F14062018");
    journalEntry.setTransactionDate(LocalDate.now().toString());
    journalEntry.setTransactionType("ADBT");
    journalEntry.setClerk("Boring Clerk");
    journalEntry.setNote("Account Db");
    journalEntry.setMessage("Account Has Been Debited");

    Set <Debtor> debtorSet = new HashSet <>();
    debtorSet.add(new Debtor(debtorAccount.getIdentifier(), debtorAccount.getBalance().toString()));

    Set <Creditor> creditorSet = new HashSet <>();
    creditorSet.add(new Creditor(creditorAccount.getIdentifier(), creditorAccount.getBalance().toString()));

    journalEntry.setDebtors(debtorSet);
    journalEntry.setCreditors(creditorSet);

    Gson gson = new Gson();
    this.mockMvc.perform(post("/journal")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .content(gson.toJson(journalEntry)))
            .andExpect(status().isAccepted())
            .andDo(document("document-create-journal-entry", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()),
                    requestFields(
                            fieldWithPath("transactionIdentifier").description("Transaction ID"),
                            fieldWithPath("transactionDate").description("Account identifier"),
                            fieldWithPath("transactionType").description("Type of transaction"),
                            fieldWithPath("clerk").type("String").description("Clerk who initiated transaction"),
                            fieldWithPath("note").type("String").description("Transaction note"),
                            fieldWithPath("debtors").type("Set<Debtors>").description("Set of debtors"),
                            fieldWithPath("creditors").type("Set<Creditors>").description("Set of creditors"),
                            fieldWithPath("message").description("Associated ledger")
                    )));
  }

  @Test
  public void documentFetchJournalEntries ( ) throws Exception {

    final Ledger assetLedgerOne = LedgerGenerator.createRandomLedger();
    assetLedgerOne.setIdentifier("7120");
    assetLedgerOne.setType(AccountType.ASSET.name());
    this.testSubject.createLedger(assetLedgerOne);
    this.eventRecorder.wait(EventConstants.POST_LEDGER, assetLedgerOne.getIdentifier());

    final Account accountToDebitOne = AccountGenerator.createRandomAccount(assetLedgerOne.getIdentifier());
    accountToDebitOne.setIdentifier("7140");
    accountToDebitOne.setType(AccountType.ASSET.name());
    accountToDebitOne.setBalance(1000.0);
    this.testSubject.createAccount(accountToDebitOne);
    this.eventRecorder.wait(EventConstants.POST_ACCOUNT, accountToDebitOne.getIdentifier());

    final Ledger liabilityLedgerOne = LedgerGenerator.createRandomLedger();
    liabilityLedgerOne.setIdentifier("8150");
    liabilityLedgerOne.setType(AccountType.LIABILITY.name());
    this.testSubject.createLedger(liabilityLedgerOne);
    this.eventRecorder.wait(EventConstants.POST_LEDGER, liabilityLedgerOne.getIdentifier());

    final Account accountToCreditOne = AccountGenerator.createRandomAccount(liabilityLedgerOne.getIdentifier());
    accountToCreditOne.setIdentifier("8160");
    accountToCreditOne.setType(AccountType.LIABILITY.name());
    this.testSubject.createAccount(accountToCreditOne);
    this.eventRecorder.wait(EventConstants.POST_ACCOUNT, accountToCreditOne.getIdentifier());

    final JournalEntry journalEntryOne = JournalEntryGenerator.createRandomJournalEntry(accountToDebitOne, "50.00",
            accountToCreditOne, "50.00");
    final OffsetDateTime startOne = OffsetDateTime.of(2017, 6, 20, 1, 0, 0, 0, ZoneOffset.UTC);
    journalEntryOne.setTransactionDate(startOne.format(DateTimeFormatter.ISO_ZONED_DATE_TIME));
    journalEntryOne.setTransactionIdentifier("FE1");
    journalEntryOne.setTransactionType("ACCT");
    journalEntryOne.setClerk("Mr. " + journalEntryOne.getClerk().toUpperCase().charAt(0) + journalEntryOne.getClerk().substring(1, 5));
    journalEntryOne.setNote("Noted Transfer");
    journalEntryOne.setMessage("First Message Noted");

    this.testSubject.createJournalEntry(journalEntryOne);
    this.eventRecorder.wait(EventConstants.POST_JOURNAL_ENTRY, journalEntryOne.getTransactionIdentifier());
    this.eventRecorder.wait(EventConstants.RELEASE_JOURNAL_ENTRY, journalEntryOne.getTransactionIdentifier());

    final Ledger assetLedgerTwo = LedgerGenerator.createRandomLedger();
    assetLedgerTwo.setIdentifier("7200");
    assetLedgerTwo.setType(AccountType.ASSET.name());
    this.testSubject.createLedger(assetLedgerTwo);
    this.eventRecorder.wait(EventConstants.POST_LEDGER, assetLedgerTwo.getIdentifier());

    final Account accountToDebitTwo = AccountGenerator.createRandomAccount(assetLedgerTwo.getIdentifier());
    accountToDebitTwo.setIdentifier("7210");
    accountToDebitTwo.setType(AccountType.ASSET.name());
    accountToDebitTwo.setBalance(2000.0);
    this.testSubject.createAccount(accountToDebitTwo);
    this.eventRecorder.wait(EventConstants.POST_ACCOUNT, accountToDebitTwo.getIdentifier());

    final Ledger liabilityLedgerTwo = LedgerGenerator.createRandomLedger();
    liabilityLedgerTwo.setIdentifier("8200");
    liabilityLedgerTwo.setType(AccountType.LIABILITY.name());
    this.testSubject.createLedger(liabilityLedgerTwo);
    this.eventRecorder.wait(EventConstants.POST_LEDGER, liabilityLedgerTwo.getIdentifier());

    final Account accountToCreditTwo = AccountGenerator.createRandomAccount(liabilityLedgerTwo.getIdentifier());
    accountToCreditTwo.setIdentifier("8210");
    accountToCreditTwo.setType(AccountType.LIABILITY.name());
    this.testSubject.createAccount(accountToCreditTwo);
    this.eventRecorder.wait(EventConstants.POST_ACCOUNT, accountToCreditTwo.getIdentifier());

    final JournalEntry journalEntryTwo = JournalEntryGenerator.createRandomJournalEntry(accountToDebitTwo, "40.00",
            accountToCreditTwo, "40.00");
    final OffsetDateTime startTwo = OffsetDateTime.of(2017, 6, 24, 1, 0, 0, 0, ZoneOffset.UTC);
    journalEntryTwo.setTransactionDate(startTwo.format(DateTimeFormatter.ISO_ZONED_DATE_TIME));
    journalEntryTwo.setTransactionIdentifier("FE2");
    journalEntryTwo.setTransactionType("ACRT");
    journalEntryTwo.setClerk("Mrs. " + journalEntryTwo.getClerk().toUpperCase().charAt(0) + journalEntryTwo.getClerk().substring(1, 5));
    journalEntryTwo.setNote("Noted Credit");
    journalEntryTwo.setMessage("Message Noted");

    this.testSubject.createJournalEntry(journalEntryTwo);
    this.eventRecorder.wait(EventConstants.POST_JOURNAL_ENTRY, journalEntryTwo.getTransactionIdentifier());
    this.eventRecorder.wait(EventConstants.RELEASE_JOURNAL_ENTRY, journalEntryTwo.getTransactionIdentifier());

    final LocalDate beginDate = LocalDate.of(2017, 6, 20);
    final LocalDate endDate = LocalDate.of(2017, 6, 24);
    final String dateRange = MessageFormat.format("{0}..{1}",
            DateConverter.toIsoString(beginDate),
            DateConverter.toIsoString(endDate));

    final List <JournalEntry> journalEntriesPage =
            this.testSubject.fetchJournalEntries(dateRange, accountToDebitOne.getIdentifier(), BigDecimal.valueOf(50.00D));

    Gson gson = new Gson();
    this.mockMvc.perform(get("/journal")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .accept(MediaType.ALL_VALUE))
            .andExpect(status().isOk())
            .andDo(document("document-fetch-journal-entries", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint())));
  }

  @Test
  public void documentFindJournalEntry ( ) throws Exception {

    final Ledger assetLedger = LedgerGenerator.createRandomLedger();
    assetLedger.setIdentifier("7100");
    assetLedger.setType(AccountType.ASSET.name());
    this.testSubject.createLedger(assetLedger);
    this.eventRecorder.wait(EventConstants.POST_LEDGER, assetLedger.getIdentifier());

    final Account accountToDebit = AccountGenerator.createRandomAccount(assetLedger.getIdentifier());
    accountToDebit.setIdentifier("7110");
    accountToDebit.setType(AccountType.ASSET.name());
    accountToDebit.setBalance(1000.0);
    this.testSubject.createAccount(accountToDebit);
    this.eventRecorder.wait(EventConstants.POST_ACCOUNT, accountToDebit.getIdentifier());

    final Ledger liabilityLedger = LedgerGenerator.createRandomLedger();
    liabilityLedger.setIdentifier("8100");
    liabilityLedger.setType(AccountType.LIABILITY.name());
    this.testSubject.createLedger(liabilityLedger);
    this.eventRecorder.wait(EventConstants.POST_LEDGER, liabilityLedger.getIdentifier());

    final Account accountToCredit = AccountGenerator.createRandomAccount(liabilityLedger.getIdentifier());
    accountToCredit.setIdentifier("8110");
    accountToCredit.setType(AccountType.LIABILITY.name());
    this.testSubject.createAccount(accountToCredit);
    this.eventRecorder.wait(EventConstants.POST_ACCOUNT, accountToCredit.getIdentifier());

    final JournalEntry journalEntry = JournalEntryGenerator.createRandomJournalEntry(accountToDebit, "50.00",
            accountToCredit, "50.00");
    final OffsetDateTime start = OffsetDateTime.of(2017, 6, 24, 1, 0, 0, 0, ZoneOffset.UTC);
    journalEntry.setTransactionDate(start.format(DateTimeFormatter.ISO_ZONED_DATE_TIME));
    journalEntry.setTransactionIdentifier("FE136183");
    journalEntry.setTransactionType("ACCO");
    journalEntry.setClerk("Mr. " + journalEntry.getClerk().toUpperCase().charAt(0) + journalEntry.getClerk().substring(1, 5));
    journalEntry.setNote("Noted");
    journalEntry.setMessage("Message Noted");

    this.testSubject.createJournalEntry(journalEntry);
    this.eventRecorder.wait(EventConstants.POST_JOURNAL_ENTRY, journalEntry.getTransactionIdentifier());
    this.eventRecorder.wait(EventConstants.RELEASE_JOURNAL_ENTRY, journalEntry.getTransactionIdentifier());

    this.mockMvc.perform(get("/journal/" + journalEntry.getTransactionIdentifier())
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .accept(MediaType.ALL_VALUE))
            .andExpect(status().isOk())
            .andDo(document("document-find-journal-entry", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()),
                    responseFields(
                            fieldWithPath("transactionIdentifier").description("Transaction ID"),
                            fieldWithPath("transactionDate").description("Account identifier"),
                            fieldWithPath("transactionType").description("Type of transaction"),
                            fieldWithPath("clerk").type("String").description("Clerk who initiated transaction"),
                            fieldWithPath("note").type("String").description("Transaction note"),
                            fieldWithPath("debtors").type("Set<Debtors>").description("Set of debtors"),
                            fieldWithPath("creditors").type("Set<Creditors>").description("Set of creditors"),
                            fieldWithPath("state").type("State").description("State of journal entry " +
                                    " + \n" +
                                    " *enum* _State_ { + \n" +
                                    "    PENDING, + \n" +
                                    "    PROCESSED + \n" +
                                    "  } +"),
                            fieldWithPath("message").description("Journal Message  ")
                    )));
  }
}
