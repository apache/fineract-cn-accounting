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
import org.apache.fineract.cn.accounting.api.v1.domain.financial.statement.FinancialCondition;
import org.apache.fineract.cn.accounting.api.v1.domain.financial.statement.FinancialConditionEntry;
import org.apache.fineract.cn.accounting.api.v1.domain.financial.statement.FinancialConditionSection;
import org.apache.fineract.cn.accounting.service.internal.repository.LedgerRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.EnumSet;
import org.apache.fineract.cn.lang.DateConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FinancialConditionService {

  private final LedgerRepository ledgerRepository;

  @Autowired
  public FinancialConditionService(final LedgerRepository ledgerRepository) {
    super();
    this.ledgerRepository = ledgerRepository;
  }

  public FinancialCondition getFinancialCondition() {
    final FinancialCondition financialCondition = new FinancialCondition();
    financialCondition.setDate(DateConverter.toIsoString(LocalDateTime.now(Clock.systemUTC())));

    this.createFinancialConditionSection(financialCondition, AccountType.ASSET, FinancialConditionSection.Type.ASSET);
    this.createFinancialConditionSection(financialCondition, AccountType.EQUITY, FinancialConditionSection.Type.EQUITY);
    this.createFinancialConditionSection(financialCondition, AccountType.LIABILITY, FinancialConditionSection.Type.LIABILITY);

    financialCondition.setTotalAssets(
        this.calculateTotal(financialCondition,
            EnumSet.of(FinancialConditionSection.Type.ASSET))
    );
    financialCondition.setTotalEquitiesAndLiabilities(
        this.calculateTotal(financialCondition,
            EnumSet.of(FinancialConditionSection.Type.EQUITY, FinancialConditionSection.Type.LIABILITY))
    );

    return financialCondition;
  }

  private void createFinancialConditionSection(final FinancialCondition financialCondition, final AccountType accountType,
                                               final FinancialConditionSection.Type financialConditionType) {
    this.ledgerRepository.findByParentLedgerIsNullAndType(accountType.name()).forEach(ledgerEntity -> {
      final FinancialConditionSection financialConditionSection = new FinancialConditionSection();
      financialConditionSection.setType(financialConditionType.name());
      financialConditionSection.setDescription(ledgerEntity.getName());
      financialCondition.add(financialConditionSection);

      this.ledgerRepository.findByParentLedgerOrderByIdentifier(ledgerEntity).forEach(subLedgerEntity -> {
        final FinancialConditionEntry financialConditionEntry = new FinancialConditionEntry();
        financialConditionEntry.setDescription(subLedgerEntity.getName());
        final BigDecimal totalValue = subLedgerEntity.getTotalValue() != null ? subLedgerEntity.getTotalValue() : BigDecimal.ZERO;
        financialConditionEntry.setValue(totalValue);
        financialConditionSection.add(financialConditionEntry);
      });
    });
  }

  private BigDecimal calculateTotal(final FinancialCondition financialCondition,
                                    final EnumSet<FinancialConditionSection.Type> financialConditionTypes) {
    return financialCondition.getFinancialConditionSections()
        .stream()
        .filter(financialConditionSection ->
            financialConditionTypes.contains(FinancialConditionSection.Type.valueOf(financialConditionSection.getType())))
        .map(FinancialConditionSection::getSubtotal)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }
}
