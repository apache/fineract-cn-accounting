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
import io.mifos.accounting.api.v1.domain.ChartOfAccountEntry;
import io.mifos.accounting.api.v1.domain.Ledger;
import io.mifos.accounting.service.AccountingServiceConfiguration;
import io.mifos.accounting.util.AccountGenerator;
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

import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class TestChartOfAccounts {

  private static final String APP_NAME = "accounting-v1";
  private static final String TEST_USER = "accountant";

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

  public TestChartOfAccounts() {
    super();
  }

  @Before
  public void prepTest() throws Exception {
    this.autoUserContext = this.tenantApplicationSecurityEnvironment.createAutoUserContext(TestChartOfAccounts.TEST_USER);
  }

  @After
  public void cleanTest() throws Exception {
    this.autoUserContext.close();
  }

  @Test
  public void shouldShowChartOfAccounts() throws Exception {
    final Ledger parentRevenueLedger = LedgerGenerator.createLedger("10000", AccountType.REVENUE);
    this.testSubject.createLedger(parentRevenueLedger);
    this.eventRecorder.wait(EventConstants.POST_LEDGER, parentRevenueLedger.getIdentifier());

    final Ledger interestRevenueLedger = LedgerGenerator.createLedger("11000", AccountType.REVENUE);
    this.testSubject.addSubLedger(parentRevenueLedger.getIdentifier(), interestRevenueLedger);
    this.eventRecorder.wait(EventConstants.POST_LEDGER, interestRevenueLedger.getIdentifier());

    final Account consumerInterestRevenueAccount =
        AccountGenerator.createAccount(interestRevenueLedger.getIdentifier(), "11100", AccountType.REVENUE);
    this.testSubject.createAccount(consumerInterestRevenueAccount);
    this.eventRecorder.wait(EventConstants.POST_ACCOUNT, consumerInterestRevenueAccount.getIdentifier());

    final Ledger feeRevenueLedger = LedgerGenerator.createLedger("12000", AccountType.REVENUE);
    this.testSubject.addSubLedger(parentRevenueLedger.getIdentifier(), feeRevenueLedger);
    this.eventRecorder.wait(EventConstants.POST_LEDGER, feeRevenueLedger.getIdentifier());

    final Ledger specialFeeRevenueLedger = LedgerGenerator.createLedger("12100", AccountType.REVENUE);
    this.testSubject.addSubLedger(feeRevenueLedger.getIdentifier(), specialFeeRevenueLedger);
    this.eventRecorder.wait(EventConstants.POST_LEDGER, specialFeeRevenueLedger.getIdentifier());

    final Ledger parentAssetLedger = LedgerGenerator.createLedger("70000", AccountType.ASSET);
    this.testSubject.createLedger(parentAssetLedger);
    this.eventRecorder.wait(EventConstants.POST_LEDGER, parentAssetLedger.getIdentifier());

    final Ledger consumerLoanAssetLedger = LedgerGenerator.createLedger("73000", AccountType.ASSET);
    consumerLoanAssetLedger.setShowAccountsInChart(Boolean.FALSE);
    this.testSubject.addSubLedger(parentAssetLedger.getIdentifier(), consumerLoanAssetLedger);
    this.eventRecorder.wait(EventConstants.POST_LEDGER, consumerLoanAssetLedger.getIdentifier());

    for (int i = 1; i < 100; i++) {
      final String identifier = Integer.valueOf(73000 + i).toString();
      final Account consumerLoanAccount =
          AccountGenerator.createAccount(consumerLoanAssetLedger.getIdentifier(), identifier, AccountType.ASSET);
      this.testSubject.createAccount(consumerLoanAccount);
      this.eventRecorder.wait(EventConstants.POST_ACCOUNT, identifier);
    }

    final List<ChartOfAccountEntry> chartOfAccounts = this.testSubject.getChartOfAccounts();
    Assert.assertNotNull(chartOfAccounts);
    Assert.assertEquals(7, chartOfAccounts.size());
    Assert.assertEquals(Integer.valueOf(0), chartOfAccounts.get(0).getLevel());
    Assert.assertEquals(Integer.valueOf(1), chartOfAccounts.get(1).getLevel());
    Assert.assertEquals(Integer.valueOf(2), chartOfAccounts.get(2).getLevel());
    Assert.assertEquals(Integer.valueOf(1), chartOfAccounts.get(3).getLevel());
    Assert.assertEquals(Integer.valueOf(2), chartOfAccounts.get(4).getLevel());
    Assert.assertEquals(Integer.valueOf(0), chartOfAccounts.get(5).getLevel());
    Assert.assertEquals(Integer.valueOf(1), chartOfAccounts.get(6).getLevel());
  }

  private boolean waitForInitialize() {
    try {
      return this.eventRecorder.wait(EventConstants.INITIALIZE, "1");
    } catch (final InterruptedException e) {
      throw new IllegalStateException(e);
    }
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
