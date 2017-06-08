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
package io.mifos.accounting.service.internal.command.handler;

import io.mifos.accounting.api.v1.EventConstants;
import io.mifos.accounting.api.v1.domain.Ledger;
import io.mifos.accounting.service.ServiceConstants;
import io.mifos.accounting.service.internal.command.AddSubLedgerCommand;
import io.mifos.accounting.service.internal.command.CreateLedgerCommand;
import io.mifos.accounting.service.internal.command.DeleteLedgerCommand;
import io.mifos.accounting.service.internal.command.ModifyLedgerCommand;
import io.mifos.accounting.service.internal.repository.LedgerEntity;
import io.mifos.accounting.service.internal.repository.LedgerRepository;
import io.mifos.core.api.util.UserContextHolder;
import io.mifos.core.command.annotation.Aggregate;
import io.mifos.core.command.annotation.CommandHandler;
import io.mifos.core.command.annotation.CommandLogLevel;
import io.mifos.core.command.annotation.EventEmitter;
import io.mifos.core.lang.ServiceException;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@SuppressWarnings({"unused", "WeakerAccess"})
@Aggregate
public class LedgerCommandHandler {

  private final Logger logger;
  private final LedgerRepository ledgerRepository;

  @Autowired
  public LedgerCommandHandler(@Qualifier(ServiceConstants.LOGGER_NAME) final Logger logger,
                              final LedgerRepository ledgerRepository) {
    super();
    this.logger = logger;
    this.ledgerRepository = ledgerRepository;
  }

  @Transactional
  @CommandHandler(logStart = CommandLogLevel.INFO, logFinish = CommandLogLevel.INFO)
  @EventEmitter(selectorName = EventConstants.SELECTOR_NAME, selectorValue = EventConstants.POST_LEDGER)
  public String createLedger(final CreateLedgerCommand createLedgerCommand) {
    final Ledger ledger = createLedgerCommand.ledger();

    this.logger.debug("Received create ledger command with identifier {}.", ledger.getIdentifier());

    final LedgerEntity parentLedgerEntity = new LedgerEntity();
    parentLedgerEntity.setIdentifier(ledger.getIdentifier());
    parentLedgerEntity.setType(ledger.getType());
    parentLedgerEntity.setName(ledger.getName());
    parentLedgerEntity.setDescription(ledger.getDescription());
    parentLedgerEntity.setCreatedBy(UserContextHolder.checkedGetUser());
    parentLedgerEntity.setCreatedOn(LocalDateTime.now(Clock.systemUTC()));
    parentLedgerEntity.setShowAccountsInChart(ledger.getShowAccountsInChart());
    final LedgerEntity savedParentLedger = this.ledgerRepository.save(parentLedgerEntity);
    this.addSubLedgersInternal(ledger.getSubLedgers(), savedParentLedger);

    this.logger.debug("Ledger {} created.", ledger.getIdentifier());

    return ledger.getIdentifier();
  }

  @Transactional
  @CommandHandler(logStart = CommandLogLevel.INFO, logFinish = CommandLogLevel.INFO)
  @EventEmitter(selectorName = EventConstants.SELECTOR_NAME, selectorValue = EventConstants.POST_LEDGER)
  public String addSubLedger(final AddSubLedgerCommand addSubLedgerCommand) {
    final LedgerEntity parentLedger =
        this.ledgerRepository.findByIdentifier(addSubLedgerCommand.parentLedgerIdentifier());
    final Ledger subLedger = addSubLedgerCommand.subLedger();
    final LedgerEntity subLedgerEntity = this.ledgerRepository.findByIdentifier(subLedger.getIdentifier());
    if (subLedgerEntity == null) {
      this.addSubLedgersInternal(Collections.singletonList(subLedger), parentLedger);
    } else {
      subLedgerEntity.setParentLedger(parentLedger);
      subLedgerEntity.setLastModifiedBy(UserContextHolder.checkedGetUser());
      subLedgerEntity.setLastModifiedOn(LocalDateTime.now(Clock.systemUTC()));
      this.ledgerRepository.save(subLedgerEntity);
    }
    parentLedger.setLastModifiedBy(UserContextHolder.checkedGetUser());
    parentLedger.setLastModifiedOn(LocalDateTime.now(Clock.systemUTC()));
    this.ledgerRepository.save(parentLedger);
    return subLedger.getIdentifier();
  }

  @Transactional
  @CommandHandler(logStart = CommandLogLevel.INFO, logFinish = CommandLogLevel.INFO)
  @EventEmitter(selectorName = EventConstants.SELECTOR_NAME, selectorValue = EventConstants.PUT_LEDGER)
  public String modifyLedger(final ModifyLedgerCommand modifyLedgerCommand) {
    final Ledger ledger2modify = modifyLedgerCommand.ledger();
    final LedgerEntity ledgerEntity =
        this.ledgerRepository.findByIdentifier(ledger2modify.getIdentifier());
    ledgerEntity.setName(ledger2modify.getName());
    ledgerEntity.setDescription(ledger2modify.getDescription());
    ledgerEntity.setLastModifiedBy(UserContextHolder.checkedGetUser());
    ledgerEntity.setLastModifiedOn(LocalDateTime.now(Clock.systemUTC()));
    ledgerEntity.setShowAccountsInChart(ledger2modify.getShowAccountsInChart());
    this.ledgerRepository.save(ledgerEntity);
    return ledger2modify.getIdentifier();
  }

  @Transactional
  @CommandHandler(logStart = CommandLogLevel.INFO, logFinish = CommandLogLevel.INFO)
  @EventEmitter(selectorName = EventConstants.SELECTOR_NAME, selectorValue = EventConstants.DELETE_LEDGER)
  public String deleteLedger(final DeleteLedgerCommand deleteLedgerCommand) {
    this.ledgerRepository.delete(this.ledgerRepository.findByIdentifier(deleteLedgerCommand.identifier()));
    return deleteLedgerCommand.identifier();
  }

  @Transactional
  public void addSubLedgersInternal(final List<Ledger> subLedgers, final LedgerEntity parentLedgerEntity) {
    if (subLedgers != null) {

      this.logger.debug(
          "Add {} sub ledger(s) to parent ledger {}.", subLedgers.size(),
          parentLedgerEntity.getIdentifier()
      );

      for (final Ledger subLedger : subLedgers) {
        if (!subLedger.getType().equals(parentLedgerEntity.getType())) {
          this.logger.error(
              "Type of sub ledger {} must match parent ledger {}. Expected {}, was {}",
              subLedger.getIdentifier(), parentLedgerEntity.getIdentifier(),
              parentLedgerEntity.getType(), subLedger.getType()
          );

          throw ServiceException.badRequest(
              "Type of sub ledger {0} must match parent ledger {1}. Expected {2}, was {3}",
              subLedger.getIdentifier(), parentLedgerEntity.getIdentifier(),
              parentLedgerEntity.getType(), subLedger.getType()
          );
        }
        final LedgerEntity subLedgerEntity = new LedgerEntity();
        subLedgerEntity.setIdentifier(subLedger.getIdentifier());
        subLedgerEntity.setType(subLedger.getType());
        subLedgerEntity.setName(subLedger.getName());
        subLedgerEntity.setDescription(subLedger.getDescription());
        subLedgerEntity.setCreatedBy(UserContextHolder.checkedGetUser());
        subLedgerEntity.setCreatedOn(LocalDateTime.now(Clock.systemUTC()));
        subLedgerEntity.setShowAccountsInChart(subLedger.getShowAccountsInChart());
        subLedgerEntity.setParentLedger(parentLedgerEntity);
        final LedgerEntity savedSubLedger = this.ledgerRepository.save(subLedgerEntity);
        this.addSubLedgersInternal(subLedger.getSubLedgers(), savedSubLedger);

        this.logger.debug("Sub ledger {} created.", subLedger.getIdentifier());
      }
    }
  }
}
