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
package io.mifos.accounting.api.v1.domain;

@SuppressWarnings("WeakerAccess")
public class TrialBalanceEntry {

  private Ledger ledger;
  private Type type;
  private Double amount;

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

  public Double getAmount() {
    return this.amount;
  }

  public void setAmount(final Double amount) {
    this.amount = amount;
  }

  public enum Type {
    DEBIT,
    CREDIT
  }
}
