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
package io.mifos.accounting.service.internal.repository;

import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;

@SuppressWarnings({"unused", "WeakerAccess"})
@Table(name = "thoth_journal_entry_lookup")
public class JournalEntryLookup {

  @PartitionKey
  @Column(name = "transaction_identifier")
  private String transactionIdentifier;
  @Column(name = "date_bucket")
  private String dateBucket;

  public JournalEntryLookup() {
    super();
  }

  public String getTransactionIdentifier() {
    return this.transactionIdentifier;
  }

  public void setTransactionIdentifier(final String transactionIdentifier) {
    this.transactionIdentifier = transactionIdentifier;
  }

  public String getDateBucket() {
    return this.dateBucket;
  }

  public void setDateBucket(final String dateBucket) {
    this.dateBucket = dateBucket;
  }
}
