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
package io.mifos.accounting.service.internal.service.helper;

import io.mifos.customer.api.v1.client.CustomerManager;
import io.mifos.customer.api.v1.client.CustomerNotFoundException;
import io.mifos.customer.api.v1.domain.PayrollDistribution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CustomerAdapter {

  private final CustomerManager customerManager;

  @Autowired
  public CustomerAdapter(final CustomerManager customerManager) {
    super();
    this.customerManager = customerManager;
  }

  public Optional<PayrollDistribution> findPayrollDistribution(final String customerIdentifier) {
    try {
      return Optional.ofNullable(this.customerManager.getPayrollDistribution(customerIdentifier));
    } catch (final CustomerNotFoundException cnfex) {
      return Optional.empty();
    }
  }
}
