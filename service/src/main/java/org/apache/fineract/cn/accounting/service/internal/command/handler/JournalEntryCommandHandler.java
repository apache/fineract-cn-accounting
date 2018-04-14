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
import org.apache.fineract.cn.accounting.api.v1.domain.Creditor;
import org.apache.fineract.cn.accounting.api.v1.domain.Debtor;
import org.apache.fineract.cn.accounting.api.v1.domain.JournalEntry;
import org.apache.fineract.cn.accounting.service.internal.command.BookJournalEntryCommand;
import org.apache.fineract.cn.accounting.service.internal.command.CreateJournalEntryCommand;
import org.apache.fineract.cn.accounting.service.internal.command.ReleaseJournalEntryCommand;
import org.apache.fineract.cn.accounting.service.internal.repository.CreditorType;
import org.apache.fineract.cn.accounting.service.internal.repository.DebtorType;
import org.apache.fineract.cn.accounting.service.internal.repository.JournalEntryEntity;
import org.apache.fineract.cn.accounting.service.internal.repository.JournalEntryRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.fineract.cn.api.util.UserContextHolder;
import org.apache.fineract.cn.command.annotation.Aggregate;
import org.apache.fineract.cn.command.annotation.CommandHandler;
import org.apache.fineract.cn.command.annotation.CommandLogLevel;
import org.apache.fineract.cn.command.annotation.EventEmitter;
import org.apache.fineract.cn.command.gateway.CommandGateway;
import org.apache.fineract.cn.lang.DateConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@SuppressWarnings("unused")
@Aggregate
public class JournalEntryCommandHandler {

  private final CommandGateway commandGateway;
  private final JournalEntryRepository journalEntryRepository;

  @Autowired
  public JournalEntryCommandHandler(final CommandGateway commandGateway,
                                    final JournalEntryRepository journalEntryRepository) {
    this.commandGateway = commandGateway;
    this.journalEntryRepository = journalEntryRepository;
  }

  @Transactional
  @CommandHandler(logStart = CommandLogLevel.NONE, logFinish = CommandLogLevel.NONE)
  @EventEmitter(selectorName = EventConstants.SELECTOR_NAME, selectorValue = EventConstants.POST_JOURNAL_ENTRY)
  public String createJournalEntry(final CreateJournalEntryCommand createJournalEntryCommand) {
    final JournalEntry journalEntry = createJournalEntryCommand.journalEntry();
    final Set<Debtor> debtors = journalEntry.getDebtors();
    final Set<DebtorType> debtorTypes = debtors
        .stream()
        .map(debtor -> {
          final DebtorType debtorType = new DebtorType();
          debtorType.setAccountNumber(debtor.getAccountNumber());
          debtorType.setAmount(Double.valueOf(debtor.getAmount()));
          return debtorType;
        })
        .collect(Collectors.toSet());
    final Set<Creditor> creditors = journalEntry.getCreditors();
    final Set<CreditorType> creditorTypes = creditors
        .stream()
        .map(creditor -> {
          final CreditorType creditorType = new CreditorType();
          creditorType.setAccountNumber(creditor.getAccountNumber());
          creditorType.setAmount(Double.valueOf(creditor.getAmount()));
          return creditorType;
        })
        .collect(Collectors.toSet());
    final JournalEntryEntity journalEntryEntity = new JournalEntryEntity();
    journalEntryEntity.setTransactionIdentifier(journalEntry.getTransactionIdentifier());
    final LocalDateTime transactionDate = DateConverter.fromIsoString(journalEntry.getTransactionDate());
    journalEntryEntity.setDateBucket(DateConverter.toIsoString(DateConverter.toLocalDate(transactionDate)));
    journalEntryEntity.setTransactionDate(transactionDate);
    journalEntryEntity.setTransactionType(journalEntry.getTransactionType());
    journalEntryEntity.setClerk(journalEntry.getClerk() != null ? journalEntry.getClerk() : UserContextHolder.checkedGetUser());
    journalEntryEntity.setNote(journalEntry.getNote());
    journalEntryEntity.setDebtors(debtorTypes);
    journalEntryEntity.setCreditors(creditorTypes);
    journalEntryEntity.setMessage(journalEntry.getMessage());
    journalEntryEntity.setState(JournalEntry.State.PENDING.name());
    journalEntryEntity.setCreatedBy(UserContextHolder.checkedGetUser());
    journalEntryEntity.setCreatedOn(LocalDateTime.now(Clock.systemUTC()));
    journalEntryRepository.saveJournalEntry(journalEntryEntity);
    this.commandGateway.process(new BookJournalEntryCommand(journalEntry.getTransactionIdentifier()));
    return journalEntry.getTransactionIdentifier();
  }

  @Transactional
  @CommandHandler(logStart = CommandLogLevel.NONE, logFinish = CommandLogLevel.NONE)
  public void releaseJournalEntry(final ReleaseJournalEntryCommand releaseJournalEntryCommand) {
    final String transactionIdentifier = releaseJournalEntryCommand.transactionIdentifier();
    final Optional<JournalEntryEntity> optionalJournalEntry = this.journalEntryRepository.findJournalEntry(transactionIdentifier);
    if (optionalJournalEntry.isPresent()) {
      final JournalEntryEntity journalEntryEntity = optionalJournalEntry.get();
      journalEntryEntity.setState(JournalEntry.State.PROCESSED.name());
      this.journalEntryRepository.saveJournalEntry(journalEntryEntity);
    }
  }
}
