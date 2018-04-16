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
import org.apache.fineract.cn.accounting.api.v1.domain.financial.statement.TrialBalance;
import org.apache.fineract.cn.accounting.service.internal.service.TrialBalanceService;
import org.apache.fineract.cn.anubis.annotation.AcceptedTokenType;
import org.apache.fineract.cn.anubis.annotation.Permittable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@SuppressWarnings("unused")
@RestController
@RequestMapping("/trialbalance")
public class TrialBalanceController {

  private final TrialBalanceService trialBalanceService;

  @Autowired
  public TrialBalanceController(final TrialBalanceService trialBalanceService) {
    super();
    this.trialBalanceService = trialBalanceService;
  }

  @Permittable(value = AcceptedTokenType.TENANT, groupId = PermittableGroupIds.THOTH_LEDGER)
  @RequestMapping(
      method = RequestMethod.GET,
      produces = {MediaType.APPLICATION_JSON_VALUE},
      consumes = {MediaType.ALL_VALUE}
  )
  @ResponseBody
  public ResponseEntity<TrialBalance> getTrialBalance(
      @RequestParam(value = "includeEmptyEntries", required = false) final boolean includeEmptyEntries) {
    return ResponseEntity.ok(this.trialBalanceService.getTrialBalance(includeEmptyEntries));
  }
}
