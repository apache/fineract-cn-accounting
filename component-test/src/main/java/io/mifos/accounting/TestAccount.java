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
import io.mifos.accounting.api.v1.client.AccountAlreadyExistsException;
import io.mifos.accounting.api.v1.client.AccountNotFoundException;
import io.mifos.accounting.api.v1.client.LedgerManager;
import io.mifos.accounting.api.v1.domain.*;
import io.mifos.accounting.service.AccountingServiceConfiguration;
import io.mifos.accounting.util.AccountGenerator;
import io.mifos.accounting.util.LedgerGenerator;
import org.apache.commons.lang3.RandomStringUtils;
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
public class TestAccount {
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

  public TestAccount() {
    super();
  }

  @Before
  public void prepTest() throws Exception {
    this.autoUserContext = this.tenantApplicationSecurityEnvironment.createAutoUserContext(TestAccount.TEST_USER);
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
  public void shouldCreateAccount() throws Exception {
    final Ledger ledger = LedgerGenerator.createRandomLedger();

    this.testSubject.createLedger(ledger);

    this.eventRecorder.wait(EventConstants.POST_LEDGER, ledger.getIdentifier());

    final Account account = AccountGenerator.createRandomAccount(ledger.getIdentifier());

    this.testSubject.createAccount(account);

    this.eventRecorder.wait(EventConstants.POST_ACCOUNT, account.getIdentifier());

    final Account savedAccount = this.testSubject.findAccount(account.getIdentifier());
    Assert.assertNotNull(savedAccount);
  }

  @Test
  public void shouldNotCreateAccountAlreadyExists() throws Exception {
    final Ledger ledger = LedgerGenerator.createRandomLedger();

    this.testSubject.createLedger(ledger);

    this.eventRecorder.wait(EventConstants.POST_LEDGER, ledger.getIdentifier());

    final Account account = AccountGenerator.createRandomAccount(ledger.getIdentifier());

    this.testSubject.createAccount(account);

    this.eventRecorder.wait(EventConstants.POST_ACCOUNT, account.getIdentifier());

    try {
      this.testSubject.createAccount(account);
      Assert.fail();
    } catch (final AccountAlreadyExistsException ignored) {
    }
  }

  @Test
  public void shouldNotCreatedAccountUnknownReferenceAccount() throws Exception {
    final Ledger ledger = LedgerGenerator.createRandomLedger();

    this.testSubject.createLedger(ledger);

    this.eventRecorder.wait(EventConstants.POST_LEDGER, ledger.getIdentifier());

    final Account account = AccountGenerator.createRandomAccount(ledger.getIdentifier());
    account.setReferenceAccount(RandomStringUtils.randomAlphanumeric(8));

    try {
      this.testSubject.createAccount(account);
      Assert.fail();
    } catch (final IllegalArgumentException ex) {
      Assert.assertTrue(ex.getMessage().contains(account.getReferenceAccount()));
    }
  }

  @Test
  public void shouldNotCreateAccountUnknownLedger() throws Exception {
    final Account account = AccountGenerator.createRandomAccount(RandomStringUtils.randomAlphanumeric(8));

    try {
      this.testSubject.createAccount(account);
      Assert.fail();
    } catch (final IllegalArgumentException ex) {
      Assert.assertTrue(ex.getMessage().contains(account.getLedger()));
    }
  }

  @Test
  public void shouldNotCreatedAccountTypeMismatch() throws Exception {
    final Ledger ledger = LedgerGenerator.createRandomLedger();

    this.testSubject.createLedger(ledger);

    this.eventRecorder.wait(EventConstants.POST_LEDGER, ledger.getIdentifier());

    final Account account = AccountGenerator.createRandomAccount(ledger.getIdentifier());
    account.setType(AccountType.LIABILITY.name());

    try {
      this.testSubject.createAccount(account);
      Assert.fail();
    } catch (final IllegalArgumentException ex) {
      Assert.assertTrue(ex.getMessage().contains(account.getType()));
    }
  }

  @Test
  public void shouldFindAccount() throws Exception {
    final Ledger ledger = LedgerGenerator.createRandomLedger();

    this.testSubject.createLedger(ledger);

    this.eventRecorder.wait(EventConstants.POST_LEDGER, ledger.getIdentifier());

    final Account referenceAccount = AccountGenerator.createRandomAccount(ledger.getIdentifier());

    this.testSubject.createAccount(referenceAccount);

    this.eventRecorder.wait(EventConstants.POST_ACCOUNT, referenceAccount.getIdentifier());

    final Account account = AccountGenerator.createRandomAccount(ledger.getIdentifier());
    account.setReferenceAccount(referenceAccount.getIdentifier());
    this.testSubject.createAccount(account);

    this.eventRecorder.wait(EventConstants.POST_ACCOUNT, account.getIdentifier());

    final Account savedAccount = this.testSubject.findAccount(account.getIdentifier());
    Assert.assertNotNull(savedAccount);
    Assert.assertEquals(account.getIdentifier(), savedAccount.getIdentifier());
    Assert.assertEquals(account.getName(), savedAccount.getName());
    Assert.assertEquals(account.getType(), savedAccount.getType());
    Assert.assertEquals(account.getLedger(), savedAccount.getLedger());
    Assert.assertEquals(account.getReferenceAccount(), savedAccount.getReferenceAccount());
    Assert.assertTrue(account.getHolders().containsAll(savedAccount.getHolders()));
    Assert.assertTrue(account.getSignatureAuthorities().containsAll(savedAccount.getSignatureAuthorities()));
    Assert.assertEquals(account.getBalance(), savedAccount.getBalance());
    Assert.assertNotNull(savedAccount.getCreatedBy());
    Assert.assertNotNull(savedAccount.getCreatedOn());
    Assert.assertNull(savedAccount.getLastModifiedBy());
    Assert.assertNull(savedAccount.getLastModifiedOn());
    Assert.assertEquals(Account.State.OPEN.name(), savedAccount.getState());
  }

  @Test
  public void shouldNotFindAccountUnknown() {
    final String randomName = RandomStringUtils.randomAlphanumeric(8);
    try {
      this.testSubject.findAccount(randomName);
      Assert.fail();
    } catch (final AccountNotFoundException ignored) {
    }
  }

  @Test
  public void shouldFetchAccounts() throws Exception {
    final AccountPage currentAccountPage = this.testSubject.fetchAccounts(true, null, null, null, null, null);

    final Ledger ledger = LedgerGenerator.createRandomLedger();

    this.testSubject.createLedger(ledger);

    this.eventRecorder.wait(EventConstants.POST_LEDGER, ledger.getIdentifier());

    final Account account = AccountGenerator.createRandomAccount(ledger.getIdentifier());

    this.testSubject.createAccount(account);

    this.eventRecorder.wait(EventConstants.POST_ACCOUNT, account.getIdentifier());

    final AccountPage accountPage = this.testSubject.fetchAccounts(true, null, null, null, null, null);
    Assert.assertEquals(currentAccountPage.getTotalElements() + 1L, accountPage.getTotalElements().longValue());
  }

  @Test
  public void shouldFetchAccountForTerm() throws Exception {
    final Ledger ledger = LedgerGenerator.createRandomLedger();

    this.testSubject.createLedger(ledger);

    this.eventRecorder.wait(EventConstants.POST_LEDGER, ledger.getIdentifier());

    final Account referenceAccount = AccountGenerator.createRandomAccount(ledger.getIdentifier());
    referenceAccount.setIdentifier("001.1");

    this.testSubject.createAccount(referenceAccount);

    this.eventRecorder.wait(EventConstants.POST_ACCOUNT, referenceAccount.getIdentifier());

    final Account account = AccountGenerator.createRandomAccount(ledger.getIdentifier());
    account.setIdentifier("001.2");
    account.setReferenceAccount(referenceAccount.getIdentifier());

    this.testSubject.createAccount(account);

    this.eventRecorder.wait(EventConstants.POST_ACCOUNT, account.getIdentifier());

    final AccountPage accountPage = this.testSubject.fetchAccounts(true, "001", null, null, null, null);
    Assert.assertTrue(accountPage.getTotalElements() == 2);
  }

  @Test
  public void shouldNotFetchAccountUnknownTerm() throws Exception {
    final AccountPage accountPage =
        this.testSubject.fetchAccounts(true, RandomStringUtils.randomAlphanumeric(8), null, null, null, null);
    Assert.assertTrue(accountPage.getTotalElements() == 0);
  }

  @Test
  public void shouldFindOnlyActiveAccounts() throws Exception {
    final Ledger ledger = LedgerGenerator.createRandomLedger();

    this.testSubject.createLedger(ledger);

    this.eventRecorder.wait(EventConstants.POST_LEDGER, ledger.getIdentifier());

    final Account referenceAccount = AccountGenerator.createRandomAccount(ledger.getIdentifier());
    this.testSubject.createAccount(referenceAccount);
    this.eventRecorder.wait(EventConstants.POST_ACCOUNT, referenceAccount.getIdentifier());

    final Account account = AccountGenerator.createRandomAccount(ledger.getIdentifier());
    account.setReferenceAccount(referenceAccount.getIdentifier());
    this.testSubject.createAccount(account);
    this.eventRecorder.wait(EventConstants.POST_ACCOUNT, account.getIdentifier());

    final AccountCommand accountCommand = new AccountCommand();
    accountCommand.setAction(AccountCommand.Action.CLOSE.name());
    accountCommand.setComment("close reference!");
    this.testSubject.accountCommand(referenceAccount.getIdentifier(), accountCommand);
    Assert.assertTrue(this.eventRecorder.wait(EventConstants.CLOSE_ACCOUNT, referenceAccount.getIdentifier()));

    final AccountPage accountPage = this.testSubject.fetchAccounts(false, null, null, null, null, null);
    Assert.assertTrue(accountPage.getTotalElements() == 1);
  }

  @Test
  public void shouldModifyAccount() throws Exception {
    final Ledger ledger = LedgerGenerator.createRandomLedger();

    this.testSubject.createLedger(ledger);

    this.eventRecorder.wait(EventConstants.POST_LEDGER, ledger.getIdentifier());

    final Account account = AccountGenerator.createRandomAccount(ledger.getIdentifier());
    this.testSubject.createAccount(account);

    this.eventRecorder.wait(EventConstants.POST_ACCOUNT, account.getIdentifier());

    final Account modifiedAccount = AccountGenerator.createRandomAccount(ledger.getIdentifier());
    modifiedAccount.setIdentifier(account.getIdentifier());
    modifiedAccount.setType(account.getType());

    this.testSubject.modifyAccount(modifiedAccount.getIdentifier(), modifiedAccount);

    this.eventRecorder.wait(EventConstants.PUT_ACCOUNT, modifiedAccount.getIdentifier());

    final Account fetchedAccount = this.testSubject.findAccount(account.getIdentifier());
    Assert.assertNotNull(fetchedAccount);
    Assert.assertEquals(modifiedAccount.getIdentifier(), fetchedAccount.getIdentifier());
    Assert.assertEquals(modifiedAccount.getType(), fetchedAccount.getType());
    Assert.assertEquals(modifiedAccount.getLedger(), fetchedAccount.getLedger());
    Assert.assertEquals(modifiedAccount.getReferenceAccount(), fetchedAccount.getReferenceAccount());
    Assert.assertTrue(modifiedAccount.getHolders().containsAll(fetchedAccount.getHolders()));
    Assert.assertTrue(modifiedAccount.getSignatureAuthorities().containsAll(fetchedAccount.getSignatureAuthorities()));
    Assert.assertEquals(modifiedAccount.getBalance(), fetchedAccount.getBalance());
    Assert.assertNotNull(fetchedAccount.getCreatedBy());
    Assert.assertNotNull(fetchedAccount.getCreatedOn());
    Assert.assertNotNull(fetchedAccount.getLastModifiedBy());
    Assert.assertNotNull(fetchedAccount.getLastModifiedOn());
    Assert.assertEquals(Account.State.OPEN.name(), fetchedAccount.getState());
  }

  @Test
  public void shouldCloseAccount() throws Exception {
    final Ledger randomLedger = LedgerGenerator.createRandomLedger();
    this.testSubject.createLedger(randomLedger);
    this.eventRecorder.wait(EventConstants.POST_LEDGER, randomLedger.getIdentifier());

    final Account randomAccount = AccountGenerator.createRandomAccount(randomLedger.getIdentifier());
    this.testSubject.createAccount(randomAccount);
    this.eventRecorder.wait(EventConstants.POST_ACCOUNT, randomAccount.getIdentifier());

    final AccountCommand accountCommand = new AccountCommand();
    accountCommand.setAction(AccountCommand.Action.CLOSE.name());
    accountCommand.setComment("close this!");
    this.testSubject.accountCommand(randomAccount.getIdentifier(), accountCommand);
    Assert.assertTrue(this.eventRecorder.wait(EventConstants.CLOSE_ACCOUNT, randomAccount.getIdentifier()));
  }

  @Test
  public void shouldReopenAccount() throws Exception {
    final Ledger randomLedger = LedgerGenerator.createRandomLedger();
    this.testSubject.createLedger(randomLedger);
    this.eventRecorder.wait(EventConstants.POST_LEDGER, randomLedger.getIdentifier());

    final Account randomAccount = AccountGenerator.createRandomAccount(randomLedger.getIdentifier());
    this.testSubject.createAccount(randomAccount);
    this.eventRecorder.wait(EventConstants.POST_ACCOUNT, randomAccount.getIdentifier());

    final AccountCommand closeAccountCommand = new AccountCommand();
    closeAccountCommand.setAction(AccountCommand.Action.CLOSE.name());
    closeAccountCommand.setComment("close this!");
    this.testSubject.accountCommand(randomAccount.getIdentifier(), closeAccountCommand);
    this.eventRecorder.wait(EventConstants.CLOSE_ACCOUNT, randomAccount.getIdentifier());

    final AccountCommand reopenAccountCommand = new AccountCommand();
    reopenAccountCommand.setAction(AccountCommand.Action.REOPEN.name());
    reopenAccountCommand.setComment("reopen it!");
    this.testSubject.accountCommand(randomAccount.getIdentifier(), reopenAccountCommand);
    Assert.assertTrue(this.eventRecorder.wait(EventConstants.REOPEN_ACCOUNT, randomAccount.getIdentifier()));
  }

  @Test
  public void shouldLockAccount() throws Exception {
    final Ledger randomLedger = LedgerGenerator.createRandomLedger();
    this.testSubject.createLedger(randomLedger);
    this.eventRecorder.wait(EventConstants.POST_LEDGER, randomLedger.getIdentifier());

    final Account randomAccount = AccountGenerator.createRandomAccount(randomLedger.getIdentifier());
    this.testSubject.createAccount(randomAccount);
    this.eventRecorder.wait(EventConstants.POST_ACCOUNT, randomAccount.getIdentifier());

    final AccountCommand accountCommand = new AccountCommand();
    accountCommand.setAction(AccountCommand.Action.LOCK.name());
    accountCommand.setComment("lock this!");
    this.testSubject.accountCommand(randomAccount.getIdentifier(), accountCommand);
    Assert.assertTrue(this.eventRecorder.wait(EventConstants.LOCK_ACCOUNT, randomAccount.getIdentifier()));
  }

  @Test
  public void shouldUnlockAccount() throws Exception {
    final Ledger randomLedger = LedgerGenerator.createRandomLedger();
    this.testSubject.createLedger(randomLedger);
    this.eventRecorder.wait(EventConstants.POST_LEDGER, randomLedger.getIdentifier());

    final Account randomAccount = AccountGenerator.createRandomAccount(randomLedger.getIdentifier());
    this.testSubject.createAccount(randomAccount);
    this.eventRecorder.wait(EventConstants.POST_ACCOUNT, randomAccount.getIdentifier());

    final AccountCommand lockAccountCommand = new AccountCommand();
    lockAccountCommand.setAction(AccountCommand.Action.LOCK.name());
    lockAccountCommand.setComment("lock this!");
    this.testSubject.accountCommand(randomAccount.getIdentifier(), lockAccountCommand);
    this.eventRecorder.wait(EventConstants.LOCK_ACCOUNT, randomAccount.getIdentifier());

    final AccountCommand unlockAccountCommand = new AccountCommand();
    unlockAccountCommand.setAction(AccountCommand.Action.UNLOCK.name());
    unlockAccountCommand.setComment("unlock it!");
    this.testSubject.accountCommand(randomAccount.getIdentifier(), unlockAccountCommand);
    Assert.assertTrue(this.eventRecorder.wait(EventConstants.UNLOCK_ACCOUNT, randomAccount.getIdentifier()));
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
