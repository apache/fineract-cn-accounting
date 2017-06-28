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
import io.mifos.accounting.api.v1.domain.AccountPage;
import io.mifos.accounting.api.v1.domain.Ledger;
import io.mifos.accounting.api.v1.domain.LedgerPage;
import io.mifos.accounting.service.internal.command.AddSubLedgerCommand;
import io.mifos.accounting.service.internal.command.CreateLedgerCommand;
import io.mifos.accounting.service.internal.command.DeleteLedgerCommand;
import io.mifos.accounting.service.internal.command.ModifyLedgerCommand;
import io.mifos.accounting.service.internal.service.LedgerService;
import io.mifos.accounting.service.rest.paging.PageableBuilder;
import io.mifos.anubis.annotation.AcceptedTokenType;
import io.mifos.anubis.annotation.Permittable;
import io.mifos.core.command.gateway.CommandGateway;
import io.mifos.core.lang.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
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
import java.util.Optional;

@SuppressWarnings({"unused"})
@RestController
@RequestMapping("/ledgers")
public class LedgerRestController {

  private final CommandGateway commandGateway;
  private final LedgerService ledgerService;

  @Autowired
  public LedgerRestController(final CommandGateway commandGateway,
                              final LedgerService ledgerService) {
    super();
    this.commandGateway = commandGateway;
    this.ledgerService = ledgerService;
  }

  @Permittable(value = AcceptedTokenType.TENANT, groupId = PermittableGroupIds.THOTH_LEDGER)
  @RequestMapping(
      method = RequestMethod.POST,
      produces = {MediaType.APPLICATION_JSON_VALUE},
      consumes = {MediaType.APPLICATION_JSON_VALUE}
  )
  @ResponseBody
  ResponseEntity<Void> createLedger(@RequestBody @Valid final Ledger ledger) {
    if (ledger.getParentLedgerIdentifier() != null) {
      throw ServiceException.badRequest("Ledger {0} is not a root.", ledger.getIdentifier());
    }

    if (this.ledgerService.findLedger(ledger.getIdentifier()).isPresent()) {
      throw ServiceException.conflict("Ledger {0} already exists.", ledger.getIdentifier());
    }

    this.commandGateway.process(new CreateLedgerCommand(ledger));
    return ResponseEntity.accepted().build();
  }

  @Permittable(value = AcceptedTokenType.TENANT, groupId = PermittableGroupIds.THOTH_LEDGER)
  @RequestMapping(
      method = RequestMethod.GET,
      produces = {MediaType.APPLICATION_JSON_VALUE},
      consumes = {MediaType.ALL_VALUE}
  )
  @ResponseBody
  ResponseEntity<LedgerPage> fetchLedgers(@RequestParam(value = "includeSubLedgers", required = false, defaultValue = "false") final boolean includeSubLedgers,
                                          @RequestParam(value = "term", required = false) final String term,
                                          @RequestParam(value = "type", required = false) final String type,
                                          @RequestParam(value = "pageIndex", required = false) final Integer pageIndex,
                                          @RequestParam(value = "size", required = false) final Integer size,
                                          @RequestParam(value = "sortColumn", required = false) final String sortColumn,
                                          @RequestParam(value = "sortDirection", required = false) final String sortDirection) {

    return ResponseEntity.ok(
        this.ledgerService.fetchLedgers(
            includeSubLedgers, term, type, PageableBuilder.create(pageIndex, size, sortColumn, sortDirection)
        )
    );
  }

  @Permittable(value = AcceptedTokenType.TENANT, groupId = PermittableGroupIds.THOTH_LEDGER)
  @RequestMapping(
      value = "/{identifier}",
      method = RequestMethod.GET,
      produces = {MediaType.APPLICATION_JSON_VALUE},
      consumes = {MediaType.ALL_VALUE}
  )
  @ResponseBody
  ResponseEntity<Ledger> findLedger(@PathVariable("identifier") final String identifier) {
    final Optional<Ledger> optionalLedger = this.ledgerService.findLedger(identifier);
    if (optionalLedger.isPresent()) {
      return ResponseEntity.ok(optionalLedger.get());
    } else {
      throw ServiceException.notFound("Ledger {0} not found.", identifier);
    }
  }

  @Permittable(value = AcceptedTokenType.TENANT, groupId = PermittableGroupIds.THOTH_LEDGER)
  @RequestMapping(
      value = "/{identifier}",
      method = RequestMethod.POST,
      produces = {MediaType.APPLICATION_JSON_VALUE},
      consumes = {MediaType.APPLICATION_JSON_VALUE}
  )
  @ResponseBody
  ResponseEntity<Void> addSubLedger(@PathVariable("identifier") final String identifier,
                                    @RequestBody @Valid final Ledger subLedger) {
    final Optional<Ledger> optionalParentLedger = this.ledgerService.findLedger(identifier);
    if (optionalParentLedger.isPresent()) {
      final Ledger parentLedger = optionalParentLedger.get();
      if (!parentLedger.getType().equals(subLedger.getType())) {
        throw ServiceException.badRequest("Ledger type must be the same.");
      }
    } else {
      throw ServiceException.notFound("Parent ledger {0} not found.", identifier);
    }

    if (this.ledgerService.findLedger(subLedger.getIdentifier()).isPresent()) {
      throw ServiceException.conflict("Ledger {0} already exists.", subLedger.getIdentifier());
    }

    this.commandGateway.process(new AddSubLedgerCommand(identifier, subLedger));

    return ResponseEntity.accepted().build();
  }

  @Permittable(value = AcceptedTokenType.TENANT, groupId = PermittableGroupIds.THOTH_LEDGER)
  @RequestMapping(
      value = "/{identifier}",
      method = RequestMethod.PUT,
      produces = {MediaType.APPLICATION_JSON_VALUE},
      consumes = {MediaType.APPLICATION_JSON_VALUE}
  )
  @ResponseBody
  ResponseEntity<Void> modifyLedger(@PathVariable("identifier") final String identifier,
                                    @RequestBody @Valid final Ledger ledger) {
    if (!identifier.equals(ledger.getIdentifier())) {
      throw ServiceException.badRequest("Addressed resource {0} does not match ledger {1}",
          identifier, ledger.getIdentifier());
    }

    if (!this.ledgerService.findLedger(identifier).isPresent()) {
      throw ServiceException.notFound("Ledger {0} not found.", identifier);
    }

    this.commandGateway.process(new ModifyLedgerCommand(ledger));

    return ResponseEntity.accepted().build();
  }

  @Permittable(value = AcceptedTokenType.TENANT, groupId = PermittableGroupIds.THOTH_LEDGER)
  @RequestMapping(
      value = "/{identifier}",
      method = RequestMethod.DELETE,
      produces = {MediaType.APPLICATION_JSON_VALUE},
      consumes = {MediaType.ALL_VALUE}
  )
  @ResponseBody
  ResponseEntity<Void> deleteLedger(@PathVariable("identifier") final String identifier) {
    final Optional<Ledger> optionalLedger = this.ledgerService.findLedger(identifier);
    if (optionalLedger.isPresent()) {
      final Ledger ledger = optionalLedger.get();
      if (!ledger.getSubLedgers().isEmpty()) {
        throw ServiceException.conflict("Ledger {0} holds sub ledgers.", identifier);
      }
    } else {
      throw ServiceException.notFound("Ledger {0} not found.", identifier);
    }

    if (this.ledgerService.hasAccounts(identifier)) {
      throw ServiceException.conflict("Ledger {0} has assigned accounts.", identifier);
    }

    this.commandGateway.process(new DeleteLedgerCommand(identifier));

    return ResponseEntity.accepted().build();
  }

  @Permittable(value = AcceptedTokenType.TENANT, groupId = PermittableGroupIds.THOTH_LEDGER)
  @RequestMapping(
      value = "/{identifier}/accounts",
      method = RequestMethod.GET,
      produces = {MediaType.APPLICATION_JSON_VALUE},
      consumes = {MediaType.ALL_VALUE}
  )
  @ResponseBody
  ResponseEntity<AccountPage> fetchAccountsOfLedger(@PathVariable("identifier") final String identifier,
                                                    @RequestParam(value = "pageIndex", required = false) final Integer pageIndex,
                                                    @RequestParam(value = "size", required = false) final Integer size,
                                                    @RequestParam(value = "sortColumn", required = false) final String sortColumn,
                                                    @RequestParam(value = "sortDirection", required = false) final String sortDirection) {
    if (!this.ledgerService.findLedger(identifier).isPresent()) {
      throw ServiceException.notFound("Ledger {0} not found.", identifier);
    }
    return ResponseEntity.ok(this.ledgerService.fetchAccounts(identifier, PageableBuilder.create(pageIndex, size, sortColumn, sortDirection)));
  }

}
