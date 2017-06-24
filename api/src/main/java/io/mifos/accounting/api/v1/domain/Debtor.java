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

import io.mifos.core.lang.validation.constraints.ValidIdentifier;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import java.util.Objects;

@SuppressWarnings({"unused", "WeakerAccess"})
public final class Debtor {
  @ValidIdentifier(maxLength = 34)
  private String accountNumber;
  @NotNull
  @DecimalMin(value = "0.00", inclusive = false)
  private String amount;

  public Debtor() {
    super();
  }

  public Debtor(String accountNumber, String amount) {
    this.accountNumber = accountNumber;
    this.amount = amount;
  }

  public String getAccountNumber() {
    return this.accountNumber;
  }

  public void setAccountNumber(final String accountNumber) {
    this.accountNumber = accountNumber;
  }

  public String getAmount() {
    return this.amount;
  }

  public void setAmount(final String amount) {
    this.amount = amount;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Debtor debtor = (Debtor) o;
    return Objects.equals(accountNumber, debtor.accountNumber) &&
            Objects.equals(amount, debtor.amount);
  }

  @Override
  public int hashCode() {
    return Objects.hash(accountNumber, amount);
  }

  @Override
  public String toString() {
    return "Debtor{" +
            "accountNumber='" + accountNumber + '\'' +
            ", amount='" + amount + '\'' +
            '}';
  }
}
