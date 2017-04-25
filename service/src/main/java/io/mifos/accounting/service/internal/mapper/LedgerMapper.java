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
package io.mifos.accounting.service.internal.mapper;

import io.mifos.accounting.api.v1.domain.Ledger;
import io.mifos.accounting.service.internal.repository.LedgerEntity;
import io.mifos.core.lang.DateConverter;

public class LedgerMapper {

  private LedgerMapper() {
    super();
  }

  public static Ledger map(final LedgerEntity ledgerEntity) {
    final Ledger ledger = new Ledger();
    ledger.setType(ledgerEntity.getType());
    ledger.setIdentifier(ledgerEntity.getIdentifier());
    ledger.setName(ledgerEntity.getName());
    ledger.setDescription(ledgerEntity.getDescription());
    if (ledgerEntity.getParentLedger() != null) {
      ledger.setParentLedgerIdentifier(ledgerEntity.getParentLedger().getIdentifier());
    }
    ledger.setCreatedBy(ledgerEntity.getCreatedBy());
    ledger.setCreatedOn(DateConverter.toIsoString(ledgerEntity.getCreatedOn()));
    if (ledgerEntity.getLastModifiedBy() != null) {
      ledger.setLastModifiedBy(ledgerEntity.getLastModifiedBy());
      ledger.setLastModifiedOn(DateConverter.toIsoString(ledgerEntity.getLastModifiedOn()));
    }
    ledger.setShowAccountsInChart(ledgerEntity.getShowAccountsInChart());
    return ledger;
  }
}
