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
package io.mifos.accounting.service.internal.repository.specification;

import io.mifos.accounting.service.internal.repository.LedgerEntity;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.Predicate;
import java.util.ArrayList;

public class LedgerSpecification {

  private LedgerSpecification() {
    super();
  }

  public static Specification<LedgerEntity> createSpecification(
      final boolean includeSubLedger, final String term, final String type) {
    return (root, query, cb) -> {

      final ArrayList<Predicate> predicates = new ArrayList<>();

      if (!includeSubLedger) {
        predicates.add(
            cb.isNull(root.get("parentLedger"))
        );
      }

      if (term != null) {
        final String likeExpression = "%" + term + "%";

        predicates.add(
            cb.or(
                cb.like(root.get("identifier"), likeExpression),
                cb.like(root.get("name"), likeExpression)
            )
        );
      }

      if (type != null) {
        predicates.add(cb.equal(root.get("type"), type));
      }

      return cb.and(predicates.toArray(new Predicate[predicates.size()]));
    };
  }
}
