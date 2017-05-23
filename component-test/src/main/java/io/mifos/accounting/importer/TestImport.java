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
import io.mifos.accounting.api.v1.domain.*;
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
    final URL uri = ClassLoader.getSystemResource("importdata/account-happy-case.txt");
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
  public void testLedgerImportHappyCase() throws IOException, InterruptedException {
    final LedgerImporter ledgerImporter = new LedgerImporter(testSubject, logger);
    final URL uri = ClassLoader.getSystemResource("importdata/ledger-happy-case.txt");
    ledgerImporter.importCSV(uri);

    //Import a second time.
    ledgerImporter.importCSV(uri);

    Assert.assertTrue(eventRecorder.wait(EventConstants.POST_LEDGER, "110"));
    Assert.assertTrue(eventRecorder.wait(EventConstants.POST_LEDGER, "111"));
    Assert.assertTrue(eventRecorder.wait(EventConstants.POST_LEDGER, "111.1"));

    final LedgerPage ledgerPage = testSubject.fetchLedgers(true, "11", null, null, null, null);
    final List<Ledger> ledgers = ledgerPage.getLedgers();
    Assert.assertEquals(3,ledgers.size());
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
    final URL uri = ClassLoader.getSystemResource("importdata/ledger-missing-name-case.txt");
    ledgerImporter.importCSV(uri);

    //Import a second time.
    ledgerImporter.importCSV(uri);

    Assert.assertTrue(eventRecorder.wait(EventConstants.POST_LEDGER, "210"));
    Assert.assertTrue(eventRecorder.wait(EventConstants.POST_LEDGER, "211"));
    Assert.assertTrue(eventRecorder.wait(EventConstants.POST_LEDGER, "211.1"));

    final LedgerPage ledgerPage = testSubject.fetchLedgers(true, "21", null, null, null, null);
    final List<Ledger> ledgers = ledgerPage.getLedgers();
    Assert.assertEquals(3,ledgers.size());

    ledgers.forEach(x -> Assert.assertEquals(x.getIdentifier(), x.getName()));
  }
}
