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

public class FinancialConditionSection {

  public enum Type {
    ASSET,
    EQUITY,
    LIABILITY
  }

  @NotEmpty
  private Type type;
  @NotEmpty
  private String description;
  @NotEmpty
  private List<FinancialConditionEntry> financialConditionEntries = new ArrayList<>();
  @NotNull
  private BigDecimal subtotal = BigDecimal.ZERO;

  public FinancialConditionSection() {
    super();
  }

  public String getType() {
    return this.type.name();
  }

  public void setType(final String type) {
    this.type = Type.valueOf(type);
  }

  public String getDescription() {
    return this.description;
  }

  public void setDescription(final String description) {
    this.description = description;
  }

  public List<FinancialConditionEntry> getFinancialConditionEntries() {
    return this.financialConditionEntries;
  }

  public BigDecimal getSubtotal() {
    return this.subtotal;
  }

  public void add(final FinancialConditionEntry financialConditionEntry) {
    this.financialConditionEntries.add(financialConditionEntry);
    this.subtotal = this.subtotal.add(financialConditionEntry.getValue());
  }
}
