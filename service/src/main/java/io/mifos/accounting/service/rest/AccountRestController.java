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
package io.mifos.accounting.service.rest;

import io.mifos.accounting.api.v1.PermittableGroupIds;
import io.mifos.accounting.api.v1.client.AccountNotFoundException;
import io.mifos.accounting.api.v1.domain.Account;
import io.mifos.accounting.api.v1.domain.AccountCommand;
import io.mifos.accounting.api.v1.domain.AccountEntryPage;
import io.mifos.accounting.api.v1.domain.AccountPage;
import io.mifos.accounting.api.v1.domain.Ledger;
import io.mifos.accounting.service.helper.DateRangeHelper;
import io.mifos.accounting.service.internal.command.CloseAccountCommand;
import io.mifos.accounting.service.internal.command.CreateAccountCommand;
import io.mifos.accounting.service.internal.command.DeleteAccountCommand;
import io.mifos.accounting.service.internal.command.LockAccountCommand;
import io.mifos.accounting.service.internal.command.ModifyAccountCommand;
import io.mifos.accounting.service.internal.command.ReopenAccountCommand;
import io.mifos.accounting.service.internal.command.UnlockAccountCommand;
import io.mifos.accounting.service.internal.service.AccountService;
import io.mifos.accounting.service.internal.service.LedgerService;
import io.mifos.accounting.service.rest.paging.PageableBuilder;
import io.mifos.anubis.annotation.AcceptedTokenType;
import io.mifos.anubis.annotation.Permittable;
import io.mifos.core.api.annotation.ThrowsException;
import io.mifos.core.command.gateway.CommandGateway;
import io.mifos.core.lang.DateConverter;
import io.mifos.core.lang.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

@SuppressWarnings({"unused"})
@RestController
@RequestMapping("/accounts")
public class AccountRestController {

  private final CommandGateway commandGateway;
  private final AccountService accountService;
  private final LedgerService ledgerService;

  @Autowired
  public AccountRestController(final CommandGateway commandGateway,
                               final AccountService accountService,
                               final LedgerService ledgerService) {
    super();
    this.commandGateway = commandGateway;
    this.accountService = accountService;
    this.ledgerService = ledgerService;
  }

  @Permittable(value = AcceptedTokenType.TENANT, groupId = PermittableGroupIds.THOTH_ACCOUNT)
  @RequestMapping(
      method = RequestMethod.POST,
      produces = {MediaType.APPLICATION_JSON_VALUE},
      consumes = {MediaType.APPLICATION_JSON_VALUE}
  )
  @ResponseBody
  ResponseEntity<Void> createAccount(@RequestBody @Valid final Account account) {
    if (this.accountService.findAccount(account.getIdentifier()).isPresent()) {
      throw ServiceException.conflict("Account {0} already exists.", account.getIdentifier());
    }

    if (account.getReferenceAccount() != null
        && !this.accountService.findAccount(account.getReferenceAccount()).isPresent()) {
      throw ServiceException.badRequest("Reference account {0} not available.",
          account.getReferenceAccount());
    }

    validateLedger(account);

    this.commandGateway.process(new CreateAccountCommand(account));

    return ResponseEntity.accepted().build();
  }

  @Permittable(value = AcceptedTokenType.TENANT, groupId = PermittableGroupIds.THOTH_ACCOUNT)
  @RequestMapping(
      method = RequestMethod.GET,
      produces = {MediaType.APPLICATION_JSON_VALUE},
      consumes = {MediaType.ALL_VALUE}
  )
  @ResponseBody
  ResponseEntity<AccountPage> fetchAccounts(
      @RequestParam(value = "includeClosed", required = false, defaultValue = "false") final boolean includeClosed,
      @RequestParam(value = "term", required = false) final String term,
      @RequestParam(value = "type", required = false) final String type,
      @RequestParam(value = "pageIndex", required = false) final Integer pageIndex,
      @RequestParam(value = "size", required = false) final Integer size,
      @RequestParam(value = "sortColumn", required = false) final String sortColumn,
      @RequestParam(value = "sortDirection", required = false) final String sortDirection
  ) {
    return ResponseEntity.ok(
        this.accountService.fetchAccounts(
            includeClosed, term, type, PageableBuilder.create(pageIndex, size, sortColumn, sortDirection)
        )
    );
  }

  @Permittable(value = AcceptedTokenType.TENANT, groupId = PermittableGroupIds.THOTH_ACCOUNT)
  @RequestMapping(
      value = "/{identifier}",
      method = RequestMethod.GET,
      produces = {MediaType.APPLICATION_JSON_VALUE},
      consumes = {MediaType.ALL_VALUE}
  )
  @ResponseBody
  ResponseEntity<Account> findAccount(@PathVariable("identifier") final String identifier) {
    final Optional<Account> optionalAccount = this.accountService.findAccount(identifier);
    if (optionalAccount.isPresent()) {
      return ResponseEntity.ok(optionalAccount.get());
    } else {
      throw ServiceException.notFound("Account {0} not found.", identifier);
    }
  }

  @Permittable(value = AcceptedTokenType.TENANT, groupId = PermittableGroupIds.THOTH_ACCOUNT)
  @RequestMapping(
      value = "/{identifier}",
      method = RequestMethod.PUT,
      produces = {MediaType.APPLICATION_JSON_VALUE},
      consumes = {MediaType.APPLICATION_JSON_VALUE}
  )
  @ResponseBody
  ResponseEntity<Void> modifyAccount(@PathVariable("identifier") final String identifier,
                                     @RequestBody @Valid final Account account) {
    if (!identifier.equals(account.getIdentifier())) {
      throw ServiceException.badRequest("Addressed resource {0} does not match account {1}",
          identifier, account.getIdentifier());
    }

    if (!this.accountService.findAccount(identifier).isPresent()) {
      throw ServiceException.notFound("Account {0} not found.", identifier);
    }

    if (account.getReferenceAccount() != null
        && !this.accountService.findAccount(account.getReferenceAccount()).isPresent()) {
      throw ServiceException.badRequest("Reference account {0} not available.",
          account.getReferenceAccount());
    }

    validateLedger(account);

    this.commandGateway.process(new ModifyAccountCommand(account));
    return ResponseEntity.accepted().build();
  }

  @Permittable(value = AcceptedTokenType.TENANT, groupId = PermittableGroupIds.THOTH_ACCOUNT)
  @RequestMapping(
      value = "/{identifier}/entries",
      method = RequestMethod.GET,
      produces = {MediaType.APPLICATION_JSON_VALUE},
      consumes = {MediaType.ALL_VALUE}
  )
  @ResponseBody
  ResponseEntity<AccountEntryPage> fetchAccountEntries(
      @PathVariable("identifier") final String identifier,
      @RequestParam(value = "dateRange", required = false) final String dateRange,
      @RequestParam(value = "pageIndex", required = false) final Integer pageIndex,
      @RequestParam(value = "size", required = false) final Integer size
  ) {
    final String[] dates = DateRangeHelper.split(dateRange);

    final LocalDateTime dateFrom = this.parseDateTime(dates[0]);
    final LocalDateTime dateTo = this.parseDateTime(dates[1]);

    return ResponseEntity.ok(this.accountService.fetchAccountEntries(identifier, dateFrom, dateTo, PageableBuilder.create(pageIndex, size, "transactionDate", null)));
  }

  @Permittable(value = AcceptedTokenType.TENANT, groupId = PermittableGroupIds.THOTH_ACCOUNT)
  @RequestMapping(
          value = "/{identifier}/commands",
          method = RequestMethod.GET,
          produces = MediaType.APPLICATION_JSON_VALUE,
          consumes = MediaType.ALL_VALUE
  )
  @ThrowsException(status = HttpStatus.NOT_FOUND, exception = AccountNotFoundException.class)
  ResponseEntity<List<AccountCommand>> fetchAccountCommands(@PathVariable("identifier") final String identifier){
    final Optional<Account> optionalAccount = this.accountService.findAccount(identifier);
    if (optionalAccount.isPresent()) {
      return ResponseEntity.ok(this.accountService.fetchCommandsByAccount(identifier));
    } else {
      throw ServiceException.notFound("Account {0} not found.", identifier);
    }
  }

  @Permittable(value = AcceptedTokenType.TENANT, groupId = PermittableGroupIds.THOTH_ACCOUNT)
  @RequestMapping(
      value = "/{identifier}/commands",
      method = RequestMethod.POST,
      produces = {MediaType.APPLICATION_JSON_VALUE},
      consumes = {MediaType.APPLICATION_JSON_VALUE}
  )
  @ResponseBody
  ResponseEntity<Void> accountCommand(@PathVariable("identifier") final String identifier,
                                      @RequestBody @Valid final AccountCommand accountCommand) {

    switch (AccountCommand.Action.valueOf(accountCommand.getAction())) {
      case CLOSE:
        this.commandGateway.process(new CloseAccountCommand(identifier, accountCommand.getComment()));
        break;
      case LOCK:
        this.commandGateway.process(new LockAccountCommand(identifier, accountCommand.getComment()));
        break;
      case UNLOCK:
        this.commandGateway.process(new UnlockAccountCommand(identifier, accountCommand.getComment()));
        break;
      case REOPEN:
        this.commandGateway.process(new ReopenAccountCommand(identifier, accountCommand.getComment()));
        break;
    }
    return ResponseEntity.accepted().build();
  }

  @Permittable(value = AcceptedTokenType.TENANT, groupId = PermittableGroupIds.THOTH_ACCOUNT)
  @RequestMapping(
      value = "/{identifier}",
      method = RequestMethod.DELETE,
      produces = {MediaType.APPLICATION_JSON_VALUE},
      consumes = {MediaType.ALL_VALUE}
  )
  @ResponseBody
  ResponseEntity<Void> deleteAccount(@PathVariable("identifier") final String identifier) {
    final Optional<Account> optionalAccount = this.accountService.findAccount(identifier);
    final Account account = optionalAccount.orElseThrow(() -> ServiceException.notFound("Account {0} not found", identifier));
    if (!account.getState().equals(Account.State.CLOSED.name())) {
      throw ServiceException.conflict("Account {0} is not closed.", identifier);
    }

    if (this.accountService.hasEntries(identifier)) {
      throw ServiceException.conflict("Account {0} has valid entries.", identifier);
    }

    if (this.accountService.hasReferenceAccounts(identifier)) {
      throw ServiceException.conflict("Account {0} is referenced.", identifier);
    }

    this.commandGateway.process(new DeleteAccountCommand(identifier));

    return ResponseEntity.accepted().build();
  }

  private LocalDateTime parseDateTime(String dateString){
    try{
      return DateConverter.fromIsoString(dateString);
    }catch(DateTimeParseException e){
      throw ServiceException.badRequest("Date {0} must use ISO format",
              dateString);
    }
  }

  private void validateLedger(final @RequestBody @Valid Account account) {
    final Optional<Ledger> optionalLedger = this.ledgerService.findLedger(account.getLedger());
    if (!optionalLedger.isPresent()) {
      throw ServiceException.badRequest("Ledger {0} not available.", account.getLedger());
    } else {
      final Ledger ledger = optionalLedger.get();
      if (!ledger.getType().equals(account.getType())) {
        throw ServiceException.badRequest("Account type {0} must match ledger type {1}.",
            account.getType(), ledger.getIdentifier());
      }
    }
  }

}
