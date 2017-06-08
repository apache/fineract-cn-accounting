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
package io.mifos.accounting.service.internal.command.handler;

import io.mifos.accounting.api.v1.EventConstants;
import io.mifos.accounting.api.v1.domain.TransactionType;
import io.mifos.accounting.service.internal.command.ChangeTransactionTypeCommand;
import io.mifos.accounting.service.internal.command.CreateTransactionTypeCommand;
import io.mifos.accounting.service.internal.mapper.TransactionTypeMapper;
import io.mifos.accounting.service.internal.repository.TransactionTypeEntity;
import io.mifos.accounting.service.internal.repository.TransactionTypeRepository;
import io.mifos.core.command.annotation.Aggregate;
import io.mifos.core.command.annotation.CommandHandler;
import io.mifos.core.command.annotation.CommandLogLevel;
import io.mifos.core.command.annotation.EventEmitter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

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
