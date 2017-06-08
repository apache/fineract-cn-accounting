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
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;
import java.util.Set;

@SuppressWarnings({"unused"})
public final class JournalEntry {

  @ValidIdentifier
  private String transactionIdentifier;
  @NotNull
  private String transactionDate;
  @ValidIdentifier
  private String transactionType;
  @NotEmpty
  private String clerk;
  private String note;
  @NotNull
  private Set<Debtor> debtors;
  @NotNull
  private Set<Creditor> creditors;
  private State state;
  private String message;

  public JournalEntry() {
    super();
  }

  public String getTransactionIdentifier() {
    return this.transactionIdentifier;
  }

  public void setTransactionIdentifier(final String transactionIdentifier) {
    this.transactionIdentifier = transactionIdentifier;
  }

  public String getTransactionDate() {
    return this.transactionDate;
  }

  public void setTransactionDate(final String transactionDate) {
    this.transactionDate = transactionDate;
  }

  public String getTransactionType() {
    return this.transactionType;
  }

  public void setTransactionType(final String transactionType) {
    this.transactionType = transactionType;
  }

  public String getClerk() {
    return this.clerk;
  }

  public void setClerk(final String clerk) {
    this.clerk = clerk;
  }

  public String getNote() {
    return this.note;
  }

  public void setNote(final String note) {
    this.note = note;
  }

  public Set<Debtor> getDebtors() {
    return this.debtors;
  }

  public void setDebtors(final Set<Debtor> debtors) {
    this.debtors = debtors;
  }

  public Set<Creditor> getCreditors() {
    return this.creditors;
  }

  public void setCreditors(final Set<Creditor> creditors) {
    this.creditors = creditors;
  }

  public String getState() {
    return this.state.name();
  }

  public void setState(final String state) {
    this.state = State.valueOf(state);
  }

  public String getMessage() {
    return this.message;
  }

  public void setMessage(final String message) {
    this.message = message;
  }

  @SuppressWarnings("WeakerAccess")
  public enum State {
    PENDING,
    PROCESSED
  }
}
