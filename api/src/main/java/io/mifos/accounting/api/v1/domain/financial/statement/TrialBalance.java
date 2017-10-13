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
package io.mifos.accounting.api.v1.domain.financial.statement;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class TrialBalance {

  private List<TrialBalanceEntry> trialBalanceEntries;
  private BigDecimal debitTotal;
  private BigDecimal creditTotal;

  public TrialBalance() {
    super();
  }

  public List<TrialBalanceEntry> getTrialBalanceEntries() {
    if (this.trialBalanceEntries == null) {
      this.trialBalanceEntries = new ArrayList<>();
    }
    return this.trialBalanceEntries;
  }

  public void setTrialBalanceEntries(final List<TrialBalanceEntry> trialBalanceEntries) {
    this.trialBalanceEntries = trialBalanceEntries;
  }

  public BigDecimal getDebitTotal() {
    return this.debitTotal;
  }

  public void setDebitTotal(final BigDecimal debitTotal) {
    this.debitTotal = debitTotal;
  }

  public BigDecimal getCreditTotal() {
    return this.creditTotal;
  }

  public void setCreditTotal(final BigDecimal creditTotal) {
    this.creditTotal = creditTotal;
  }
}
