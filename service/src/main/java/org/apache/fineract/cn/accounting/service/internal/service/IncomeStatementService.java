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
import org.apache.fineract.cn.accounting.api.v1.domain.financial.statement.IncomeStatement;
import org.apache.fineract.cn.accounting.api.v1.domain.financial.statement.IncomeStatementEntry;
import org.apache.fineract.cn.accounting.api.v1.domain.financial.statement.IncomeStatementSection;
import org.apache.fineract.cn.accounting.service.internal.repository.LedgerRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import org.apache.fineract.cn.lang.DateConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class IncomeStatementService {

  private final LedgerRepository ledgerRepository;

  @Autowired
  public IncomeStatementService(final LedgerRepository ledgerRepository) {
    super();
    this.ledgerRepository = ledgerRepository;
  }

  public IncomeStatement getIncomeStatement() {
    final IncomeStatement incomeStatement = new IncomeStatement();
    incomeStatement.setDate(DateConverter.toIsoString(LocalDateTime.now(Clock.systemUTC())));

    this.createIncomeStatementSection(incomeStatement, AccountType.REVENUE, IncomeStatementSection.Type.INCOME);
    this.createIncomeStatementSection(incomeStatement, AccountType.EXPENSE, IncomeStatementSection.Type.EXPENSES);

    incomeStatement.setGrossProfit(this.calculateTotal(incomeStatement, IncomeStatementSection.Type.INCOME));
    incomeStatement.setTotalExpenses(this.calculateTotal(incomeStatement, IncomeStatementSection.Type.EXPENSES));
    incomeStatement.setNetIncome(incomeStatement.getGrossProfit().subtract(incomeStatement.getTotalExpenses()));

    return incomeStatement;
  }

  private void createIncomeStatementSection(final IncomeStatement incomeStatement, final AccountType accountType,
                                            final IncomeStatementSection.Type incomeStatementType) {
    this.ledgerRepository.findByParentLedgerIsNullAndType(accountType.name()).forEach(ledgerEntity -> {
      final IncomeStatementSection incomeStatementSection = new IncomeStatementSection();
      incomeStatementSection.setType(incomeStatementType.name());
      incomeStatementSection.setDescription(ledgerEntity.getName());
      incomeStatement.add(incomeStatementSection);

      this.ledgerRepository.findByParentLedgerOrderByIdentifier(ledgerEntity).forEach(subLedgerEntity -> {
        final IncomeStatementEntry incomeStatementEntry = new IncomeStatementEntry();
        incomeStatementEntry.setDescription(subLedgerEntity.getName());
        final BigDecimal totalValue = subLedgerEntity.getTotalValue() != null ? subLedgerEntity.getTotalValue() : BigDecimal.ZERO;
        incomeStatementEntry.setValue(totalValue);
        incomeStatementSection.add(incomeStatementEntry);
      });
    });
  }

  private BigDecimal calculateTotal(final IncomeStatement incomeStatement, final IncomeStatementSection.Type incomeStatementType) {
    return incomeStatement.getIncomeStatementSections()
        .stream()
        .filter(incomeStatementSection ->
            incomeStatementSection.getType().equals(incomeStatementType.name()))
        .map(IncomeStatementSection::getSubtotal)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }
}
