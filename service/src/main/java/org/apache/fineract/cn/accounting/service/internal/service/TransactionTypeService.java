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
package org.apache.fineract.cn.accounting.service.internal.service;

import org.apache.fineract.cn.accounting.api.v1.domain.TransactionType;
import org.apache.fineract.cn.accounting.api.v1.domain.TransactionTypePage;
import org.apache.fineract.cn.accounting.service.internal.mapper.TransactionTypeMapper;
import org.apache.fineract.cn.accounting.service.internal.repository.TransactionTypeEntity;
import org.apache.fineract.cn.accounting.service.internal.repository.TransactionTypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Optional;


@Service
public class TransactionTypeService {

  private final TransactionTypeRepository transactionTypeRepository;

  @Autowired
  public TransactionTypeService(final TransactionTypeRepository transactionTypeRepository) {
    super();
    this.transactionTypeRepository = transactionTypeRepository;
  }

  public TransactionTypePage fetchTransactionTypes(final String term, final Pageable pageable) {
    final Page<TransactionTypeEntity> transactionTypeEntityPage;
    if (term != null) {
      transactionTypeEntityPage =
          this.transactionTypeRepository.findByIdentifierContainingOrNameContaining(term, term, pageable);
    } else {
      transactionTypeEntityPage = this.transactionTypeRepository.findAll(pageable);
    }

    final TransactionTypePage transactionTypePage = new TransactionTypePage();
    transactionTypePage.setTotalElements(transactionTypeEntityPage.getTotalElements());
    transactionTypePage.setTotalPages(transactionTypeEntityPage.getTotalPages());

    transactionTypePage.setTransactionTypes(new ArrayList<>(transactionTypeEntityPage.getSize()));
    transactionTypeEntityPage.forEach(transactionTypeEntity ->
        transactionTypePage.add(TransactionTypeMapper.map(transactionTypeEntity)));

    return transactionTypePage;
  }

  public Optional<TransactionType> findByIdentifier(final String identifier) {
    return this.transactionTypeRepository.findByIdentifier(identifier).map(TransactionTypeMapper::map);
  }
}
