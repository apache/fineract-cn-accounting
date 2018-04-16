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

import org.apache.fineract.cn.accounting.api.v1.domain.JournalEntry;
import org.apache.fineract.cn.accounting.service.ServiceConstants;
import org.apache.fineract.cn.accounting.service.internal.mapper.JournalEntryMapper;
import org.apache.fineract.cn.accounting.service.internal.repository.DebtorType;
import org.apache.fineract.cn.accounting.service.internal.repository.JournalEntryEntity;
import org.apache.fineract.cn.accounting.service.internal.repository.JournalEntryRepository;
import org.apache.fineract.cn.accounting.service.internal.repository.TransactionTypeEntity;
import org.apache.fineract.cn.accounting.service.internal.repository.TransactionTypeRepository;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.fineract.cn.lang.DateRange;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class JournalEntryService {

  private Logger logger;
  private final JournalEntryRepository journalEntryRepository;
  private final TransactionTypeRepository transactionTypeRepository;

  @Autowired
  public JournalEntryService(@Qualifier(ServiceConstants.LOGGER_NAME) final Logger logger,
                             final JournalEntryRepository journalEntryRepository,
                             final TransactionTypeRepository transactionTypeRepository) {
    super();
    this.logger = logger;
    this.journalEntryRepository = journalEntryRepository;
    this.transactionTypeRepository = transactionTypeRepository;
  }

  public List<JournalEntry> fetchJournalEntries(final DateRange range, final String accountNumber, final BigDecimal amount) {
    final List<JournalEntryEntity> journalEntryEntities =
        this.journalEntryRepository.fetchJournalEntries(range);

    if (journalEntryEntities != null) {

      final List<JournalEntryEntity> filteredList =
          journalEntryEntities
              .stream()
              .filter(journalEntryEntity ->
                  accountNumber == null
                      || journalEntryEntity.getDebtors().stream()
                          .anyMatch(debtorType -> debtorType.getAccountNumber().equals(accountNumber))
                      || journalEntryEntity.getCreditors().stream()
                          .anyMatch(creditorType -> creditorType.getAccountNumber().equals(accountNumber))
              )
              .filter(journalEntryEntity ->
                  amount == null
                      || amount.compareTo(
                          BigDecimal.valueOf(
                              journalEntryEntity.getDebtors().stream().mapToDouble(DebtorType::getAmount).sum()
                          )
                  ) == 0
              )
              .sorted(Comparator.comparing(JournalEntryEntity::getTransactionDate))
              .collect(Collectors.toList());

      final List<TransactionTypeEntity> transactionTypes = this.transactionTypeRepository.findAll();
      final HashMap<String, String> mappedTransactionTypes = new HashMap<>(transactionTypes.size());
      transactionTypes.forEach(transactionTypeEntity ->
          mappedTransactionTypes.put(transactionTypeEntity.getIdentifier(), transactionTypeEntity.getName())
      );

      return filteredList
          .stream()
          .map(journalEntryEntity -> {
            final JournalEntry journalEntry = JournalEntryMapper.map(journalEntryEntity);
            journalEntry.setTransactionType(mappedTransactionTypes.get(journalEntry.getTransactionType()));
            return journalEntry;
          })
          .collect(Collectors.toList());
    } else {
      return Collections.emptyList();
    }
  }

  public Optional<JournalEntry> findJournalEntry(final String transactionIdentifier) {
    final Optional<JournalEntryEntity> optionalJournalEntryEntity = this.journalEntryRepository.findJournalEntry(transactionIdentifier);

    return optionalJournalEntryEntity.map(JournalEntryMapper::map);
  }
}
