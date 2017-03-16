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

import io.mifos.anubis.test.v1.TenantApplicationSecurityEnvironmentTestRule;
import io.mifos.core.api.context.AutoUserContext;
import io.mifos.core.test.env.TestEnvironment;
import io.mifos.core.test.fixture.TenantDataStoreContextTestRule;
import io.mifos.core.test.fixture.cassandra.CassandraInitializer;
import io.mifos.core.test.fixture.mariadb.MariaDBInitializer;
import io.mifos.core.test.listener.EnableEventRecording;
import io.mifos.core.test.listener.EventRecorder;
import io.mifos.accounting.api.v1.EventConstants;
import io.mifos.accounting.api.v1.client.AccountingService;
import io.mifos.accounting.api.v1.domain.Account;
import io.mifos.accounting.api.v1.domain.AccountType;
import io.mifos.accounting.api.v1.domain.JournalEntry;
import io.mifos.accounting.api.v1.domain.Ledger;
import io.mifos.accounting.service.AccountingServiceConfiguration;
import io.mifos.accounting.util.AccountGenerator;
import io.mifos.accounting.util.JournalEntryGenerator;
import io.mifos.accounting.util.LedgerGenerator;
import org.junit.*;
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
public class TestJournalEntry {

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
  private AccountingService testSubject;
  @Autowired
  private EventRecorder eventRecorder;

  private AutoUserContext autoUserContext;

  @Before
  public void prepTest() throws Exception {
    this.autoUserContext = this.tenantApplicationSecurityEnvironment.createAutoUserContext(TestJournalEntry.TEST_USER);
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
  public void shouldCreateJournalEntry() throws Exception {
    final Ledger assetLedger = LedgerGenerator.createRandomLedger();
    assetLedger.setType(AccountType.ASSET.name());
    this.testSubject.createLedger(assetLedger);
    this.eventRecorder.wait(EventConstants.POST_LEDGER, assetLedger.getIdentifier());

    final Account debtorAccount = AccountGenerator.createRandomAccount(assetLedger.getIdentifier());
    debtorAccount.setType(AccountType.ASSET.name());
    debtorAccount.setBalance(100.00D);
    this.testSubject.createAccount(debtorAccount);
    this.eventRecorder.wait(EventConstants.POST_ACCOUNT, debtorAccount.getIdentifier());

    final Ledger liabilityLedger = LedgerGenerator.createRandomLedger();
    liabilityLedger.setType(AccountType.LIABILITY.name());
    this.testSubject.createLedger(liabilityLedger);
    this.eventRecorder.wait(EventConstants.POST_LEDGER, liabilityLedger.getIdentifier());

    final Account creditorAccount = AccountGenerator.createRandomAccount(liabilityLedger.getIdentifier());
    creditorAccount.setType(AccountType.LIABILITY.name());
    creditorAccount.setBalance(100.00D);
    this.testSubject.createAccount(creditorAccount);
    this.eventRecorder.wait(EventConstants.POST_ACCOUNT, creditorAccount.getIdentifier());

    final JournalEntry journalEntry = JournalEntryGenerator.createRandomJournalEntry(debtorAccount, "50.00",
        creditorAccount, "50.00");
    this.testSubject.createJournalEntry(journalEntry);
    this.eventRecorder.wait(EventConstants.POST_JOURNAL_ENTRY, journalEntry.getTransactionIdentifier());

    this.eventRecorder.wait(EventConstants.RELEASE_JOURNAL_ENTRY, journalEntry.getTransactionIdentifier());

    final JournalEntry foundJournalEntry = this.testSubject.findJournalEntry(journalEntry.getTransactionIdentifier());
    Assert.assertNotNull(foundJournalEntry);
    Assert.assertEquals(JournalEntry.State.PROCESSED.name(), foundJournalEntry.getState());

    final Account modifiedDebtorAccount = this.testSubject.findAccount(debtorAccount.getIdentifier());
    Assert.assertNotNull(modifiedDebtorAccount);
    Assert.assertEquals(150.0D, modifiedDebtorAccount.getBalance(), 0.0D);

    final Account modifiedCreditorAccount = this.testSubject.findAccount(creditorAccount.getIdentifier());
    Assert.assertNotNull(modifiedCreditorAccount);
    Assert.assertEquals(150.0d, modifiedCreditorAccount.getBalance(), 0.0D);
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
