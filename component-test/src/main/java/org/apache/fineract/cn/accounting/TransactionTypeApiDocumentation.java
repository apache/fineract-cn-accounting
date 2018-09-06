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
import org.apache.fineract.cn.accounting.util.TransactionTypeGenerator;
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
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class TransactionTypeApiDocumentation extends AbstractAccountingTest {

  @Rule
  public final JUnitRestDocumentation restDocumentation = new JUnitRestDocumentation("build/doc/generated-snippets/test-transaction-type");

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
  public void documentCreateTransactionType ( ) throws Exception {

    final TransactionType transactionType = TransactionTypeGenerator.createRandomTransactionType();
    transactionType.setCode("ABCD");
    transactionType.setName("Account Starter");
    transactionType.setDescription("Account Starter");

    Gson gson = new Gson();
    this.mockMvc.perform(post("/transactiontypes")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .content(gson.toJson(transactionType)))
            .andExpect(status().isAccepted())
            .andDo(document("document-create-transaction-type", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()),
                    requestFields(
                            fieldWithPath("code").description("Transaction Type's code"),
                            fieldWithPath("name").description("Name of transaction type"),
                            fieldWithPath("description").description("Description of transaction type")
                    )));
  }

  @Test
  public void documentFindTransactionType ( ) throws Exception {

    final TransactionType transactionType = TransactionTypeGenerator.createRandomTransactionType();
    transactionType.setCode("AXYZ");
    transactionType.setName("Account Lock");
    transactionType.setDescription("Lock Account");
    this.testSubject.createTransactionType(transactionType);
    this.eventRecorder.wait(EventConstants.POST_TX_TYPE, transactionType.getCode());

    Gson gson = new Gson();
    this.mockMvc.perform(get("/transactiontypes/" + transactionType.getCode())
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .content(gson.toJson(transactionType)))
            .andExpect(status().isOk())
            .andDo(document("document-find-transaction-type", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()),
                    responseFields(
                            fieldWithPath("code").description("Transaction Type's code"),
                            fieldWithPath("name").description("Name of transaction type"),
                            fieldWithPath("description").description("Description of transaction type")
                    )));

  }

  @Test
  public void documentFetchTransactionType ( ) throws Exception {

    final TransactionTypePage transactionTypePage =
            super.testSubject.fetchTransactionTypes(null, 0, 10, "code", Sort.Direction.DESC.name());

    this.mockMvc.perform(get("/transactiontypes")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .accept(MediaType.ALL_VALUE))
            .andExpect(status().isOk())
            .andDo(document("document-fetch-transaction-type", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint())));
  }

  @Test
  public void documentChangeTransactionType ( ) throws Exception {

    final TransactionType transactionType = TransactionTypeGenerator.createRandomTransactionType();
    transactionType.setCode("AZYX");
    transactionType.setName("Account Locked");
    transactionType.setDescription("Locked Account");
    this.testSubject.createTransactionType(transactionType);
    this.eventRecorder.wait(EventConstants.POST_TX_TYPE, transactionType.getCode());

    transactionType.setName("Account UnveilOne");
    transactionType.setDescription("Unveiled Account");

    Gson gson = new Gson();
    this.mockMvc.perform(put("/transactiontypes/" + transactionType.getCode())
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .content(gson.toJson(transactionType)))
            .andExpect(status().isAccepted())
            .andDo(document("document-change-transaction-type", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()),
                    requestFields(
                            fieldWithPath("code").description("Transaction Type's code"),
                            fieldWithPath("name").description("Name of transaction type"),
                            fieldWithPath("description").description("Description of transaction type")
                    )));
  }
}
