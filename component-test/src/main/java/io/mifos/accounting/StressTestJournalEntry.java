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
import io.mifos.accounting.api.v1.domain.Account;
import io.mifos.accounting.api.v1.domain.JournalEntry;
import io.mifos.accounting.api.v1.domain.Ledger;
import io.mifos.accounting.util.AccountGenerator;
import io.mifos.accounting.util.JournalEntryGenerator;
import io.mifos.accounting.util.LedgerGenerator;
import org.apache.commons.lang.math.RandomUtils;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

public class StressTestJournalEntry extends AbstractAccountingTest {

  @Test
  public void runStresser() throws Exception {
    final int numberOfLedgers = 32;
    final int numberOfAccounts = 512;
    final int numberOfJournalEntries = 1024;

    final Account[] preparedAccountsAsArray = this.prepareData(numberOfLedgers, numberOfAccounts);

    this.writeJournalEntries(4, numberOfJournalEntries, preparedAccountsAsArray);
    this.writeJournalEntries(8, numberOfJournalEntries, preparedAccountsAsArray);
    this.writeJournalEntries(16, numberOfJournalEntries, preparedAccountsAsArray);
    this.writeJournalEntries(24, numberOfJournalEntries, preparedAccountsAsArray);
    this.writeJournalEntries(32, numberOfJournalEntries, preparedAccountsAsArray);
  }

  private void writeJournalEntries(final int numberOfThreads, final int numberOfJournalEntries, final Account[] preparedAccountsAsArray) {
    final List<Future<?>> futures = new ArrayList<>(numberOfThreads);
    final ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
    final AtomicLong totalExecutionTime = new AtomicLong(0L);

    for (int t = 0; t < numberOfThreads; t++) {
      final Future<?> future = executorService.submit(
          () -> {
            long executionTime = 0L;
            int randomBound = preparedAccountsAsArray.length;
            for (int i = 0; i < numberOfJournalEntries; i++) {
              final Account debtorAccount = preparedAccountsAsArray[RandomUtils.nextInt(randomBound)];
              final Account creditorAccount = preparedAccountsAsArray[RandomUtils.nextInt(randomBound)];

              final JournalEntry randomJournalEntry =
                  JournalEntryGenerator.createRandomJournalEntry(debtorAccount, "50.00", creditorAccount, "50.00");
              final long start = System.currentTimeMillis();
              this.testSubject.createJournalEntry(randomJournalEntry);
              executionTime += (System.currentTimeMillis() - start);
            }
            totalExecutionTime.addAndGet(executionTime);
          }
      );
      futures.add(future);
    }

    futures.forEach(future -> {
      try {
        future.get();
      } catch (Exception e) {
        e.printStackTrace();
      }
    });

    final long numberOfProcessedJournalEntries = numberOfJournalEntries * numberOfThreads;
    final long processingTime = totalExecutionTime.get();
    this.logger.error("Added {} journal entries in {}s.", numberOfProcessedJournalEntries, (processingTime / 1000L));
    this.logger.error("Average processing time for one journal entry: {}ms", processingTime / numberOfProcessedJournalEntries);
  }

  private Account[] prepareData(int numberOfLedgers, int numberOfAccounts) throws Exception {
    final ArrayList<Account> createdAccounts = new ArrayList<>(numberOfLedgers * numberOfAccounts);
    final AtomicLong preparationTime = new AtomicLong(0L);
    for (int i = 0; i < numberOfLedgers; i++) {
      final long start = System.currentTimeMillis();
      final Ledger randomLedger = LedgerGenerator.createRandomLedger();
      this.testSubject.createLedger(randomLedger);
      this.eventRecorder.wait(EventConstants.POST_LEDGER, randomLedger.getIdentifier());
      for (int j = 0; j < numberOfAccounts; j++) {
        final Account randomAccount = AccountGenerator.createRandomAccount(randomLedger.getIdentifier());
        this.testSubject.createAccount(randomAccount);
        this.eventRecorder.wait(EventConstants.POST_ACCOUNT, randomAccount.getIdentifier());
        createdAccounts.add(randomAccount);
      }
      final long processingTime = (System.currentTimeMillis() - start);
      preparationTime.addAndGet(processingTime);
    }
    this.logger.error("Created {} ledgers and {} accounts in {}s.", numberOfLedgers, (numberOfAccounts * numberOfLedgers), (preparationTime.get() / 1000L));
    this.logger.error("Average processing time for one account: {}ms.", preparationTime.get() / (numberOfAccounts * numberOfLedgers));
    final Account[] toReturn = new Account[createdAccounts.size()];
    createdAccounts.toArray(toReturn);
    return toReturn;
  }
}
