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
package org.apache.fineract.cn.accounting.service.internal.command.handler;

import org.apache.fineract.cn.accounting.api.v1.EventConstants;
import org.apache.fineract.cn.accounting.api.v1.domain.TransactionType;
import org.apache.fineract.cn.accounting.service.internal.command.ChangeTransactionTypeCommand;
import org.apache.fineract.cn.accounting.service.internal.command.CreateTransactionTypeCommand;
import org.apache.fineract.cn.accounting.service.internal.mapper.TransactionTypeMapper;
import org.apache.fineract.cn.accounting.service.internal.repository.TransactionTypeEntity;
import org.apache.fineract.cn.accounting.service.internal.repository.TransactionTypeRepository;
import java.util.Optional;
import org.apache.fineract.cn.command.annotation.Aggregate;
import org.apache.fineract.cn.command.annotation.CommandHandler;
import org.apache.fineract.cn.command.annotation.CommandLogLevel;
import org.apache.fineract.cn.command.annotation.EventEmitter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@SuppressWarnings("unused")
@Aggregate
public class TransactionTypeAggregate {
  private final TransactionTypeRepository transactionTypeRepository;

  @Autowired
  public TransactionTypeAggregate(final TransactionTypeRepository transactionTypeRepository) {
    super();
    this.transactionTypeRepository = transactionTypeRepository;
  }

  @Transactional
  @CommandHandler(logStart = CommandLogLevel.DEBUG, logFinish = CommandLogLevel.DEBUG)
  @EventEmitter(selectorName = EventConstants.SELECTOR_NAME, selectorValue = EventConstants.POST_TX_TYPE)
  public String createTransactionType(final CreateTransactionTypeCommand createTransactionTypeCommand) {
    final TransactionType transactionType = createTransactionTypeCommand.transactionType();

    this.transactionTypeRepository.save(TransactionTypeMapper.map(transactionType));

    return transactionType.getCode();
  }

  @Transactional
  @CommandHandler(logStart = CommandLogLevel.DEBUG, logFinish = CommandLogLevel.DEBUG)
  @EventEmitter(selectorName = EventConstants.SELECTOR_NAME, selectorValue = EventConstants.PUT_TX_TYPE)
  public String changeTransactionType(final ChangeTransactionTypeCommand changeTransactionTypeCommand) {
    final TransactionType transactionType = changeTransactionTypeCommand.transactionType();

    final Optional<TransactionTypeEntity> optionalTransactionTypeEntity =
        this.transactionTypeRepository.findByIdentifier(transactionType.getCode());

    optionalTransactionTypeEntity.ifPresent(transactionTypeEntity -> {
      transactionTypeEntity.setName(transactionType.getName());
      transactionTypeEntity.setDescription(transactionType.getDescription());
      this.transactionTypeRepository.save(transactionTypeEntity);
    });

    return transactionType.getCode();
  }
}
