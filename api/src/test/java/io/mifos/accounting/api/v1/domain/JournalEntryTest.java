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
package io.mifos.accounting.api.v1.domain;

import io.mifos.core.lang.DateConverter;
import io.mifos.core.test.domain.ValidationTest;
import io.mifos.core.test.domain.ValidationTestCase;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.runners.Parameterized;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Myrle Krantz
 */
public class JournalEntryTest extends ValidationTest<JournalEntry> {
  @Parameterized.Parameters
  public static Collection testCases() {
    final Collection<ValidationTestCase> ret = new ArrayList<>();

    ret.add(new ValidationTestCase<JournalEntry>("valid"));
    ret.add(new ValidationTestCase<JournalEntry>("invalidCreditor")
            .adjustment(x -> x.getCreditors().add(new Creditor(null, "20.00")))
            .valid(false));
    ret.add(new ValidationTestCase<JournalEntry>("invalidDebtor")
            .adjustment(x -> x.getDebtors().add(new Debtor(null, "20.00")))
            .valid(false));
    ret.add(new ValidationTestCase<JournalEntry>("invalidBalance")
            .adjustment(x -> x.getDebtors().add(new Debtor("marvin", "-120.00")))
            .valid(false));
    ret.add(new ValidationTestCase<JournalEntry>("tooLongTransactionIdentifier")
            .adjustment(x -> x.setTransactionIdentifier(RandomStringUtils.randomAlphanumeric(33)))
            .valid(false));
    ret.add(new ValidationTestCase<JournalEntry>("tooLongTransactionType")
            .adjustment(x -> x.setTransactionType(RandomStringUtils.randomAlphanumeric(33)))
            .valid(false));

    return ret;
  }

  public JournalEntryTest(ValidationTestCase<JournalEntry> testCase) {
    super(testCase);
  }

  @Override
  protected JournalEntry createValidTestSubject() {
    final JournalEntry ret = new JournalEntry();
    final Set<Creditor> creditors = new HashSet<>();
    creditors.add(new Creditor("beevis", "20.00"));
    ret.setCreditors(creditors);
    final Set<Debtor> debtors = new HashSet<>();
    debtors.add(new Debtor("butthead", "20.00"));
    ret.setDebtors(debtors);
    ret.setClerk("Mike");
    ret.setState(JournalEntry.State.PENDING.name());
    ret.setNote("ha ha");
    ret.setTransactionIdentifier("generated");
    ret.setTransactionType("invented");
    ret.setTransactionDate(DateConverter.toIsoString(LocalDateTime.now()));

    return ret;
  }
}
