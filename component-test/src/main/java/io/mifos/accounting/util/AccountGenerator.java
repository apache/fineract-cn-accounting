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
package io.mifos.accounting.util;

import io.mifos.accounting.api.v1.domain.Account;
import io.mifos.accounting.api.v1.domain.AccountType;
import org.apache.commons.lang3.RandomStringUtils;

import java.util.Collections;
import java.util.HashSet;

public class AccountGenerator {

  private AccountGenerator() {
    super();
  }

  public static Account createRandomAccount(final String ledgerIdentifier) {
    final Account account = new Account();
    account.setIdentifier(RandomStringUtils.randomAlphanumeric(32));
    account.setName(RandomStringUtils.randomAlphanumeric(256));
    account.setType(AccountType.ASSET.name());
    account.setLedger(ledgerIdentifier);
    account.setHolders(new HashSet<>(Collections.singletonList(RandomStringUtils.randomAlphanumeric(32))));
    account.setSignatureAuthorities(new HashSet<>(Collections.singletonList(RandomStringUtils.randomAlphanumeric(32))));
    account.setBalance(0.00D);
    return account;
  }

  public static Account createAccount(final String ledgerIdentifier, final String accountIdentifier, final AccountType accountType) {
    final Account account = new Account();
    account.setIdentifier(accountIdentifier);
    account.setName(RandomStringUtils.randomAlphanumeric(256));
    account.setType(accountType.name());
    account.setLedger(ledgerIdentifier);
    account.setHolders(new HashSet<>(Collections.singletonList(RandomStringUtils.randomAlphanumeric(32))));
    account.setSignatureAuthorities(new HashSet<>(Collections.singletonList(RandomStringUtils.randomAlphanumeric(32))));
    account.setBalance(0.00D);
    return account;
  }
}
