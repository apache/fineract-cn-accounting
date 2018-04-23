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

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.fineract.cn.accounting.api.v1.EventConstants;
import org.apache.fineract.cn.accounting.api.v1.client.LedgerAlreadyExistsException;
import org.apache.fineract.cn.accounting.api.v1.client.LedgerNotFoundException;
import org.apache.fineract.cn.accounting.api.v1.client.LedgerReferenceExistsException;
import org.apache.fineract.cn.accounting.api.v1.domain.Account;
import org.apache.fineract.cn.accounting.api.v1.domain.AccountType;
import org.apache.fineract.cn.accounting.api.v1.domain.Ledger;
import org.apache.fineract.cn.accounting.api.v1.domain.LedgerPage;
import org.apache.fineract.cn.accounting.util.AccountGenerator;
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

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class TestLedger extends AbstractAccountingTest {

  @Rule
  public final JUnitRestDocumentation restDocumentation = new JUnitRestDocumentation("src/doc/generated-snippets/test-ledger");

  @Autowired
  private WebApplicationContext context;

  private MockMvc mockMvc;

  final String path = "/accounting/v1";

  @Before
  public void setUp(){

    this.mockMvc = MockMvcBuilders.webAppContextSetup(this.context)
            .apply(documentationConfiguration(this.restDocumentation))
            .alwaysDo(document("{method-name}", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint())))
            .build();
  }

  @Test
  public void shouldCreateLedger() throws Exception {
    final Ledger ledger = LedgerGenerator.createRandomLedger();

    this.testSubject.createLedger(ledger);

    Assert.assertTrue(this.eventRecorder.wait(EventConstants.POST_LEDGER, ledger.getIdentifier()));

    this.mockMvc.perform(post(path + "/ledgers")
            .contentType(MediaType.APPLICATION_JSON_VALUE).accept(MediaType.APPLICATION_JSON_VALUE)
            .content(ledger.getIdentifier()))
            .andExpect(status().isNotFound());
  }

  @Test
  public void shouldNotCreateLedgerAlreadyExists() throws Exception {
    final Ledger ledger = LedgerGenerator.createRandomLedger();

    this.testSubject.createLedger(ledger);

    this.eventRecorder.wait(EventConstants.POST_LEDGER, ledger.getIdentifier());

    try {
      this.testSubject.createLedger(ledger);
      Assert.fail();
    } catch (final LedgerAlreadyExistsException ex) {
      // do nothing, expected
    }

  }

  @Test
  public void shouldFetchLedgers() throws Exception {
    final LedgerPage currentLedgerPage = this.testSubject.fetchLedgers(false, null, null, null, null, null, null);

    final Ledger ledger = LedgerGenerator.createRandomLedger();

    this.testSubject.createLedger(ledger);

    this.eventRecorder.wait(EventConstants.POST_LEDGER, ledger.getIdentifier());

    final LedgerPage ledgerPage = this.testSubject.fetchLedgers(false, null, null, null, null, null, null);
    Assert.assertEquals(currentLedgerPage.getTotalElements() + 1L, ledgerPage.getTotalElements().longValue());

    this.mockMvc.perform(get(path + "/ledgers/")
            .accept(MediaType.ALL_VALUE)
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().is4xxClientError());
  }

  @Test
  public void shouldFetchSubLedgers() throws Exception {
    final Ledger parent = LedgerGenerator.createRandomLedger();
    final Ledger child = LedgerGenerator.createRandomLedger();
    parent.setSubLedgers(Collections.singletonList(child));

    final LedgerPage currentLedgerPage = this.testSubject.fetchLedgers(true, child.getIdentifier(), null, null, null, null, null);

    this.testSubject.createLedger(parent);

    this.eventRecorder.wait(EventConstants.POST_LEDGER, parent.getIdentifier());

    final LedgerPage ledgerPage = this.testSubject.fetchLedgers(true, child.getIdentifier(), null, null, null, null, null);
    Assert.assertEquals(currentLedgerPage.getTotalElements() + 1L, ledgerPage.getTotalElements().longValue());
    final Ledger fetchedSubLedger = ledgerPage.getLedgers().get(0);
    Assert.assertEquals(parent.getIdentifier(), fetchedSubLedger.getParentLedgerIdentifier());

    this.mockMvc.perform(get(path + "/ledgers/" + parent.getIdentifier() + "/")
            .accept(MediaType.ALL_VALUE)
            .contentType(MediaType.APPLICATION_JSON_VALUE).content(ledgerPage.getLedgers().toString()))
            .andExpect(status().is4xxClientError());
  }

  @Test
  public void shouldFindLedger() throws Exception {
    final Ledger ledger = LedgerGenerator.createRandomLedger();

    this.testSubject.createLedger(ledger);

    this.eventRecorder.wait(EventConstants.POST_LEDGER, ledger.getIdentifier());

    final Ledger foundLedger = this.testSubject.findLedger(ledger.getIdentifier());

    Assert.assertNotNull(foundLedger);
    Assert.assertEquals(ledger.getIdentifier(), foundLedger.getIdentifier());
    Assert.assertEquals(ledger.getType(), foundLedger.getType());
    Assert.assertEquals(ledger.getName(), foundLedger.getName());
    Assert.assertEquals(ledger.getDescription(), foundLedger.getDescription());
    Assert.assertNull(ledger.getParentLedgerIdentifier());
    Assert.assertTrue(foundLedger.getSubLedgers().size() == 0);
    Assert.assertNotNull(foundLedger.getCreatedBy());
    Assert.assertNotNull(foundLedger.getCreatedOn());
    Assert.assertNull(foundLedger.getLastModifiedBy());
    Assert.assertNull(foundLedger.getLastModifiedOn());
    Assert.assertEquals(ledger.getShowAccountsInChart(), foundLedger.getShowAccountsInChart());

    this.mockMvc.perform(get(path + "/ledgers/" + ledger.getIdentifier())
            .accept(MediaType.ALL_VALUE).contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(ledger.getIdentifier()))
            .andExpect(status().isNotFound());
  }

  @Test
  public void shouldNotFindLedgerUnknown() throws Exception {
    try {
      this.testSubject.findLedger(RandomStringUtils.randomAlphanumeric(8));
      Assert.fail();
    } catch (final LedgerNotFoundException ex) {
      // do nothing, expected
    }
  }

  @Test
  public void shouldAddSubLedger() throws Exception {
    final Ledger parentLedger = LedgerGenerator.createRandomLedger();

    this.testSubject.createLedger(parentLedger);

    this.eventRecorder.wait(EventConstants.POST_LEDGER, parentLedger.getIdentifier());

    final Ledger subLedger = LedgerGenerator.createRandomLedger();

    this.testSubject.addSubLedger(parentLedger.getIdentifier(), subLedger);

    this.eventRecorder.wait(EventConstants.POST_LEDGER, subLedger.getIdentifier());

    final Ledger foundParentLedger = this.testSubject.findLedger(parentLedger.getIdentifier());
    Assert.assertTrue(foundParentLedger.getSubLedgers().size() == 1);
    final Ledger foundSubLedger = foundParentLedger.getSubLedgers().get(0);
    Assert.assertEquals(subLedger.getIdentifier(), foundSubLedger.getIdentifier());

    this.mockMvc.perform(post(path + "/ledgers/" + parentLedger.getIdentifier())
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isNotFound());
  }

  @Test
  public void shouldNotAddSubLedgerParentUnknown() throws Exception {
    final Ledger subLedger = LedgerGenerator.createRandomLedger();

    try {
      this.testSubject.addSubLedger(RandomStringUtils.randomAlphanumeric(8), subLedger);
      Assert.fail();
    } catch (final LedgerNotFoundException ex) {
      // do nothing, expected
    }
  }

  @Test
  public void shouldNotAddSubLedgerAlreadyExists() throws Exception {
    final Ledger ledger = LedgerGenerator.createRandomLedger();

    this.testSubject.createLedger(ledger);

    this.eventRecorder.wait(EventConstants.POST_LEDGER, ledger.getIdentifier());

    try {
      final Ledger subLedger = LedgerGenerator.createRandomLedger();
      subLedger.setIdentifier(ledger.getIdentifier());
      this.testSubject.addSubLedger(ledger.getIdentifier(), subLedger);
      Assert.fail();
    } catch (final LedgerAlreadyExistsException ex) {
      // do nothing, expected
    }
  }

  @Test
  public void shouldModifyLedger() throws Exception {
    final Ledger ledger = LedgerGenerator.createRandomLedger();

    this.testSubject.createLedger(ledger);

    this.eventRecorder.wait(EventConstants.POST_LEDGER, ledger.getIdentifier());

    ledger.setName(RandomStringUtils.randomAlphabetic(256));
    ledger.setDescription(RandomStringUtils.randomAlphabetic(2048));
    ledger.setShowAccountsInChart(Boolean.TRUE);

    this.testSubject.modifyLedger(ledger.getIdentifier(), ledger);

    this.eventRecorder.wait(EventConstants.PUT_LEDGER, ledger.getIdentifier());

    final Ledger modifiedLedger = this.testSubject.findLedger(ledger.getIdentifier());
    Assert.assertEquals(ledger.getName(), modifiedLedger.getName());
    Assert.assertEquals(ledger.getDescription(), modifiedLedger.getDescription());
    Assert.assertNotNull(modifiedLedger.getLastModifiedBy());
    Assert.assertNotNull(modifiedLedger.getLastModifiedOn());
    Assert.assertEquals(ledger.getShowAccountsInChart(), modifiedLedger.getShowAccountsInChart());

    this.mockMvc.perform(put(path + "/ledgers/" + ledger.getIdentifier())
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isNotFound());
  }

  @Test
  public void shouldNotModifyLedgerIdentifierMismatch() throws Exception {
    final Ledger ledger = LedgerGenerator.createRandomLedger();

    this.testSubject.createLedger(ledger);

    this.eventRecorder.wait(EventConstants.POST_LEDGER, ledger.getIdentifier());

    final String randomName = RandomStringUtils.randomAlphanumeric(8);
    try {
      this.testSubject.modifyLedger(randomName, ledger);
      Assert.fail();
    } catch (final IllegalArgumentException ex) {
      Assert.assertTrue(ex.getMessage().contains(randomName));
      // do nothing, expected
    }
  }

  @Test
  public void shouldNotModifyLedgerUnknown() {
    final Ledger ledger = LedgerGenerator.createRandomLedger();

    try {
      this.testSubject.modifyLedger(ledger.getIdentifier(), ledger);
      Assert.fail();
    } catch (final LedgerNotFoundException ex) {
      // do nothing , expected
    }
  }

  @Test
  public void shouldDeleteLedger() throws Exception {
    final Ledger ledger2delete = LedgerGenerator.createRandomLedger();

    this.testSubject.createLedger(ledger2delete);

    this.eventRecorder.wait(EventConstants.POST_LEDGER, ledger2delete.getIdentifier());

    this.testSubject.deleteLedger(ledger2delete.getIdentifier());

    this.eventRecorder.wait(EventConstants.DELETE_LEDGER, ledger2delete.getIdentifier());

    try {
      this.testSubject.findLedger(ledger2delete.getIdentifier());
      Assert.fail();
    } catch (final LedgerNotFoundException ex) {
      // do nothing, expected
    }

    this.mockMvc.perform(delete(path + "/ledgers/" + ledger2delete.getIdentifier())
            .accept(MediaType.ALL_VALUE)
            .contentType(MediaType.APPLICATION_JSON)
            .content(ledger2delete.getIdentifier()))
            .andExpect(status().isNotFound());
  }

  @Test
  public void shouldNotDeleteLedgerUnknown() throws Exception {
    try {
      this.testSubject.deleteLedger(RandomStringUtils.randomAlphanumeric(8));
      Assert.fail();
    } catch (final LedgerNotFoundException ex) {
      // do nothing, expected
    }
  }

  @Test
  public void shouldNotDeleteLedgerHoldsSubLedger() throws Exception {
    final Ledger ledger2delete = LedgerGenerator.createRandomLedger();
    ledger2delete.setSubLedgers(Collections.singletonList(LedgerGenerator.createRandomLedger()));

    this.testSubject.createLedger(ledger2delete);

    this.eventRecorder.wait(EventConstants.POST_LEDGER, ledger2delete.getIdentifier());

    try {
      this.testSubject.deleteLedger(ledger2delete.getIdentifier());
      Assert.fail();
    } catch (final LedgerReferenceExistsException ex) {
      // do nothing, expected
    }
  }

  @Test
  public void shouldNotDeleteLedgerHoldsAccount() throws Exception {
    final Ledger ledger2delete = LedgerGenerator.createRandomLedger();

    this.testSubject.createLedger(ledger2delete);

    this.eventRecorder.wait(EventConstants.POST_LEDGER, ledger2delete.getIdentifier());

    final Account ledgerAccount = AccountGenerator.createRandomAccount(ledger2delete.getIdentifier());

    this.testSubject.createAccount(ledgerAccount);

    this.eventRecorder.wait(EventConstants.POST_ACCOUNT, ledgerAccount.getIdentifier());

    try {
      this.testSubject.deleteLedger(ledger2delete.getIdentifier());
      Assert.fail();
    } catch (final LedgerReferenceExistsException ex) {
      // do nothing, expected
    }
  }

  @Test
  public void shouldFindLedgerWithSeparatorInIdentifier() throws Exception {
    // RFC 3986 unreserved characters: ALPHA  DIGIT  "-", ".", "_", "~"
    final String[] unreservedCharacters = new String[] {
            "-",
            ".",
            "_"
    };

    this.logger.info("Creating {} ledgers with unreserved characters.", unreservedCharacters.length);
    boolean failed = false;
    for (String unreservedCharacter : unreservedCharacters) {
      final Ledger ledger = LedgerGenerator.createRandomLedger();
      final String identifier = RandomStringUtils.randomAlphanumeric(3) + unreservedCharacter + RandomStringUtils.randomAlphanumeric(2);
      ledger.setIdentifier(identifier);

      this.logger.info("Creating ledger '{}' with unreserved character '{}' in identifier.", identifier, unreservedCharacter);
      this.testSubject.createLedger(ledger);

      Assert.assertTrue(this.eventRecorder.wait(EventConstants.POST_LEDGER, ledger.getIdentifier()));

      try {
        this.testSubject.findLedger(ledger.getIdentifier());
        this.logger.info("Ledger '{}' with unreserved character '{}' in identifier found.", identifier, unreservedCharacter);
      } catch (final Exception ex) {
        this.logger.error("Ledger '{}' with unreserved character '{}' in identifier not found.", identifier, unreservedCharacter);
        failed = true;
      }
    }

    Assert.assertFalse(failed);
  }

  @Test
  public void shouldStreamAllAccountsBelongingToLedger() throws InterruptedException {
    final Ledger assetLedger = LedgerGenerator.createRandomLedger();
    assetLedger.setType(AccountType.ASSET.name());
    this.testSubject.createLedger(assetLedger);
    this.eventRecorder.wait(EventConstants.POST_LEDGER, assetLedger.getIdentifier());

    final List<Account> createdAssetAccounts = Stream.generate(() -> AccountGenerator.createRandomAccount(assetLedger.getIdentifier())).limit(1)
            .peek(account -> {
              account.setType(AccountType.ASSET.name());
              this.testSubject.createAccount(account);
            })
            .collect(Collectors.toList());

    for (final Account account : createdAssetAccounts) {
      this.eventRecorder.wait(EventConstants.POST_ACCOUNT, account.getIdentifier());
    }

    final List<Account> foundAccounts = testSubject.streamAccountsOfLedger(assetLedger.getIdentifier(), "ASC")
            .peek(account -> account.setState(null))
            .collect(Collectors.toList());

    Assert.assertEquals(createdAssetAccounts, foundAccounts);

    try{

      this.mockMvc.perform(get(path + "/ledgers/" + assetLedger.getIdentifier() + "/accounts" )
              .accept(MediaType.ALL_VALUE)
              .contentType(MediaType.APPLICATION_JSON_VALUE))
              .andExpect(status().is4xxClientError());
    } catch (Exception exception){ exception.printStackTrace(); }

  }
}
