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
import io.mifos.accounting.api.v1.domain.TransactionType;
import io.mifos.accounting.api.v1.domain.TransactionTypePage;
import io.mifos.accounting.service.ServiceConstants;
import io.mifos.accounting.service.internal.command.ChangeTransactionTypeCommand;
import io.mifos.accounting.service.internal.command.CreateTransactionTypeCommand;
import io.mifos.accounting.service.internal.mapper.TransactionTypeMapper;
import io.mifos.accounting.service.internal.service.TransactionTypeService;
import io.mifos.accounting.service.rest.paging.PageableBuilder;
import io.mifos.anubis.annotation.AcceptedTokenType;
import io.mifos.anubis.annotation.Permittable;
import io.mifos.core.command.gateway.CommandGateway;
import io.mifos.core.lang.ServiceException;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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

@RestController
@RequestMapping("/transactiontypes")
public class TransactionTypeRestController {

  private final Logger logger;
  private final CommandGateway commandGateway;
  private final TransactionTypeService transactionTypeService;

  @Autowired
  public TransactionTypeRestController(@Qualifier(ServiceConstants.LOGGER_NAME) final Logger logger,
                                       final CommandGateway commandGateway,
                                       final TransactionTypeService transactionTypeService) {
    super();
    this.logger = logger;
    this.commandGateway = commandGateway;
    this.transactionTypeService = transactionTypeService;
  }

  @Permittable(value = AcceptedTokenType.TENANT, groupId = PermittableGroupIds.THOTH_TX_TYPES)
  @RequestMapping(
      value = "",
      method = RequestMethod.POST,
      produces = {MediaType.APPLICATION_JSON_VALUE},
      consumes = {MediaType.APPLICATION_JSON_VALUE}
  )
  @ResponseBody
  ResponseEntity<Void> createTransactionType(@RequestBody @Valid final TransactionType transactionType) {
    if (this.transactionTypeService.findByIdentifier(transactionType.getCode()).isPresent()) {
      throw ServiceException.conflict("Transaction type '{0}' already exists.", transactionType.getCode());
    }

    this.commandGateway.process(new CreateTransactionTypeCommand(transactionType));
    return ResponseEntity.accepted().build();
  }


  @Permittable(value = AcceptedTokenType.TENANT, groupId = PermittableGroupIds.THOTH_TX_TYPES)
  @RequestMapping(
      value = "",
      method = RequestMethod.GET,
      produces = {MediaType.APPLICATION_JSON_VALUE},
      consumes = {MediaType.ALL_VALUE}
  )
  @ResponseBody
  ResponseEntity<TransactionTypePage> fetchTransactionTypes(@RequestParam(value = "term", required = false) final String term,
                                                            @RequestParam(value = "pageIndex", required = false) final Integer pageIndex,
                                                            @RequestParam(value = "size", required = false) final Integer size,
                                                            @RequestParam(value = "sortColumn", required = false) final String sortColumn,
                                                            @RequestParam(value = "sortDirection", required = false) final String sortDirection) {
    final String column2sort = "code".equalsIgnoreCase(sortColumn) ? "identifier" : sortColumn;
    return ResponseEntity.ok(
        this.transactionTypeService.fetchTransactionTypes(term,
            PageableBuilder.create(pageIndex, size, column2sort, sortDirection)));
  }

  @Permittable(value = AcceptedTokenType.TENANT, groupId = PermittableGroupIds.THOTH_TX_TYPES)
  @RequestMapping(
      value = "/{code}",
      method = RequestMethod.PUT,
      produces = {MediaType.APPLICATION_JSON_VALUE},
      consumes = {MediaType.APPLICATION_JSON_VALUE}
  )
  @ResponseBody
  ResponseEntity<Void> changeTransactionType(@PathVariable("code") final String code,
                                             @RequestBody @Valid final TransactionType transactionType) {
    if (!code.equals(transactionType.getCode())) {
      throw ServiceException.badRequest("Given transaction type {0} must match request path.", code);
    }

    if (!this.transactionTypeService.findByIdentifier(code).isPresent()) {
      throw ServiceException.notFound("Transaction type '{0}' not found.", code);
    }

    this.commandGateway.process(new ChangeTransactionTypeCommand(transactionType));

    return ResponseEntity.accepted().build();
  }

  @Permittable(value = AcceptedTokenType.TENANT, groupId = PermittableGroupIds.THOTH_TX_TYPES)
  @RequestMapping(
      value = "/{code}",
      method = RequestMethod.GET,
      produces = {MediaType.APPLICATION_JSON_VALUE},
      consumes = {MediaType.ALL_VALUE}
  )
  @ResponseBody
  ResponseEntity<TransactionType> findTransactionType(@PathVariable("code") final String code) {
    return ResponseEntity.ok(
        this.transactionTypeService.findByIdentifier(code)
            .orElseThrow(() -> ServiceException.notFound("Transaction type '{0}' not found.", code))
    );
  }
}
