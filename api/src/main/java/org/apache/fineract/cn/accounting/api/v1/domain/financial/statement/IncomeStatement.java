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
package org.apache.fineract.cn.accounting.api.v1.domain.financial.statement;

import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class IncomeStatement {

  @NotEmpty
  private String date;
  @NotEmpty
  private List<IncomeStatementSection> incomeStatementSections = new ArrayList<>();
  @NotNull
  private BigDecimal grossProfit;
  @NotNull
  private BigDecimal totalExpenses;
  @NotNull
  private BigDecimal netIncome;

  public IncomeStatement() {
    super();
  }

  public String getDate() {
    return this.date;
  }

  public void setDate(final String date) {
    this.date = date;
  }

  public List<IncomeStatementSection> getIncomeStatementSections() {
    return this.incomeStatementSections;
  }

  public BigDecimal getGrossProfit() {
    return this.grossProfit;
  }

  public void setGrossProfit(final BigDecimal grossProfit) {
    this.grossProfit = grossProfit;
  }

  public BigDecimal getTotalExpenses() {
    return this.totalExpenses;
  }

  public void setTotalExpenses(final BigDecimal totalExpenses) {
    this.totalExpenses = totalExpenses;
  }

  public BigDecimal getNetIncome() {
    return this.netIncome;
  }

  public void setNetIncome(final BigDecimal netIncome) {
    this.netIncome = netIncome;
  }

  public void add(final IncomeStatementSection incomeStatementSection) {
    this.incomeStatementSections.add(incomeStatementSection);
  }
}
