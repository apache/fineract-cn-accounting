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
package org.apache.fineract.cn.accounting.service.internal.mapper;

import org.apache.fineract.cn.accounting.api.v1.domain.Account;
import org.apache.fineract.cn.accounting.service.internal.repository.AccountEntity;
import java.util.Arrays;
import java.util.HashSet;
import org.apache.commons.lang.StringUtils;
import org.apache.fineract.cn.lang.DateConverter;

public class AccountMapper {

  private AccountMapper() {
    super();
  }

  public static Account map(final AccountEntity accountEntity) {
    final Account account = new Account();
    account.setIdentifier(accountEntity.getIdentifier());
    account.setName(accountEntity.getName());
    account.setType(accountEntity.getType());
    account.setLedger(accountEntity.getLedger().getIdentifier());

    if (accountEntity.getHolders() != null) {
      account.setHolders(
              new HashSet<>(Arrays.asList(StringUtils.split(accountEntity.getHolders(), ",")))
      );
    }

    if (accountEntity.getSignatureAuthorities() != null) {
      account.setSignatureAuthorities(
          new HashSet<>(Arrays.asList(StringUtils.split(accountEntity.getSignatureAuthorities(), ",")))
      );
    }
    if (accountEntity.getReferenceAccount() != null) {
      account.setReferenceAccount(accountEntity.getReferenceAccount().getIdentifier());
    }
    account.setBalance(accountEntity.getBalance());
    account.setAlternativeAccountNumber(accountEntity.getAlternativeAccountNumber());
    account.setCreatedBy(accountEntity.getCreatedBy());
    account.setCreatedOn(DateConverter.toIsoString(accountEntity.getCreatedOn()));
    if (accountEntity.getLastModifiedBy() != null) {
      account.setLastModifiedBy(accountEntity.getLastModifiedBy());
      account.setLastModifiedOn(DateConverter.toIsoString(accountEntity.getLastModifiedOn()));
    }
    account.setState(accountEntity.getState());
    return account;
  }
}
