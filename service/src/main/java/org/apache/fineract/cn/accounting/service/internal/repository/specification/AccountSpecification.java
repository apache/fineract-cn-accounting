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
package org.apache.fineract.cn.accounting.service.internal.repository.specification;

import org.apache.fineract.cn.accounting.api.v1.domain.Account;
import org.apache.fineract.cn.accounting.service.internal.repository.AccountEntity;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.Predicate;
import java.util.ArrayList;

public class AccountSpecification {

  private AccountSpecification() {
    super();
  }

  public static Specification<AccountEntity> createSpecification(
      final boolean includeClosed, final String term, final String type, final boolean includeCustomerAccounts) {

    return (root, query, cb) -> {

      final ArrayList<Predicate> predicates = new ArrayList<>();

      if (!includeClosed) {
        predicates.add(
            root.get("state").in(
                Account.State.OPEN.name(),
                Account.State.LOCKED.name()
            )
        );
      }

      if (term != null) {
        final String likeExpression = "%" + term + "%";
        predicates.add(
            cb.or(
                cb.like(root.get("identifier"), likeExpression),
                cb.like(root.get("name"), likeExpression),
                cb.like(root.get("alternativeAccountNumber"), likeExpression)
            )
        );
      }

      if (type != null) {
        predicates.add(cb.equal(root.get("type"), type));
      }

      if (!includeCustomerAccounts) {
        predicates.add(
            cb.or(
                cb.equal(root.get("holders"), ""),
                cb.isNull(root.get("holders"))
            )
        );
      }

      return cb.and(predicates.toArray(new Predicate[predicates.size()]));
    };
  }
}
