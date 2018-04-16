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
package org.apache.fineract.cn.accounting.service.rest;

import org.apache.fineract.cn.accounting.api.v1.PermittableGroupIds;
import org.apache.fineract.cn.accounting.api.v1.domain.Account;
import org.apache.fineract.cn.accounting.api.v1.domain.JournalEntry;
import org.apache.fineract.cn.accounting.service.internal.command.CreateJournalEntryCommand;
import org.apache.fineract.cn.accounting.service.internal.service.AccountService;
import org.apache.fineract.cn.accounting.service.internal.service.JournalEntryService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import javax.validation.Valid;
import org.apache.fineract.cn.anubis.annotation.AcceptedTokenType;
import org.apache.fineract.cn.anubis.annotation.Permittable;
import org.apache.fineract.cn.command.gateway.CommandGateway;
import org.apache.fineract.cn.lang.DateRange;
import org.apache.fineract.cn.lang.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@SuppressWarnings({"unused"})
@RestController
@RequestMapping("/journal")
public class JournalRestController {

  private final CommandGateway commandGateway;
  private final JournalEntryService journalEntryService;
  private final AccountService accountService;

  @Autowired
  public JournalRestController(final CommandGateway commandGateway,
                               final JournalEntryService journalEntryService,
                               final AccountService accountService) {
    super();
    this.commandGateway = commandGateway;
    this.journalEntryService = journalEntryService;
    this.accountService = accountService;
  }

  @Permittable(value = AcceptedTokenType.TENANT, groupId = PermittableGroupIds.THOTH_JOURNAL)
  @RequestMapping(
      method = RequestMethod.POST,
      produces = {MediaType.APPLICATION_JSON_VALUE},
      consumes = {MediaType.APPLICATION_JSON_VALUE}
  )
  @ResponseBody
  ResponseEntity<Void> createJournalEntry(@RequestBody @Valid final JournalEntry journalEntry) {
    if (this.journalEntryService.findJournalEntry(journalEntry.getTransactionIdentifier()).isPresent()) {
      throw ServiceException.conflict("Journal entry {0} already exists.", journalEntry.getTransactionIdentifier());
    }

    if (journalEntry.getDebtors().size() == 0) {
      throw ServiceException.badRequest("Debtors must be given.");
    }
    if (journalEntry.getCreditors().size() == 0) {
      throw ServiceException.badRequest("Creditors must be given.");
    }

    final Double debtorAmountSum = journalEntry.getDebtors()
        .stream()
        .peek(debtor -> {
          final Optional<Account> accountOptional = this.accountService.findAccount(debtor.getAccountNumber());
          if (!accountOptional.isPresent()) {
            throw ServiceException.badRequest("Unknown debtor account{0}.", debtor.getAccountNumber());
          }
          if (!accountOptional.get().getState().equals(Account.State.OPEN.name())) {
            throw ServiceException.conflict("Debtor account{0} must be in state open.", debtor.getAccountNumber());
          }
        })
        .map(debtor -> Double.valueOf(debtor.getAmount()))
        .reduce(0.0D, (x, y) -> x + y);

    final Double creditorAmountSum = journalEntry.getCreditors()
        .stream()
        .peek(creditor -> {
          final Optional<Account> accountOptional = this.accountService.findAccount(creditor.getAccountNumber());
          if (!accountOptional.isPresent()) {
            throw ServiceException.badRequest("Unknown creditor account{0}.", creditor.getAccountNumber());
          }
          if (!accountOptional.get().getState().equals(Account.State.OPEN.name())) {
            throw ServiceException.conflict("Creditor account{0} must be in state open.", creditor.getAccountNumber());
          }
        })
        .map(creditor -> Double.valueOf(creditor.getAmount()))
        .reduce(0.0D, (x, y) -> x + y);

    if (!debtorAmountSum.equals(creditorAmountSum)) {
      throw ServiceException.conflict(
          "Sum of debtor and sum of creditor amounts must be equals.");
    }

    this.commandGateway.process(new CreateJournalEntryCommand(journalEntry));
    return ResponseEntity.accepted().build();
  }

  @Permittable(value = AcceptedTokenType.TENANT, groupId = PermittableGroupIds.THOTH_JOURNAL)
  @RequestMapping(
      method = RequestMethod.GET,
      produces = {MediaType.APPLICATION_JSON_VALUE},
      consumes = {MediaType.ALL_VALUE}
  )
  @ResponseBody
  ResponseEntity<List<JournalEntry>> fetchJournalEntries(
      @RequestParam(value = "dateRange", required = false) final String dateRange,
      @RequestParam(value = "account", required = false) final String accountNumber,
      @RequestParam(value = "amount", required = false) final BigDecimal amount
  ) {
    final DateRange range = DateRange.fromIsoString(dateRange);

    return ResponseEntity.ok(this.journalEntryService.fetchJournalEntries(range, accountNumber, amount));
  }

  @Permittable(value = AcceptedTokenType.TENANT, groupId = PermittableGroupIds.THOTH_JOURNAL)
  @RequestMapping(
      value = "/{transactionIdentifier}",
      method = RequestMethod.GET,
      produces = {MediaType.APPLICATION_JSON_VALUE},
      consumes = {MediaType.ALL_VALUE}
  )
  @ResponseBody
  ResponseEntity<JournalEntry> findJournalEntry(
      @PathVariable("transactionIdentifier") final String transactionIdentifier
  ) {
    final Optional<JournalEntry> optionalJournalEntry =
        this.journalEntryService.findJournalEntry(transactionIdentifier);

    if (optionalJournalEntry.isPresent()) {
      return ResponseEntity.ok(optionalJournalEntry.get());
    } else {
      throw ServiceException.notFound("Journal entry {0} not found.", transactionIdentifier);
    }
  }
}
