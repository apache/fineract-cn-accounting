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
package io.mifos.accounting.service.internal.service;

import io.mifos.accounting.api.v1.domain.ChartOfAccountEntry;
import io.mifos.accounting.service.internal.repository.AccountEntity;
import io.mifos.accounting.service.internal.repository.AccountRepository;
import io.mifos.accounting.service.internal.repository.LedgerEntity;
import io.mifos.accounting.service.internal.repository.LedgerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class ChartOfAccountsService {

  private final LedgerRepository ledgerRepository;
  private final AccountRepository accountRepository;

  @Autowired
  public ChartOfAccountsService(final LedgerRepository ledgerRepository, final AccountRepository accountRepository) {
    super();
    this.ledgerRepository = ledgerRepository;
    this.accountRepository = accountRepository;
  }

  @Transactional(readOnly = true)
  public List<ChartOfAccountEntry> getChartOfAccounts() {
    final ArrayList<ChartOfAccountEntry> chartOfAccountEntries = new ArrayList<>();

    final List<LedgerEntity> parentLedgers = this.ledgerRepository.findByParentLedgerIsNull();
    parentLedgers.sort(Comparator.comparing(LedgerEntity::getIdentifier));

    final int level = 0;
    parentLedgers.forEach(ledgerEntity -> {
      final ChartOfAccountEntry chartOfAccountEntry = new ChartOfAccountEntry();
      chartOfAccountEntries.add(chartOfAccountEntry);
      chartOfAccountEntry.setCode(ledgerEntity.getIdentifier());
      chartOfAccountEntry.setName(ledgerEntity.getName());
      chartOfAccountEntry.setDescription(ledgerEntity.getDescription());
      chartOfAccountEntry.setType(ledgerEntity.getType());
      chartOfAccountEntry.setLevel(Integer.valueOf(level));
      final int nextLevel = level + 1;
      this.traverseHierarchy(chartOfAccountEntries, nextLevel, ledgerEntity);
    });

    return chartOfAccountEntries;
  }

  void traverseHierarchy(final List<ChartOfAccountEntry> chartOfAccountEntries, final int level, final LedgerEntity ledgerEntity) {
    if (ledgerEntity.getShowAccountsInChart()) {
      final List<AccountEntity> accountEntities = this.accountRepository.findByLedger(ledgerEntity);
      accountEntities.sort(Comparator.comparing(AccountEntity::getIdentifier));
      accountEntities.forEach(accountEntity -> {
        final ChartOfAccountEntry chartOfAccountEntry = new ChartOfAccountEntry();
        chartOfAccountEntries.add(chartOfAccountEntry);
        chartOfAccountEntry.setCode(accountEntity.getIdentifier());
        chartOfAccountEntry.setName(accountEntity.getName());
        chartOfAccountEntry.setType(accountEntity.getType());
        chartOfAccountEntry.setLevel(level);
      });
    }

    final List<LedgerEntity> subLedgers = this.ledgerRepository.findByParentLedgerOrderByIdentifier(ledgerEntity);
    if (subLedgers != null && subLedgers.size() > 0) {
      subLedgers.sort(Comparator.comparing(LedgerEntity::getIdentifier));
      subLedgers.forEach(subLedger -> {
        final ChartOfAccountEntry chartOfAccountEntry = new ChartOfAccountEntry();
        chartOfAccountEntries.add(chartOfAccountEntry);
        chartOfAccountEntry.setCode(subLedger.getIdentifier());
        chartOfAccountEntry.setName(subLedger.getName());
        chartOfAccountEntry.setType(subLedger.getType());
        chartOfAccountEntry.setLevel(level);
        final int nextLevel = level + 1;
        this.traverseHierarchy(chartOfAccountEntries, nextLevel, subLedger);
      });
    }
  }
}
