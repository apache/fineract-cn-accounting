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
package io.mifos.accounting.service.internal.service;

import io.mifos.accounting.api.v1.domain.Account;
import io.mifos.accounting.api.v1.domain.AccountPage;
import io.mifos.accounting.api.v1.domain.Ledger;
import io.mifos.accounting.api.v1.domain.LedgerPage;
import io.mifos.accounting.service.ServiceConstants;
import io.mifos.accounting.service.internal.mapper.AccountMapper;
import io.mifos.accounting.service.internal.mapper.LedgerMapper;
import io.mifos.accounting.service.internal.repository.AccountEntity;
import io.mifos.accounting.service.internal.repository.AccountRepository;
import io.mifos.accounting.service.internal.repository.LedgerEntity;
import io.mifos.accounting.service.internal.repository.LedgerRepository;
import io.mifos.accounting.service.internal.repository.specification.LedgerSpecification;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class LedgerService {

  private final Logger logger;
  private final LedgerRepository ledgerRepository;
  private final AccountRepository accountRepository;

  @Autowired
  public LedgerService(@Qualifier(ServiceConstants.LOGGER_NAME) final Logger logger,
                       final LedgerRepository ledgerRepository,
                       final AccountRepository accountRepository) {
    super();
    this.logger = logger;
    this.ledgerRepository = ledgerRepository;
    this.accountRepository = accountRepository;
  }

  public LedgerPage fetchLedgers(final boolean includeSubLedgers,
                                 final String term,
                                 final String type,
                                 final Pageable pageable) {
    final LedgerPage ledgerPage = new LedgerPage();

    final Page<LedgerEntity> ledgerEntities = this.ledgerRepository.findAll(
        LedgerSpecification.createSpecification(includeSubLedgers, term, type), pageable
    );

    ledgerPage.setTotalPages(ledgerEntities.getTotalPages());
    ledgerPage.setTotalElements(ledgerEntities.getTotalElements());

    ledgerPage.setLedgers(this.mapToLedger(ledgerEntities.getContent()));

    return ledgerPage;
  }

  private List<Ledger> mapToLedger(List<LedgerEntity> ledgerEntities) {
    final List<Ledger> result = new ArrayList<>(ledgerEntities.size());

    if(!ledgerEntities.isEmpty()) {
      ledgerEntities.forEach(ledgerEntity -> {
        final Ledger ledger = LedgerMapper.map(ledgerEntity);
        this.addSubLedgers(ledger, this.ledgerRepository.findByParentLedgerOrderByIdentifier(ledgerEntity));
        result.add(ledger);
      });
    }

    return result;
  }

  public Optional<Ledger> findLedger(final String identifier) {
    final LedgerEntity ledgerEntity = this.ledgerRepository.findByIdentifier(identifier);
    if (ledgerEntity != null) {
      final Ledger ledger = LedgerMapper.map(ledgerEntity);
      this.addSubLedgers(ledger, this.ledgerRepository.findByParentLedgerOrderByIdentifier(ledgerEntity));
      return Optional.of(ledger);
    } else {
      return Optional.empty();
    }
  }

  public AccountPage fetchAccounts(final String ledgerIdentifier, final Pageable pageable) {
    final LedgerEntity ledgerEntity = this.ledgerRepository.findByIdentifier(ledgerIdentifier);
    final Page<AccountEntity> accountEntities = this.accountRepository.findByLedger(ledgerEntity, pageable);

    final AccountPage accountPage = new AccountPage();
    accountPage.setTotalPages(accountEntities.getTotalPages());
    accountPage.setTotalElements(accountEntities.getTotalElements());

    if(accountEntities.getSize() > 0){
      final List<Account> accounts = new ArrayList<>(accountEntities.getSize());
      accountEntities.forEach(accountEntity -> accounts.add(AccountMapper.map(accountEntity)));
      accountPage.setAccounts(accounts);
    }

    return accountPage;
  }

  public boolean hasAccounts(final String ledgerIdentifier) {
    final LedgerEntity ledgerEntity = this.ledgerRepository.findByIdentifier(ledgerIdentifier);
    final List<AccountEntity> ledgerAccounts = this.accountRepository.findByLedger(ledgerEntity);
    return ledgerAccounts.size() > 0;
  }

  private void addSubLedgers(final Ledger parentLedger,
                             final List<LedgerEntity> subLedgerEntities) {
    if (subLedgerEntities != null) {
      final List<Ledger> subLedgers = new ArrayList<>(subLedgerEntities.size());
      subLedgerEntities.forEach(subLedgerEntity -> subLedgers.add(LedgerMapper.map(subLedgerEntity)));
      parentLedger.setSubLedgers(subLedgers);
    }
  }
}
