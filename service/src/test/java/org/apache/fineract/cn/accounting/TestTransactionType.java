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
import org.apache.fineract.cn.accounting.api.v1.client.TransactionTypeAlreadyExists;
import org.apache.fineract.cn.accounting.api.v1.client.TransactionTypeNotFoundException;
import org.apache.fineract.cn.accounting.api.v1.client.TransactionTypeValidationException;
import org.apache.fineract.cn.accounting.api.v1.domain.TransactionType;
import org.apache.fineract.cn.accounting.api.v1.domain.TransactionTypePage;
import org.apache.fineract.cn.accounting.util.TransactionTypeGenerator;
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

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class TestTransactionType extends AbstractAccountingTest {

  public TestTransactionType() {
    super();
  }

  @Rule
  public final JUnitRestDocumentation restDocumentation = new JUnitRestDocumentation("src/doc/generated-snippets/test-transaction");

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
  public void shouldFetchPreConfiguredTypes() {
    final TransactionTypePage transactionTypePage =
            super.testSubject.fetchTransactionTypes(null, 0, 100, "identifier", Sort.Direction.DESC.name());

    Assert.assertTrue(transactionTypePage.getTotalElements() >= 54L);

    try{
      this.mockMvc.perform(get(path + "/transactiontypes/")
              .accept(MediaType.ALL_VALUE)
              .contentType(MediaType.APPLICATION_JSON_VALUE))
              .andExpect(status().is4xxClientError());
    } catch( Exception e ){ e.printStackTrace();}
  }

  @Test
  public void shouldCreateTransactionType() throws Exception {
    final TransactionType transactionType = TransactionTypeGenerator.createRandomTransactionType();

    super.testSubject.createTransactionType(transactionType);

    Assert.assertTrue(super.eventRecorder.wait(EventConstants.POST_TX_TYPE, transactionType.getCode()));

    this.mockMvc.perform(post(path + "/transactiontypes")
            .contentType(MediaType.APPLICATION_JSON_VALUE).accept(MediaType.APPLICATION_JSON_VALUE)
            .content(transactionType.getCode()))
            .andExpect(status().isNotFound());
  }

  @Test(expected = TransactionTypeAlreadyExists.class)
  public void shouldNotCreateTransactionTypeAlreadyExists() throws Exception {
    final TransactionType transactionType = TransactionTypeGenerator.createRandomTransactionType();
    transactionType.setCode("duplicate");

    super.testSubject.createTransactionType(transactionType);

    Assert.assertTrue(super.eventRecorder.wait(EventConstants.POST_TX_TYPE, transactionType.getCode()));

    super.testSubject.createTransactionType(transactionType);
  }

  @Test
  public void shouldChangeTransactionType() throws Exception {
    final String code = "changeable";

    final TransactionType transactionType = TransactionTypeGenerator.createRandomTransactionType();
    transactionType.setCode(code);

    super.testSubject.createTransactionType(transactionType);

    Assert.assertTrue(super.eventRecorder.wait(EventConstants.POST_TX_TYPE, transactionType.getCode()));

    final String changedName = "Changed name";
    transactionType.setName(changedName);

    super.testSubject.changeTransactionType(code, transactionType);

    Assert.assertTrue(super.eventRecorder.wait(EventConstants.PUT_TX_TYPE, transactionType.getCode()));

    final TransactionTypePage transactionTypePage =
            super.testSubject.fetchTransactionTypes(code, 0, 1, null, null);

    Assert.assertTrue(transactionTypePage.getTotalElements() == 1L);
    final TransactionType fetchedTransactionType = transactionTypePage.getTransactionTypes().get(0);
    Assert.assertEquals(changedName, fetchedTransactionType.getName());

    this.mockMvc.perform(put(path + "/transactiontypes/" + transactionType.getCode())
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isNotFound());
  }

  @Test(expected = TransactionTypeNotFoundException.class)
  public void shouldNotChangeTransactionTypeNotFound() {
    final String code = "unknown";

    final TransactionType transactionType = TransactionTypeGenerator.createRandomTransactionType();
    transactionType.setCode(code);

    super.testSubject.changeTransactionType(code, transactionType);
  }

  @Test(expected = TransactionTypeValidationException.class)
  public void shouldNotChangeTransactionTypeCodeMismatch() {
    final String code = "mismatch";

    final TransactionType transactionType = TransactionTypeGenerator.createRandomTransactionType();

    super.testSubject.changeTransactionType(code, transactionType);
  }

  @Test
  public void shouldFindTransactionType() throws Exception {
    final TransactionType randomTransactionType = TransactionTypeGenerator.createRandomTransactionType();

    super.testSubject.createTransactionType(randomTransactionType);

    Assert.assertTrue(super.eventRecorder.wait(EventConstants.POST_TX_TYPE, randomTransactionType.getCode()));

    final TransactionType fetchedTransactionType = super.testSubject.findTransactionType(randomTransactionType.getCode());
    Assert.assertNotNull(fetchedTransactionType);
    Assert.assertEquals(randomTransactionType.getCode(), fetchedTransactionType.getCode());
    Assert.assertEquals(randomTransactionType.getName(), fetchedTransactionType.getName());
    Assert.assertEquals(randomTransactionType.getDescription(), fetchedTransactionType.getDescription());

    this.mockMvc.perform(get(path + "/transactiontypes/" + fetchedTransactionType.getCode())
            .accept(MediaType.ALL_VALUE).contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(fetchedTransactionType.getCode()))
            .andExpect(status().isNotFound());
  }

  @Test(expected = TransactionTypeNotFoundException.class)
  public void shouldNotFindTransactionTypeNotFound() {
    super.testSubject.findTransactionType("unknown");
  }
}
