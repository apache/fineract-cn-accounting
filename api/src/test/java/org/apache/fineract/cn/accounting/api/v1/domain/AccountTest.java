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
package org.apache.fineract.cn.accounting.api.v1.domain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.fineract.cn.test.domain.ValidationTest;
import org.apache.fineract.cn.test.domain.ValidationTestCase;
import org.junit.runners.Parameterized;

/**
 * @author Myrle Krantz
 */
public class AccountTest extends ValidationTest<Account> {
  @Parameterized.Parameters
  public static Collection testCases() {
    final Collection<ValidationTestCase> ret = new ArrayList<>();

    ret.add(new ValidationTestCase<Account>("valid")
            .adjustment(x -> {})
            .valid(true));
    ret.add(new ValidationTestCase<Account>("nullBalance")
            .adjustment(x -> x.setBalance(null))
            .valid(false));
    ret.add(new ValidationTestCase<Account>("nameTooLong")
        .adjustment(x -> x.setName(RandomStringUtils.randomAlphanumeric(257)))
        .valid(false));
    ret.add(new ValidationTestCase<Account>("validState")
        .adjustment(x -> x.setState("OPEN"))
        .valid(true));
    ret.add(new ValidationTestCase<Account>("validNullState")
        .adjustment(x -> x.setState(null))
        .valid(true));

    return ret;
  }

  public AccountTest(final ValidationTestCase<Account> testCase) { super(testCase); }

  protected Account createValidTestSubject() {
    final Account ret = new Account();
    ret.setBalance(0d);
    ret.setName(RandomStringUtils.randomAlphanumeric(256));
    ret.setIdentifier("validAccountIdentifier");
    ret.setState(Account.State.OPEN.name());
    ret.setType(AccountType.ASSET.name());
    ret.setHolders(Collections.singleton("freddieMercury"));
    ret.setLedger("validLedgerIdentifier");
    ret.setReferenceAccount("validReferenceAccountIdentifier");
    ret.setSignatureAuthorities(Collections.singleton("shesAKiller"));
    return ret;
  }
}