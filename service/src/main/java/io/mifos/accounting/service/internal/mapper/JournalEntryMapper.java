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

import io.mifos.accounting.api.v1.domain.Creditor;
import io.mifos.accounting.api.v1.domain.Debtor;
import io.mifos.accounting.api.v1.domain.JournalEntry;
import io.mifos.accounting.service.internal.repository.JournalEntryEntity;
import io.mifos.core.lang.DateConverter;

import java.util.stream.Collectors;

public class JournalEntryMapper {

  private JournalEntryMapper() {
    super();
  }

  public static JournalEntry map(final JournalEntryEntity journalEntryEntity) {
    final JournalEntry journalEntry = new JournalEntry();
    journalEntry.setTransactionIdentifier(journalEntryEntity.getTransactionIdentifier());
    journalEntry.setTransactionDate(DateConverter.toIsoString(journalEntryEntity.getTransactionDate()));
    journalEntry.setTransactionType(journalEntryEntity.getTransactionType());
    journalEntry.setClerk(journalEntryEntity.getClerk());
    journalEntry.setNote(journalEntryEntity.getNote());
    journalEntry.setDebtors(
        journalEntryEntity.getDebtors()
            .stream()
            .map(debtorType -> {
              final Debtor debtor = new Debtor();
              debtor.setAccountNumber(debtorType.getAccountNumber());
              debtor.setAmount(Double.toString(debtorType.getAmount()));
              return debtor;
            })
            .collect(Collectors.toSet())
    );
    journalEntry.setCreditors(
        journalEntryEntity.getCreditors()
            .stream()
            .map(creditorType -> {
              final Creditor creditor = new Creditor();
              creditor.setAccountNumber(creditorType.getAccountNumber());
              creditor.setAmount(Double.toString(creditorType.getAmount()));
              return creditor;
            })
            .collect(Collectors.toSet())
    );
    journalEntry.setMessage(journalEntryEntity.getMessage());
    journalEntry.setState(journalEntryEntity.getState());
    return journalEntry;
  }
}
