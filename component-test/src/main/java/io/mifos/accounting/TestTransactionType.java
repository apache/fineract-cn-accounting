/*
 * Copyright 2017 The Mifos Initiative.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.mifos.accounting;

import io.mifos.accounting.api.v1.EventConstants;
import io.mifos.accounting.api.v1.client.TransactionTypeAlreadyExists;
import io.mifos.accounting.api.v1.client.TransactionTypeNotFoundException;
import io.mifos.accounting.api.v1.client.TransactionTypeValidationException;
import io.mifos.accounting.api.v1.domain.TransactionType;
import io.mifos.accounting.api.v1.domain.TransactionTypePage;
import io.mifos.accounting.util.TransactionTypeGenerator;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.data.domain.Sort;

public class TestTransactionType extends AbstractAccountingTest {

  public TestTransactionType() {
    super();
  }

  @Test
  public void shouldFetchPreConfiguredTypes() {
    final TransactionTypePage transactionTypePage =
        super.testSubject.fetchTransactionTypes(null, 0, 100, "identifier", Sort.Direction.DESC.name());

    Assert.assertTrue(transactionTypePage.getTotalElements() >= 54L);
  }

  @Test
  public void shouldCreateTransactionType() throws Exception {
    final TransactionType transactionType = TransactionTypeGenerator.createRandomTransactionType();

    super.testSubject.createTransactionType(transactionType);

    Assert.assertTrue(super.eventRecorder.wait(EventConstants.POST_TX_TYPE, transactionType.getCode()));
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
  }

  @Test(expected = TransactionTypeNotFoundException.class)
  public void shouldNotFindTransactionTypeNotFound() {
    super.testSubject.findTransactionType("unknown");
  }
}
