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

import org.apache.fineract.cn.accounting.api.v1.domain.Ledger;

import java.math.BigDecimal;

@SuppressWarnings("WeakerAccess")
public class TrialBalanceEntry {

  private Ledger ledger;
  private Type type;
  private BigDecimal amount;

  public TrialBalanceEntry() {
    super();
  }

  public Ledger getLedger() {
    return this.ledger;
  }

  public void setLedger(final Ledger ledger) {
    this.ledger = ledger;
  }

  public String getType() {
    return this.type.name();
  }

  public void setType(final String type) {
    this.type = Type.valueOf(type);
  }

  public BigDecimal getAmount() {
    return this.amount;
  }

  public void setAmount(final BigDecimal amount) {
    this.amount = amount;
  }

  public enum Type {
    DEBIT,
    CREDIT
  }
}
