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
package io.mifos.accounting.service.internal.mapper;

import io.mifos.accounting.api.v1.domain.TransactionType;
import io.mifos.accounting.service.internal.repository.TransactionTypeEntity;

public class TransactionTypeMapper {

  private TransactionTypeMapper() {
    super();
  }

  public static TransactionType map(final TransactionTypeEntity transactionTypeEntity) {
    final TransactionType transactionType = new TransactionType();
    transactionType.setCode(transactionTypeEntity.getIdentifier());
    transactionType.setName(transactionTypeEntity.getName());
    transactionType.setDescription(transactionTypeEntity.getDescription());

    return transactionType;
  }

  public static TransactionTypeEntity map(final TransactionType transactionType) {
    final TransactionTypeEntity transactionTypeEntity = new TransactionTypeEntity();
    transactionTypeEntity.setIdentifier(transactionType.getCode());
    transactionTypeEntity.setName(transactionType.getName());
    transactionTypeEntity.setDescription(transactionType.getDescription());

    return transactionTypeEntity;
  }
}
