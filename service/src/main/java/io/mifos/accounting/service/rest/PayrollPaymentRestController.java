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
import io.mifos.accounting.api.v1.domain.PayrollCollectionHistory;
import io.mifos.accounting.api.v1.domain.PayrollCollectionSheet;
import io.mifos.accounting.api.v1.domain.PayrollPaymentPage;
import io.mifos.accounting.service.ServiceConstants;
import io.mifos.accounting.service.internal.command.ProcessPayrollPaymentCommand;
import io.mifos.accounting.service.internal.service.PayrollPaymentService;
import io.mifos.accounting.service.rest.paging.PageableBuilder;
import io.mifos.anubis.annotation.AcceptedTokenType;
import io.mifos.anubis.annotation.Permittable;
import io.mifos.core.command.gateway.CommandGateway;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
import java.util.List;

@RestController
@RequestMapping("/payroll")
public class PayrollPaymentRestController {

  private final Logger logger;
  private final CommandGateway commandGateway;
  private final PayrollPaymentService payrollPaymentService;

  @Autowired
  public PayrollPaymentRestController(@Qualifier(ServiceConstants.LOGGER_NAME) final Logger logger,
                                      final CommandGateway commandGateway,
                                      final PayrollPaymentService payrollPaymentService) {
    super();
    this.logger = logger;
    this.commandGateway = commandGateway;
    this.payrollPaymentService = payrollPaymentService;
  }

  @Permittable(value = AcceptedTokenType.TENANT, groupId = PermittableGroupIds.THOTH_ACCOUNT)
  @RequestMapping(
      method = RequestMethod.POST,
      produces = MediaType.APPLICATION_JSON_VALUE,
      consumes = MediaType.APPLICATION_JSON_VALUE
  )
  @ResponseBody
  public ResponseEntity<Void> post(@RequestBody @Valid final PayrollCollectionSheet payrollCollectionSheet) {
    this.commandGateway.process(new ProcessPayrollPaymentCommand(payrollCollectionSheet));
    return ResponseEntity.accepted().build();
  }

  @Permittable(value = AcceptedTokenType.TENANT, groupId = PermittableGroupIds.THOTH_ACCOUNT)
  @RequestMapping(
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE,
      consumes = MediaType.ALL_VALUE
  )
  @ResponseBody
  ResponseEntity<List<PayrollCollectionHistory>> getPayrollCollectionHistory() {
    return ResponseEntity.ok(this.payrollPaymentService.fetchPayrollCollectionHistory());
  }

  @Permittable(value = AcceptedTokenType.TENANT, groupId = PermittableGroupIds.THOTH_ACCOUNT)
  @RequestMapping(
      value = "/{identifier}/payments",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE,
      consumes = MediaType.ALL_VALUE
  )
  @ResponseBody
  ResponseEntity<PayrollPaymentPage> getPayrollPaymentHistory(@PathVariable("identifier") final String identifier,
                                                              @RequestParam(value = "pageIndex", required = false) final Integer pageIndex,
                                                              @RequestParam(value = "size", required = false) final Integer size,
                                                              @RequestParam(value = "sortColumn", required = false) final String sortColumn,
                                                              @RequestParam(value = "sortDirection", required = false) final String sortDirection) {
    final Pageable pageable = PageableBuilder.create(
        pageIndex, size, sortColumn != null ? sortColumn : "customerIdentifier", sortDirection
    );

    return ResponseEntity.ok(this.payrollPaymentService.fetchPayrollPayments(identifier, pageable));
  }
}
