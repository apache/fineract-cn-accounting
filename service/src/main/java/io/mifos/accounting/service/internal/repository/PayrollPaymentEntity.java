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
package io.mifos.accounting.service.internal.repository;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "thoth_payroll_payments")
public class PayrollPaymentEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  private Long id;
  @Column(name = "collection_identifier", nullable = false, length = 32)
  private String collectionIdentifier;
  @Column(name = "customer_identifier", nullable = false, length = 32)
  private String customerIdentifier;
  @Column(name = "employer", nullable = false, length = 256)
  private String employer;
  @Column(name = "salary", nullable = false, precision = 15, scale = 5)
  private BigDecimal salary;

  public PayrollPaymentEntity() {
    super();
  }

  public Long getId() {
    return this.id;
  }

  public void setId(final Long id) {
    this.id = id;
  }

  public String getCollectionIdentifier() {
    return this.collectionIdentifier;
  }

  public void setCollectionIdentifier(final String collectionIdentifier) {
    this.collectionIdentifier = collectionIdentifier;
  }

  public String getCustomerIdentifier() {
    return this.customerIdentifier;
  }

  public void setCustomerIdentifier(final String customerIdentifier) {
    this.customerIdentifier = customerIdentifier;
  }

  public String getEmployer() {
    return this.employer;
  }

  public void setEmployer(final String employer) {
    this.employer = employer;
  }

  public BigDecimal getSalary() {
    return this.salary;
  }

  public void setSalary(final BigDecimal salary) {
    this.salary = salary;
  }
}
