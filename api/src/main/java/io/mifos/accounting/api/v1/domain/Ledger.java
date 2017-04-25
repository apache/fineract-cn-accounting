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

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;

@SuppressWarnings({"unused"})
public final class Ledger {

  @NotNull
  private AccountType type;
  @ValidIdentifier
  private String identifier;
  @NotEmpty
  private String name;
  private String description;
  private String parentLedgerIdentifier;
  @Valid
  private List<Ledger> subLedgers;
  private String createdOn;
  private String createdBy;
  private String lastModifiedOn;
  private String lastModifiedBy;
  @NotNull
  private Boolean showAccountsInChart;

  public Ledger() {
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

  public String getDescription() {
    return this.description;
  }

  public String getParentLedgerIdentifier() {
    return this.parentLedgerIdentifier;
  }

  public void setParentLedgerIdentifier(final String parentLedgerIdentifier) {
    this.parentLedgerIdentifier = parentLedgerIdentifier;
  }

  public void setDescription(final String description) {
    this.description = description;
  }

  public List<Ledger> getSubLedgers() {
    return this.subLedgers;
  }

  public void setSubLedgers(final List<Ledger> subLedgers) {
    this.subLedgers = subLedgers;
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

  public Boolean getShowAccountsInChart() {
    return this.showAccountsInChart;
  }

  public void setShowAccountsInChart(final Boolean showAccountsInChart) {
    this.showAccountsInChart = showAccountsInChart;
  }
}
