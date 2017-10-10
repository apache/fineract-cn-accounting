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

import io.mifos.accounting.api.v1.domain.AccountType;
import io.mifos.accounting.api.v1.domain.TrialBalance;
import io.mifos.accounting.api.v1.domain.TrialBalanceEntry;
import io.mifos.accounting.service.internal.mapper.LedgerMapper;
import io.mifos.accounting.service.internal.repository.AccountRepository;
import io.mifos.accounting.service.internal.repository.LedgerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;

@Service
public class TrialBalanceService {

  private final LedgerRepository ledgerRepository;
  private final AccountRepository accountRepository;

  @Autowired
  public TrialBalanceService(final LedgerRepository ledgerRepository,
                             final AccountRepository accountRepository) {
    super();
    this.ledgerRepository = ledgerRepository;
    this.accountRepository = accountRepository;
  }

  public TrialBalance getTrialBalance(final boolean includeEmptyEntries) {
    final TrialBalance trialBalance = new TrialBalance();
    this.ledgerRepository.findAll().forEach(ledgerEntity -> {
      final BigDecimal totalValue = ledgerEntity.getTotalValue() != null ? ledgerEntity.getTotalValue() : BigDecimal.ZERO;
      if (!includeEmptyEntries && totalValue.compareTo(BigDecimal.ZERO) != 0) {
        return;
      }
      final TrialBalanceEntry trialBalanceEntry = new TrialBalanceEntry();
      trialBalanceEntry.setLedger(LedgerMapper.map(ledgerEntity));
      switch (AccountType.valueOf(ledgerEntity.getType())) {
        case ASSET:
        case EXPENSE:
          trialBalanceEntry.setType(TrialBalanceEntry.Type.DEBIT.name());
          break;
        case LIABILITY:
        case EQUITY:
        case REVENUE:
          trialBalanceEntry.setType(TrialBalanceEntry.Type.CREDIT.name());
          break;
      }
      trialBalanceEntry.setAmount(totalValue.doubleValue());
      trialBalance.getTrialBalanceEntries().add(trialBalanceEntry);
    });

    trialBalance.setDebitTotal(
        trialBalance.getTrialBalanceEntries()
            .stream()
            .filter(trialBalanceEntry ->
                trialBalanceEntry.getType().equals(TrialBalanceEntry.Type.DEBIT.name())
                && trialBalanceEntry.getLedger().getParentLedgerIdentifier() == null)
            .mapToDouble(TrialBalanceEntry::getAmount)
            .sum()
    );

    trialBalance.setCreditTotal(
        trialBalance.getTrialBalanceEntries()
            .stream()
            .filter(trialBalanceEntry ->
                trialBalanceEntry.getType().equals(TrialBalanceEntry.Type.CREDIT.name())
                    && trialBalanceEntry.getLedger().getParentLedgerIdentifier() == null)
            .mapToDouble(TrialBalanceEntry::getAmount)
            .sum()
    );

    // Sort by ledger identifier ASC
    trialBalance.getTrialBalanceEntries().sort(Comparator.comparing(o -> o.getLedger().getIdentifier()));

    return trialBalance;
  }
}
