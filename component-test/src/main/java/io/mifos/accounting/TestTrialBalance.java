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
package io.mifos.accounting;

import io.mifos.accounting.api.v1.EventConstants;
import io.mifos.accounting.api.v1.client.LedgerManager;
import io.mifos.accounting.api.v1.domain.Account;
import io.mifos.accounting.api.v1.domain.AccountType;
import io.mifos.accounting.api.v1.domain.JournalEntry;
import io.mifos.accounting.api.v1.domain.Ledger;
import io.mifos.accounting.api.v1.domain.TrialBalance;
import io.mifos.accounting.service.AccountingServiceConfiguration;
import io.mifos.accounting.util.AccountGenerator;
import io.mifos.accounting.util.JournalEntryGenerator;
import io.mifos.accounting.util.LedgerGenerator;
import io.mifos.anubis.test.v1.TenantApplicationSecurityEnvironmentTestRule;
import io.mifos.core.api.context.AutoUserContext;
import io.mifos.core.test.env.TestEnvironment;
import io.mifos.core.test.fixture.TenantDataStoreContextTestRule;
import io.mifos.core.test.fixture.cassandra.CassandraInitializer;
import io.mifos.core.test.fixture.mariadb.MariaDBInitializer;
import io.mifos.core.test.listener.EnableEventRecording;
import io.mifos.core.test.listener.EventRecorder;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.cloud.netflix.ribbon.RibbonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class TestTrialBalance {

  private static final String APP_NAME = "accounting-v1";
  private static final String TEST_USER = "setna";

  private final static TestEnvironment testEnvironment = new TestEnvironment(APP_NAME);
  private final static CassandraInitializer cassandraInitializer = new CassandraInitializer();
  private final static MariaDBInitializer mariaDBInitializer = new MariaDBInitializer();
  private final static TenantDataStoreContextTestRule tenantDataStoreContext = TenantDataStoreContextTestRule.forRandomTenantName(cassandraInitializer, mariaDBInitializer);

  @ClassRule
  public static TestRule orderClassRules = RuleChain
          .outerRule(testEnvironment)
          .around(cassandraInitializer)
          .around(mariaDBInitializer)
          .around(tenantDataStoreContext);

  @Rule
  public final TenantApplicationSecurityEnvironmentTestRule tenantApplicationSecurityEnvironment
          = new TenantApplicationSecurityEnvironmentTestRule(testEnvironment, this::waitForInitialize);

  @Autowired
  private LedgerManager testSubject;
  @Autowired
  private EventRecorder eventRecorder;

  private AutoUserContext autoUserContext;

  public TestTrialBalance() {
    super();
  }

  @Before
  public void prepTest() throws Exception {
    this.autoUserContext = this.tenantApplicationSecurityEnvironment.createAutoUserContext(TestTrialBalance.TEST_USER);
  }

  @After
  public void cleanTest() throws Exception {
    this.autoUserContext.close();
  }

  public boolean waitForInitialize() {
    try {
      return this.eventRecorder.wait(EventConstants.INITIALIZE, "1");
    } catch (final InterruptedException e) {
      throw new IllegalStateException(e);
    }
  }

  @Test
  public void shouldGenerateTrialBalance() throws Exception {
    final Ledger ledgerOne = LedgerGenerator.createRandomLedger();
    this.testSubject.createLedger(ledgerOne);
    this.eventRecorder.wait(EventConstants.POST_LEDGER, ledgerOne.getIdentifier());

    final Ledger subLedgerOne = LedgerGenerator.createRandomLedger();
    this.testSubject.addSubLedger(ledgerOne.getIdentifier(), subLedgerOne);
    this.eventRecorder.wait(EventConstants.POST_LEDGER, subLedgerOne.getIdentifier());

    final Ledger ledgerTwo = LedgerGenerator.createRandomLedger();
    ledgerTwo.setType(AccountType.LIABILITY.name());
    this.testSubject.createLedger(ledgerTwo);
    this.eventRecorder.wait(EventConstants.POST_LEDGER, ledgerTwo.getIdentifier());

    final Account account4ledgerOne = AccountGenerator.createRandomAccount(ledgerOne.getIdentifier());
    this.testSubject.createAccount(account4ledgerOne);
    this.eventRecorder.wait(EventConstants.POST_ACCOUNT, account4ledgerOne.getIdentifier());

    final Account account4subLedgerOne = AccountGenerator.createRandomAccount(subLedgerOne.getIdentifier());
    this.testSubject.createAccount(account4subLedgerOne);
    this.eventRecorder.wait(EventConstants.POST_ACCOUNT, account4subLedgerOne.getIdentifier());

    final Account account4ledgerTwo = AccountGenerator.createRandomAccount(ledgerTwo.getIdentifier());
    account4ledgerTwo.setType(AccountType.LIABILITY.name());
    this.testSubject.createAccount(account4ledgerTwo);
    this.eventRecorder.wait(EventConstants.POST_ACCOUNT, account4ledgerTwo.getIdentifier());

    final JournalEntry firstBooking =
        JournalEntryGenerator.createRandomJournalEntry(account4ledgerOne, "50.00", account4ledgerTwo, "50.00");
    this.testSubject.createJournalEntry(firstBooking);
    this.eventRecorder.wait(EventConstants.RELEASE_JOURNAL_ENTRY, firstBooking.getTransactionIdentifier());

    final JournalEntry secondBooking =
        JournalEntryGenerator.createRandomJournalEntry(account4subLedgerOne, "50.00", account4ledgerTwo, "50.00");
    this.testSubject.createJournalEntry(secondBooking);
    this.eventRecorder.wait(EventConstants.RELEASE_JOURNAL_ENTRY, secondBooking.getTransactionIdentifier());

    final TrialBalance trialBalance = this.testSubject.getTrialBalance(true);
    Assert.assertNotNull(trialBalance);
    Assert.assertEquals(3, trialBalance.getTrialBalanceEntries().size());
    Assert.assertEquals(100.00D, trialBalance.getDebitTotal(), 0.00D);
    Assert.assertEquals(100.00D, trialBalance.getCreditTotal(), 0.00D);
  }

  @Configuration
  @EnableEventRecording
  @EnableFeignClients(basePackages = {"io.mifos.accounting.api.v1"})
  @RibbonClient(name = APP_NAME)
  @Import({AccountingServiceConfiguration.class})
  @ComponentScan("io.mifos.accounting.listener")
  public static class TestConfiguration {
    public TestConfiguration() {
      super();
    }

    @Bean
    public Logger logger() {
      return LoggerFactory.getLogger("test-logger");
    }
  }
}
