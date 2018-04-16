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

public class FinancialCondition {

  @NotEmpty
  private String date;
  @NotEmpty
  private List<FinancialConditionSection> financialConditionSections = new ArrayList<>();
  @NotNull
  private BigDecimal totalAssets;
  @NotNull
  private BigDecimal totalEquitiesAndLiabilities;

  public FinancialCondition() {
    super();
  }

  public String getDate() {
    return this.date;
  }

  public void setDate(final String date) {
    this.date = date;
  }

  public List<FinancialConditionSection> getFinancialConditionSections() {
    return this.financialConditionSections;
  }

  public BigDecimal getTotalAssets() {
    return this.totalAssets;
  }

  public void setTotalAssets(final BigDecimal totalAssets) {
    this.totalAssets = totalAssets;
  }

  public BigDecimal getTotalEquitiesAndLiabilities() {
    return this.totalEquitiesAndLiabilities;
  }

  public void setTotalEquitiesAndLiabilities(final BigDecimal totalEquitiesAndLiabilities) {
    this.totalEquitiesAndLiabilities = totalEquitiesAndLiabilities;
  }

  public void add(final FinancialConditionSection financialConditionSection) {
    this.financialConditionSections.add(financialConditionSection);
  }
}
