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

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;

public class PayrollCollectionSheet {

  @ValidIdentifier(maxLength = 34)
  private String sourceAccountNumber;
  @NotNull
  @Valid
  private List<PayrollPayment> payrollPayments;

  public PayrollCollectionSheet() {
    super();
  }

  public String getSourceAccountNumber() {
    return this.sourceAccountNumber;
  }

  public void setSourceAccountNumber(final String sourceAccountNumber) {
    this.sourceAccountNumber = sourceAccountNumber;
  }

  public List<PayrollPayment> getPayrollPayments() {
    return this.payrollPayments;
  }

  public void setPayrollPayments(final List<PayrollPayment> payrollPayments) {
    this.payrollPayments = payrollPayments;
  }
}
