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
package org.apache.fineract.cn.accounting.service.internal.repository;

import java.time.LocalDateTime;
import javax.persistence.Convert;
import org.apache.fineract.cn.postgresql.util.LocalDateTimeConverter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountEntryRepository extends JpaRepository<AccountEntryEntity, Long> {

  @Convert(converter = LocalDateTimeConverter.class)
  Page<AccountEntryEntity> findByAccountAndTransactionDateBetween(final AccountEntity accountEntity,
                                                                  final LocalDateTime dateFrom,
                                                                  final LocalDateTime dateTo,
                                                                  final Pageable pageable);

  @Convert(converter = LocalDateTimeConverter.class)
  Page<AccountEntryEntity> findByAccountAndTransactionDateBetweenAndMessageEquals(final AccountEntity accountEntity,
                                                                                  final LocalDateTime dateFrom,
                                                                                  final LocalDateTime dateTo,
                                                                                  final String message,
                                                                                  final Pageable pageable);


  @Query("SELECT CASE WHEN count(a) > 0 THEN true ELSE false END FROM AccountEntryEntity a where a.account = :accountEntity")
  Boolean existsByAccount(@Param("accountEntity") final AccountEntity accountEntity);
}
