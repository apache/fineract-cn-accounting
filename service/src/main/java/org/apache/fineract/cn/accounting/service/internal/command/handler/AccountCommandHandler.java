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
package org.apache.fineract.cn.accounting.service.internal.command.handler;

import org.apache.fineract.cn.accounting.api.v1.EventConstants;
import org.apache.fineract.cn.accounting.api.v1.domain.Account;
import org.apache.fineract.cn.accounting.api.v1.domain.AccountCommand;
import org.apache.fineract.cn.accounting.api.v1.domain.AccountEntry;
import org.apache.fineract.cn.accounting.api.v1.domain.AccountType;
import org.apache.fineract.cn.accounting.api.v1.domain.JournalEntry;
import org.apache.fineract.cn.accounting.service.ServiceConstants;
import org.apache.fineract.cn.accounting.service.internal.command.BookJournalEntryCommand;
import org.apache.fineract.cn.accounting.service.internal.command.CloseAccountCommand;
import org.apache.fineract.cn.accounting.service.internal.command.CreateAccountCommand;
import org.apache.fineract.cn.accounting.service.internal.command.DeleteAccountCommand;
import org.apache.fineract.cn.accounting.service.internal.command.LockAccountCommand;
import org.apache.fineract.cn.accounting.service.internal.command.ModifyAccountCommand;
import org.apache.fineract.cn.accounting.service.internal.command.ReleaseJournalEntryCommand;
import org.apache.fineract.cn.accounting.service.internal.command.ReopenAccountCommand;
import org.apache.fineract.cn.accounting.service.internal.command.UnlockAccountCommand;
import org.apache.fineract.cn.accounting.service.internal.repository.AccountEntity;
import org.apache.fineract.cn.accounting.service.internal.repository.AccountEntryEntity;
import org.apache.fineract.cn.accounting.service.internal.repository.AccountEntryRepository;
import org.apache.fineract.cn.accounting.service.internal.repository.AccountRepository;
import org.apache.fineract.cn.accounting.service.internal.repository.CommandEntity;
import org.apache.fineract.cn.accounting.service.internal.repository.CommandRepository;
import org.apache.fineract.cn.accounting.service.internal.repository.JournalEntryEntity;
import org.apache.fineract.cn.accounting.service.internal.repository.JournalEntryRepository;
import org.apache.fineract.cn.accounting.service.internal.repository.LedgerEntity;
import org.apache.fineract.cn.accounting.service.internal.repository.LedgerRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.fineract.cn.api.util.UserContextHolder;
import org.apache.fineract.cn.command.annotation.Aggregate;
import org.apache.fineract.cn.command.annotation.CommandHandler;
import org.apache.fineract.cn.command.annotation.CommandLogLevel;
import org.apache.fineract.cn.command.annotation.EventEmitter;
import org.apache.fineract.cn.command.gateway.CommandGateway;
import org.apache.fineract.cn.lang.ServiceException;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

@SuppressWarnings("unused")
@Aggregate
public class AccountCommandHandler {

  private final Logger logger;
  private final CommandGateway commandGateway;
  private final AccountRepository accountRepository;
  private final AccountEntryRepository accountEntryRepository;
  private final LedgerRepository ledgerRepository;
  private final JournalEntryRepository journalEntryRepository;
  private final CommandRepository commandRepository;

  @Autowired
  public AccountCommandHandler(@Qualifier(ServiceConstants.LOGGER_NAME) final Logger logger,
                               final CommandGateway commandGateway,
                               final AccountRepository accountRepository,
                               final AccountEntryRepository accountEntryRepository,
                               final LedgerRepository ledgerRepository,
                               final JournalEntryRepository journalEntryRepository,
                               final CommandRepository commandRepository) {
    super();
    this.logger = logger;
    this.commandGateway = commandGateway;
    this.accountRepository = accountRepository;
    this.accountEntryRepository = accountEntryRepository;
    this.ledgerRepository = ledgerRepository;
    this.journalEntryRepository = journalEntryRepository;
    this.commandRepository = commandRepository;
  }

  @Transactional
  @CommandHandler(logStart = CommandLogLevel.INFO, logFinish = CommandLogLevel.INFO)
  @EventEmitter(selectorName = EventConstants.SELECTOR_NAME, selectorValue = EventConstants.POST_ACCOUNT)
  public String createAccount(final CreateAccountCommand createAccountCommand) {
    final Account account = createAccountCommand.account();
    final AccountEntity accountEntity = new AccountEntity();
    accountEntity.setIdentifier(account.getIdentifier());
    accountEntity.setName(account.getName());
    accountEntity.setType(account.getType());

    final LedgerEntity ledger = this.ledgerRepository.findByIdentifier(account.getLedger());
    accountEntity.setLedger(ledger);

    AccountEntity referenceAccount = null;
    if (account.getReferenceAccount() != null) {
      referenceAccount = this.accountRepository.findByIdentifier(account.getReferenceAccount());
      if (referenceAccount.getState().equals(Account.State.OPEN.name())) {
        accountEntity.setReferenceAccount(referenceAccount);
      } else {
        throw ServiceException.badRequest("Reference account {0} is not valid.", referenceAccount.getIdentifier());
      }
    }

    if (account.getHolders() != null) {
      accountEntity.setHolders(
              account.getHolders()
                      .stream()
                      .collect(Collectors.joining(","))
      );
    }

    if (account.getSignatureAuthorities() != null) {
      accountEntity.setSignatureAuthorities(
          account.getSignatureAuthorities()
              .stream()
              .collect(Collectors.joining(","))
      );
    }

    accountEntity.setBalance(account.getBalance());
    accountEntity.setState(Account.State.OPEN.name());
    accountEntity.setAlternativeAccountNumber(account.getAlternativeAccountNumber());
    accountEntity.setCreatedBy(UserContextHolder.checkedGetUser());
    accountEntity.setCreatedOn(LocalDateTime.now(Clock.systemUTC()));

    final AccountEntity savedAccountEntity = this.accountRepository.save(accountEntity);

    if (referenceAccount != null) {
      referenceAccount.setLastModifiedBy(UserContextHolder.checkedGetUser());
      referenceAccount.setLastModifiedOn(LocalDateTime.now(Clock.systemUTC()));
      this.accountRepository.save(referenceAccount);
    }

    this.ledgerRepository.save(ledger);

    if (savedAccountEntity.getBalance() != null && savedAccountEntity.getBalance() != 0.00D) {
      this.adjustLedgerTotals(
          savedAccountEntity.getLedger().getIdentifier(), BigDecimal.valueOf(savedAccountEntity.getBalance()));
    }

    return account.getIdentifier();
  }

  @Transactional
  @CommandHandler(logStart = CommandLogLevel.INFO, logFinish = CommandLogLevel.INFO)
  @EventEmitter(selectorName = EventConstants.SELECTOR_NAME, selectorValue = EventConstants.PUT_ACCOUNT)
  public String modifyAccount(final ModifyAccountCommand modifyAccountCommand) {
    final Account account = modifyAccountCommand.account();
    final AccountEntity accountEntity = this.accountRepository.findByIdentifier(account.getIdentifier());

    if (account.getName() != null) {
      accountEntity.setName(account.getName());
    }

    LedgerEntity ledger = null;
    if (!account.getLedger().equals(accountEntity.getLedger().getIdentifier())) {
      ledger = this.ledgerRepository.findByIdentifier(account.getLedger());
      accountEntity.setLedger(ledger);
    }

    AccountEntity referenceAccount = null;
    if (account.getReferenceAccount() != null) {
      if (!account.getReferenceAccount().equals(accountEntity.getReferenceAccount().getIdentifier())) {
        referenceAccount = this.accountRepository.findByIdentifier(account.getReferenceAccount());
        accountEntity.setReferenceAccount(referenceAccount);
      }
    } else {
      accountEntity.setReferenceAccount(null);
    }

    if (account.getHolders() != null) {
      accountEntity.setHolders(
              account.getHolders()
                      .stream()
                      .collect(Collectors.joining(","))
      );
    } else {
      accountEntity.setHolders(null);
    }

    if (account.getSignatureAuthorities() != null) {
      accountEntity.setSignatureAuthorities(
          account.getSignatureAuthorities()
              .stream()
              .collect(Collectors.joining(","))
      );
    } else {
      accountEntity.setSignatureAuthorities(null);
    }

    accountEntity.setLastModifiedBy(UserContextHolder.checkedGetUser());
    accountEntity.setLastModifiedOn(LocalDateTime.now(Clock.systemUTC()));

    this.accountRepository.save(accountEntity);

    if (referenceAccount != null) {
      referenceAccount.setLastModifiedBy(UserContextHolder.checkedGetUser());
      referenceAccount.setLastModifiedOn(LocalDateTime.now(Clock.systemUTC()));
      this.accountRepository.save(referenceAccount);
    }

    if (ledger != null) {
      this.ledgerRepository.save(ledger);
    }

    return account.getIdentifier();
  }

  @Transactional
  @CommandHandler(logStart = CommandLogLevel.INFO, logFinish = CommandLogLevel.INFO)
  @EventEmitter(selectorName = EventConstants.SELECTOR_NAME, selectorValue = EventConstants.CLOSE_ACCOUNT)
  public String closeAccount(final CloseAccountCommand closeAccountCommand) {
    final String modifyingUser = SecurityContextHolder.getContext().getAuthentication().getName();
    final LocalDateTime now = LocalDateTime.now(Clock.systemUTC());
    final String identifier = closeAccountCommand.identifier();

    final AccountEntity accountEntity = this.accountRepository.findByIdentifier(identifier);
    accountEntity.setState(Account.State.CLOSED.name());
    accountEntity.setLastModifiedBy(modifyingUser);
    accountEntity.setLastModifiedOn(now);
    this.accountRepository.save(accountEntity);

    final CommandEntity commandEntity = new CommandEntity();
    commandEntity.setType(AccountCommand.Action.CLOSE.name());
    commandEntity.setAccount(accountEntity);
    commandEntity.setComment(closeAccountCommand.comment());
    commandEntity.setCreatedBy(modifyingUser);
    commandEntity.setCreatedOn(now);
    this.commandRepository.save(commandEntity);

    return identifier;
  }

  @Transactional
  @CommandHandler(logStart = CommandLogLevel.INFO, logFinish = CommandLogLevel.INFO)
  @EventEmitter(selectorName = EventConstants.SELECTOR_NAME, selectorValue = EventConstants.LOCK_ACCOUNT)
  public String lockAccount(final LockAccountCommand lockAccountCommand) {
    final String modifyingUser = SecurityContextHolder.getContext().getAuthentication().getName();
    final LocalDateTime now = LocalDateTime.now(Clock.systemUTC());
    final String identifier = lockAccountCommand.identifier();

    final AccountEntity accountEntity = this.accountRepository.findByIdentifier(identifier);
    accountEntity.setState(Account.State.LOCKED.name());
    accountEntity.setLastModifiedBy(modifyingUser);
    accountEntity.setLastModifiedOn(now);
    this.accountRepository.save(accountEntity);

    final CommandEntity commandEntity = new CommandEntity();
    commandEntity.setType(AccountCommand.Action.LOCK.name());
    commandEntity.setAccount(accountEntity);
    commandEntity.setComment(lockAccountCommand.comment());
    commandEntity.setCreatedBy(modifyingUser);
    commandEntity.setCreatedOn(now);
    this.commandRepository.save(commandEntity);

    return identifier;
  }

  @Transactional
  @CommandHandler(logStart = CommandLogLevel.INFO, logFinish = CommandLogLevel.INFO)
  @EventEmitter(selectorName = EventConstants.SELECTOR_NAME, selectorValue = EventConstants.UNLOCK_ACCOUNT)
  public String unlockAccount(final UnlockAccountCommand unlockAccountCommand) {
    final String modifyingUser = SecurityContextHolder.getContext().getAuthentication().getName();
    final LocalDateTime now = LocalDateTime.now(Clock.systemUTC());
    final String identifier = unlockAccountCommand.identifier();

    final AccountEntity accountEntity = this.accountRepository.findByIdentifier(identifier);
    accountEntity.setState(Account.State.OPEN.name());
    accountEntity.setLastModifiedBy(modifyingUser);
    accountEntity.setLastModifiedOn(now);
    this.accountRepository.save(accountEntity);

    final CommandEntity commandEntity = new CommandEntity();
    commandEntity.setType(AccountCommand.Action.UNLOCK.name());
    commandEntity.setAccount(accountEntity);
    commandEntity.setComment(unlockAccountCommand.comment());
    commandEntity.setCreatedBy(modifyingUser);
    commandEntity.setCreatedOn(now);
    this.commandRepository.save(commandEntity);

    return identifier;
  }

  @Transactional
  @CommandHandler(logStart = CommandLogLevel.INFO, logFinish = CommandLogLevel.INFO)
  @EventEmitter(selectorName = EventConstants.SELECTOR_NAME, selectorValue = EventConstants.REOPEN_ACCOUNT)
  public String reopenAccount(final ReopenAccountCommand reopenAccountCommand) {
    final String modifyingUser = SecurityContextHolder.getContext().getAuthentication().getName();
    final LocalDateTime now = LocalDateTime.now(Clock.systemUTC());
    final String identifier = reopenAccountCommand.identifier();

    final AccountEntity accountEntity = this.accountRepository.findByIdentifier(identifier);
    accountEntity.setState(Account.State.OPEN.name());
    accountEntity.setLastModifiedBy(modifyingUser);
    accountEntity.setLastModifiedOn(now);
    this.accountRepository.save(accountEntity);

    final CommandEntity commandEntity = new CommandEntity();
    commandEntity.setType(AccountCommand.Action.REOPEN.name());
    commandEntity.setAccount(accountEntity);
    commandEntity.setComment(reopenAccountCommand.comment());
    commandEntity.setCreatedBy(modifyingUser);
    commandEntity.setCreatedOn(now);
    this.commandRepository.save(commandEntity);

    return identifier;
  }

  @Transactional
  @CommandHandler(logStart = CommandLogLevel.NONE, logFinish = CommandLogLevel.NONE)
  @EventEmitter(selectorName = EventConstants.SELECTOR_NAME, selectorValue = EventConstants.RELEASE_JOURNAL_ENTRY)
  public String bookJournalEntry(final BookJournalEntryCommand bookJournalEntryCommand) {
    final String transactionIdentifier = bookJournalEntryCommand.transactionIdentifier();

    final Optional<JournalEntryEntity> optionalJournalEntry = this.journalEntryRepository.findJournalEntry(transactionIdentifier);
    if (optionalJournalEntry.isPresent()) {
      final JournalEntryEntity journalEntryEntity = optionalJournalEntry.get();
      if (!journalEntryEntity.getState().equals(JournalEntry.State.PENDING.name())) {
        return null;
      }
      // process all debtors
      journalEntryEntity.getDebtors()
          .forEach(debtor -> {
            final String accountNumber = debtor.getAccountNumber();
            final AccountEntity accountEntity = this.accountRepository.findByIdentifier(accountNumber);
            final AccountType accountType = AccountType.valueOf(accountEntity.getType());
            final BigDecimal amount;
            switch (accountType) {
              case ASSET:
              case EXPENSE:
                accountEntity.setBalance(accountEntity.getBalance() + debtor.getAmount());
                amount = BigDecimal.valueOf(debtor.getAmount());
                break;
              case LIABILITY:
              case EQUITY:
              case REVENUE:
                accountEntity.setBalance(accountEntity.getBalance() - debtor.getAmount());
                amount = BigDecimal.valueOf(debtor.getAmount()).negate();
                break;
              default:
                amount = BigDecimal.ZERO;
            }
            final AccountEntity savedAccountEntity = this.accountRepository.save(accountEntity);
            final AccountEntryEntity accountEntryEntity = new AccountEntryEntity();
            accountEntryEntity.setType(AccountEntry.Type.DEBIT.name());
            accountEntryEntity.setAccount(savedAccountEntity);
            accountEntryEntity.setBalance(savedAccountEntity.getBalance());
            accountEntryEntity.setAmount(debtor.getAmount());
            accountEntryEntity.setMessage(journalEntryEntity.getMessage());
            accountEntryEntity.setTransactionDate(journalEntryEntity.getTransactionDate());
            this.accountEntryRepository.save(accountEntryEntity);
            this.adjustLedgerTotals(savedAccountEntity.getLedger().getIdentifier(), amount);
          });
      // process all creditors
      journalEntryEntity.getCreditors()
          .forEach(creditor -> {
            final String accountNumber = creditor.getAccountNumber();
            final AccountEntity accountEntity = this.accountRepository.findByIdentifier(accountNumber);
            final AccountType accountType = AccountType.valueOf(accountEntity.getType());
            final BigDecimal amount;
            switch (accountType) {
              case ASSET:
              case EXPENSE:
                accountEntity.setBalance(accountEntity.getBalance() - creditor.getAmount());
                amount = BigDecimal.valueOf(creditor.getAmount()).negate();
                break;
              case LIABILITY:
              case EQUITY:
              case REVENUE:
                accountEntity.setBalance(accountEntity.getBalance() + creditor.getAmount());
                amount = BigDecimal.valueOf(creditor.getAmount());
                break;
              default:
                amount = BigDecimal.ZERO;
            }
            final AccountEntity savedAccountEntity = this.accountRepository.save(accountEntity);
            final AccountEntryEntity accountEntryEntity = new AccountEntryEntity();
            accountEntryEntity.setType(AccountEntry.Type.CREDIT.name());
            accountEntryEntity.setAccount(savedAccountEntity);
            accountEntryEntity.setBalance(savedAccountEntity.getBalance());
            accountEntryEntity.setAmount(creditor.getAmount());
            accountEntryEntity.setMessage(journalEntryEntity.getMessage());
            accountEntryEntity.setTransactionDate(journalEntryEntity.getTransactionDate());
            this.accountEntryRepository.save(accountEntryEntity);
            this.adjustLedgerTotals(savedAccountEntity.getLedger().getIdentifier(), amount);
          });
      this.commandGateway.process(new ReleaseJournalEntryCommand(transactionIdentifier));
      return transactionIdentifier;
    } else {
      return null;
    }
  }

  @Transactional
  @CommandHandler(logStart = CommandLogLevel.INFO, logFinish = CommandLogLevel.INFO)
  @EventEmitter(selectorName = EventConstants.SELECTOR_NAME, selectorValue = EventConstants.DELETE_ACCOUNT)
  public String deleteAccount(final DeleteAccountCommand deleteAccountCommand) {
    final String accountIdentifier = deleteAccountCommand.identifier();
    final AccountEntity accountEntity = this.accountRepository.findByIdentifier(accountIdentifier);

    final List<CommandEntity> commandEntities = this.commandRepository.findByAccount(accountEntity);
    this.commandRepository.delete(commandEntities);

    this.accountRepository.delete(accountEntity);
    return accountIdentifier;
  }

  @Transactional
  public void adjustLedgerTotals(final String ledgerIdentifier, final BigDecimal amount) {
    final LedgerEntity ledger = this.ledgerRepository.findByIdentifier(ledgerIdentifier);
    final BigDecimal currentTotal = ledger.getTotalValue() != null ? ledger.getTotalValue() : BigDecimal.ZERO;
    ledger.setTotalValue(currentTotal.add(amount));
    final LedgerEntity savedLedger = this.ledgerRepository.save(ledger);
    if (savedLedger.getParentLedger() != null) {
      this.adjustLedgerTotals(savedLedger.getParentLedger().getIdentifier(), amount);
    }
  }
}
