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
package org.apache.fineract.cn.accounting.api.v1.domain;

import java.util.Objects;
import java.util.Set;
import javax.validation.constraints.NotNull;
import org.apache.fineract.cn.lang.validation.constraints.ValidIdentifier;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.NotEmpty;

@SuppressWarnings({"unused", "WeakerAccess"})
public final class Account {

  private AccountType type;
  @ValidIdentifier(maxLength = 34)
  private String identifier;
  @NotEmpty
  @Length(max = 256)
  private String name;
  private Set<String> holders;
  private Set<String> signatureAuthorities;
  @NotNull
  private Double balance;
  private String referenceAccount;
  @ValidIdentifier
  private String ledger;
  private State state;
  private String alternativeAccountNumber;
  private String createdOn;
  private String createdBy;
  private String lastModifiedOn;
  private String lastModifiedBy;

  public Account() {
    super();
  }

  public String getType() {
    return this.type.name();
  }

  public void setType(final String type) {
    this.type = AccountType.valueOf(type);
  }

  public String getIdentifier() {
    return this.identifier;
  }

  public void setIdentifier(final String identifier) {
    this.identifier = identifier;
  }

  public String getName() {
    return this.name;
  }

  public void setName(final String name) {
    this.name = name;
  }

  public Set<String> getHolders() {
    return this.holders;
  }

  public void setHolders(final Set<String> holders) {
    this.holders = holders;
  }

  public Set<String> getSignatureAuthorities() {
    return this.signatureAuthorities;
  }

  public void setSignatureAuthorities(final Set<String> signatureAuthorities) {
    this.signatureAuthorities = signatureAuthorities;
  }

  public Double getBalance() {
    return this.balance;
  }

  public void setBalance(final Double balance) {
    this.balance = balance;
  }

  public String getReferenceAccount() {
    return this.referenceAccount;
  }

  public void setReferenceAccount(final String referenceAccount) {
    this.referenceAccount = referenceAccount;
  }

  public String getLedger() {
    return this.ledger;
  }

  public void setLedger(final String ledger) {
    this.ledger = ledger;
  }

  public String getState() {
    return this.state.name();
  }

  public void setState(final String state) {
    if (state == null)
      this.state = null;
    else
      this.state = State.valueOf(state);
  }

  public String getAlternativeAccountNumber() {
    return this.alternativeAccountNumber;
  }

  public void setAlternativeAccountNumber(final String alternativeAccountNumber) {
    this.alternativeAccountNumber = alternativeAccountNumber;
  }

  public String getCreatedOn() {
    return this.createdOn;
  }

  public void setCreatedOn(final String createdOn) {
    this.createdOn = createdOn;
  }

  public String getCreatedBy() {
    return this.createdBy;
  }

  public void setCreatedBy(final String createdBy) {
    this.createdBy = createdBy;
  }

  public String getLastModifiedOn() {
    return this.lastModifiedOn;
  }

  public void setLastModifiedOn(final String lastModifiedOn) {
    this.lastModifiedOn = lastModifiedOn;
  }

  public String getLastModifiedBy() {
    return this.lastModifiedBy;
  }

  public void setLastModifiedBy(final String lastModifiedBy) {
    this.lastModifiedBy = lastModifiedBy;
  }

  @SuppressWarnings("WeakerAccess")
  public enum State {
    OPEN,
    LOCKED,
    CLOSED
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Account account = (Account) o;
    return type == account.type &&
            Objects.equals(identifier, account.identifier) &&
            Objects.equals(name, account.name) &&
            Objects.equals(holders, account.holders) &&
            Objects.equals(signatureAuthorities, account.signatureAuthorities) &&
            Objects.equals(balance, account.balance) &&
            Objects.equals(referenceAccount, account.referenceAccount) &&
            Objects.equals(ledger, account.ledger) &&
            state == account.state;
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, identifier, name, holders, signatureAuthorities, balance, referenceAccount, ledger, state);
  }

  @Override
  public String toString() {
    return "Account{" +
            "type=" + type +
            ", identifier='" + identifier + '\'' +
            ", name='" + name + '\'' +
            ", holders=" + holders +
            ", signatureAuthorities=" + signatureAuthorities +
            ", balance=" + balance +
            ", referenceAccount='" + referenceAccount + '\'' +
            ", ledger='" + ledger + '\'' +
            ", state=" + state +
            ", createdOn='" + createdOn + '\'' +
            ", createdBy='" + createdBy + '\'' +
            ", lastModifiedOn='" + lastModifiedOn + '\'' +
            ", lastModifiedBy='" + lastModifiedBy + '\'' +
            '}';
  }
}
