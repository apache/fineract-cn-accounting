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
package io.mifos.accounting.importer;

import io.mifos.accounting.AbstractAccountingTest;
import io.mifos.accounting.api.v1.EventConstants;
import io.mifos.accounting.api.v1.domain.Account;
import io.mifos.accounting.api.v1.domain.AccountPage;
import io.mifos.accounting.api.v1.domain.AccountType;
import io.mifos.accounting.api.v1.domain.Ledger;
import io.mifos.accounting.api.v1.domain.LedgerPage;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Optional;

/**
 * @author Myrle Krantz
 */
public class TestImport extends AbstractAccountingTest {
  @Test
  public void testAccountImportHappyCase() throws IOException, InterruptedException {
    final Ledger assetLedger = new Ledger();
    assetLedger.setType(AccountType.ASSET.name());
    assetLedger.setIdentifier("assetLedger");
    assetLedger.setName("asset Ledger");
    assetLedger.setShowAccountsInChart(true);

    testSubject.createLedger(assetLedger);
    Assert.assertTrue(eventRecorder.wait(EventConstants.POST_LEDGER, assetLedger.getIdentifier()));

    final Ledger equityLedger = new Ledger();
    equityLedger.setType(AccountType.EQUITY.name());
    equityLedger.setIdentifier("equityLedger");
    equityLedger.setName("equity Ledger");
    equityLedger.setShowAccountsInChart(true);

    testSubject.createLedger(equityLedger);
    Assert.assertTrue(eventRecorder.wait(EventConstants.POST_LEDGER, equityLedger.getIdentifier()));


    final AccountImporter accountImporter = new AccountImporter(testSubject, logger);
    final URL uri = ClassLoader.getSystemResource("importdata/account-happy-case.csv");
    accountImporter.importCSV(uri);
    Assert.assertTrue(eventRecorder.wait(EventConstants.POST_ACCOUNT, "abcd"));
    Assert.assertTrue(eventRecorder.wait(EventConstants.POST_ACCOUNT, "xyz"));

    //Import a second time.
    accountImporter.importCSV(uri);

    final AccountPage accountsOfAssetLedger
            = testSubject.fetchAccountsOfLedger(assetLedger.getIdentifier(), 0, 10, null, null);
    final Account firstAccount = accountsOfAssetLedger.getAccounts().get(0);
    Assert.assertEquals("abcd", firstAccount.getIdentifier());
    Assert.assertEquals(Double.valueOf(0.0), firstAccount.getBalance());
    Assert.assertEquals(AbstractAccountingTest.TEST_USER, firstAccount.getCreatedBy());

    final AccountPage accountsOfEquityLedger
            = testSubject.fetchAccountsOfLedger(equityLedger.getIdentifier(), 0, 10, null, null);
    final Account secondAccount = accountsOfEquityLedger.getAccounts().get(0);
    Assert.assertEquals("xyz", secondAccount.getIdentifier());
    Assert.assertEquals(Double.valueOf(20.0), secondAccount.getBalance());
    Assert.assertEquals(AbstractAccountingTest.TEST_USER, secondAccount.getCreatedBy());
  }

  @Test
  public void testAccountFromARealCase() throws IOException, InterruptedException {
    final Ledger ledger1000 = new Ledger();
    ledger1000.setType(AccountType.REVENUE.name());
    ledger1000.setIdentifier("1000");
    ledger1000.setName("Income");
    ledger1000.setShowAccountsInChart(true);

    testSubject.createLedger(ledger1000);
    Assert.assertTrue(eventRecorder.wait(EventConstants.POST_LEDGER, ledger1000.getIdentifier()));

    final Ledger ledger1100 = new Ledger();
    ledger1100.setType(AccountType.REVENUE.name());
    ledger1100.setIdentifier("1100");
    ledger1100.setName("Income from Loans");
    ledger1100.setParentLedgerIdentifier("1000");
    ledger1100.setShowAccountsInChart(true);

    testSubject.createLedger(ledger1100);
    Assert.assertTrue(eventRecorder.wait(EventConstants.POST_LEDGER, ledger1100.getIdentifier()));


    final AccountImporter accountImporter = new AccountImporter(testSubject, logger);
    final URL uri = ClassLoader.getSystemResource("importdata/account-from-a-real-case.csv");
    accountImporter.importCSV(uri);
    Assert.assertTrue(eventRecorder.wait(EventConstants.POST_ACCOUNT, "1101"));
    Assert.assertTrue(eventRecorder.wait(EventConstants.POST_ACCOUNT, "1102"));
    Assert.assertTrue(eventRecorder.wait(EventConstants.POST_ACCOUNT, "1103"));
    Assert.assertTrue(eventRecorder.wait(EventConstants.POST_ACCOUNT, "1104"));
    Assert.assertTrue(eventRecorder.wait(EventConstants.POST_ACCOUNT, "1105"));
    Assert.assertTrue(eventRecorder.wait(EventConstants.POST_ACCOUNT, "1120"));
    Assert.assertTrue(eventRecorder.wait(EventConstants.POST_ACCOUNT, "1121"));
    Assert.assertTrue(eventRecorder.wait(EventConstants.POST_ACCOUNT, "1140"));
    Assert.assertTrue(eventRecorder.wait(EventConstants.POST_ACCOUNT, "1190"));

    //Import a second time.
    accountImporter.importCSV(uri);

    final AccountPage accountsOfAssetLedger
            = testSubject.fetchAccountsOfLedger(ledger1100.getIdentifier(), 0, 10, null, null);
    final Account firstAccount = accountsOfAssetLedger.getAccounts().get(0);
    Assert.assertEquals("1101", firstAccount.getIdentifier());
    Assert.assertEquals("Interest on Business Loans", firstAccount.getName());
    Assert.assertEquals(Double.valueOf(0.0), firstAccount.getBalance());
    Assert.assertEquals(AccountType.REVENUE, AccountType.valueOf(firstAccount.getType()));
    Assert.assertEquals(AbstractAccountingTest.TEST_USER, firstAccount.getCreatedBy());

    final AccountPage accountsOfEquityLedger
            = testSubject.fetchAccountsOfLedger(ledger1100.getIdentifier(), 0, 10, null, null);
    final Account secondAccount = accountsOfEquityLedger.getAccounts().get(1);
    Assert.assertEquals("1102", secondAccount.getIdentifier());
    Assert.assertEquals("Interest on Agriculture Loans", secondAccount.getName());
    Assert.assertEquals(Double.valueOf(0.0), secondAccount.getBalance());
    Assert.assertEquals(AccountType.REVENUE, AccountType.valueOf(secondAccount.getType()));
    Assert.assertEquals(AbstractAccountingTest.TEST_USER, secondAccount.getCreatedBy());
  }

  @Test
  public void testLedgerImportHappyCase() throws IOException, InterruptedException {
    final LedgerImporter ledgerImporter = new LedgerImporter(testSubject, logger);
    final URL uri = ClassLoader.getSystemResource("importdata/ledger-happy-case.csv");
    ledgerImporter.importCSV(uri);

    //Import a second time.
    ledgerImporter.importCSV(uri);

    Assert.assertTrue(eventRecorder.wait(EventConstants.POST_LEDGER, "110"));
    Assert.assertTrue(eventRecorder.wait(EventConstants.POST_LEDGER, "111"));
    Assert.assertTrue(eventRecorder.wait(EventConstants.POST_LEDGER, "111.1"));

    final LedgerPage ledgerPage = testSubject.fetchLedgers(true, "11", null, null, null, null, null);
    final List<Ledger> ledgers = ledgerPage.getLedgers();
    Assert.assertTrue(ledgers.size() >= 3); //3 from this test, but other tests may already have run.
    final Optional<Ledger> ledger110 = ledgers.stream().filter(x -> x.getIdentifier().equals("110")).findAny();
    Assert.assertTrue(ledger110.isPresent());
    Assert.assertEquals("Loan Ledger", ledger110.get().getDescription());
    Assert.assertEquals("ASSET", ledger110.get().getType());
    Assert.assertEquals(true, ledger110.get().getShowAccountsInChart());
    Assert.assertEquals(AbstractAccountingTest.TEST_USER, ledger110.get().getCreatedBy());

    final Optional<Ledger> ledger111 = ledgers.stream().filter(x -> x.getIdentifier().equals("111")).findAny();
    Assert.assertTrue(ledger111.isPresent());
    Assert.assertEquals("Loan Ledger Interest", ledger111.get().getDescription());
    Assert.assertEquals("ASSET", ledger111.get().getType());
    Assert.assertEquals(false, ledger111.get().getShowAccountsInChart());
    Assert.assertEquals(AbstractAccountingTest.TEST_USER, ledger111.get().getCreatedBy());

    final Optional<Ledger> ledger111dot1 = ledgers.stream().filter(x -> x.getIdentifier().equals("111.1")).findAny();
    Assert.assertTrue(ledger111dot1.isPresent());
    Assert.assertEquals("blub blah", ledger111dot1.get().getDescription());
    Assert.assertEquals("ASSET", ledger111dot1.get().getType());
    Assert.assertEquals(true, ledger111dot1.get().getShowAccountsInChart());
    Assert.assertEquals(AbstractAccountingTest.TEST_USER, ledger111dot1.get().getCreatedBy());
  }

  @Test
  public void testLedgerImportMissingNameCase() throws IOException, InterruptedException {
    final LedgerImporter ledgerImporter = new LedgerImporter(testSubject, logger);
    final URL uri = ClassLoader.getSystemResource("importdata/ledger-missing-name-case.csv");
    ledgerImporter.importCSV(uri);

    //Import a second time.
    ledgerImporter.importCSV(uri);

    Assert.assertTrue(eventRecorder.wait(EventConstants.POST_LEDGER, "210"));
    Assert.assertTrue(eventRecorder.wait(EventConstants.POST_LEDGER, "211"));
    Assert.assertTrue(eventRecorder.wait(EventConstants.POST_LEDGER, "211.1"));

    final LedgerPage ledgerPage = testSubject.fetchLedgers(true, "21", null, null, null, null, null);
    final List<Ledger> ledgers = ledgerPage.getLedgers();
    Assert.assertEquals(3,ledgers.size());

    ledgers.forEach(x -> Assert.assertEquals(x.getIdentifier(), x.getName()));
  }
}
