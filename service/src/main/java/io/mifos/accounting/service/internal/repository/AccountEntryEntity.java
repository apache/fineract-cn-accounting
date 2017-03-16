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

import io.mifos.core.mariadb.util.LocalDateTimeConverter;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.time.LocalDateTime;

@SuppressWarnings({"unused"})
@Entity
@Table(name = "thoth_account_entries")
public class AccountEntryEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "account_id")
  private AccountEntity account;
  @Column(name = "a_type")
  private String type;
  @Column(name = "transaction_date")
  @Convert(converter = LocalDateTimeConverter.class)
  private LocalDateTime transactionDate;
  @Column(name = "message")
  private String message;
  @Column(name = "amount")
  private Double amount;
  @Column(name = "balance")
  private Double balance;

  public AccountEntryEntity() {
    super();
  }

  public Long getId() {
    return this.id;
  }

  public void setId(final Long id) {
    this.id = id;
  }

  public AccountEntity getAccount() {
    return this.account;
  }

  public void setAccount(final AccountEntity account) {
    this.account = account;
  }

  public String getType() {
    return this.type;
  }

  public void setType(final String type) {
    this.type = type;
  }

  public LocalDateTime getTransactionDate() {
    return this.transactionDate;
  }

  public void setTransactionDate(final LocalDateTime transactionDate) {
    this.transactionDate = transactionDate;
  }

  public String getMessage() {
    return this.message;
  }

  public void setMessage(final String message) {
    this.message = message;
  }

  public Double getAmount() {
    return this.amount;
  }

  public void setAmount(final Double amount) {
    this.amount = amount;
  }

  public Double getBalance() {
    return this.balance;
  }

  public void setBalance(final Double balance) {
    this.balance = balance;
  }
}
