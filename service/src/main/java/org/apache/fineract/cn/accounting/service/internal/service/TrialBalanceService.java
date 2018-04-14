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

import org.apache.fineract.cn.accounting.api.v1.domain.AccountType;
import org.apache.fineract.cn.accounting.api.v1.domain.financial.statement.TrialBalance;
import org.apache.fineract.cn.accounting.api.v1.domain.financial.statement.TrialBalanceEntry;
import org.apache.fineract.cn.accounting.service.internal.mapper.LedgerMapper;
import org.apache.fineract.cn.accounting.service.internal.repository.LedgerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;

@Service
public class TrialBalanceService {

  private final LedgerRepository ledgerRepository;

  @Autowired
  public TrialBalanceService(final LedgerRepository ledgerRepository) {
    super();
    this.ledgerRepository = ledgerRepository;
  }

  public TrialBalance getTrialBalance(final boolean includeEmptyEntries) {
    final TrialBalance trialBalance = new TrialBalance();
    this.ledgerRepository.findByParentLedgerIsNull().forEach(ledgerEntity ->
      this.ledgerRepository.findByParentLedgerOrderByIdentifier(ledgerEntity).forEach(subLedger -> {
        final BigDecimal totalValue = subLedger.getTotalValue() != null ? subLedger.getTotalValue() : BigDecimal.ZERO;
        if (!includeEmptyEntries && totalValue.compareTo(BigDecimal.ZERO) == 0) {
          return;
        }
        final TrialBalanceEntry trialBalanceEntry = new TrialBalanceEntry();
        trialBalanceEntry.setLedger(LedgerMapper.map(subLedger));
        switch (AccountType.valueOf(subLedger.getType())) {
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
        trialBalanceEntry.setAmount(totalValue);
        trialBalance.getTrialBalanceEntries().add(trialBalanceEntry);
      })
    );

    trialBalance.setDebitTotal(
        trialBalance.getTrialBalanceEntries()
            .stream()
            .filter(trialBalanceEntry -> trialBalanceEntry.getType().equals(TrialBalanceEntry.Type.DEBIT.name()))
            .map(TrialBalanceEntry::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
    );

    trialBalance.setCreditTotal(
        trialBalance.getTrialBalanceEntries()
            .stream()
            .filter(trialBalanceEntry -> trialBalanceEntry.getType().equals(TrialBalanceEntry.Type.CREDIT.name()))
            .map(TrialBalanceEntry::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
    );

    // Sort by ledger identifier ASC
    trialBalance.getTrialBalanceEntries().sort(Comparator.comparing(trailBalanceEntry -> trailBalanceEntry.getLedger().getIdentifier()));

    return trialBalance;
  }
}
