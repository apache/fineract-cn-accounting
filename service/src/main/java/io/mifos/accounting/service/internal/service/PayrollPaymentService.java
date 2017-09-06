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
package io.mifos.accounting.service.internal.service;

import io.mifos.accounting.api.v1.domain.PayrollCollectionHistory;
import io.mifos.accounting.api.v1.domain.PayrollPaymentPage;
import io.mifos.accounting.service.ServiceConstants;
import io.mifos.accounting.service.internal.mapper.PayrollPaymentMapper;
import io.mifos.accounting.service.internal.repository.PayrollCollectionRepository;
import io.mifos.accounting.service.internal.repository.PayrollPaymentEntity;
import io.mifos.accounting.service.internal.repository.PayrollPaymentRepository;
import io.mifos.core.lang.DateConverter;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class PayrollPaymentService {

  private final Logger logger;
  private final PayrollCollectionRepository payrollCollectionRepository;
  private final PayrollPaymentRepository payrollPaymentRepository;

  @Autowired
  public PayrollPaymentService(@Qualifier(ServiceConstants.LOGGER_NAME) final Logger logger,
                               final PayrollPaymentRepository payrollPaymentRepository,
                               final PayrollCollectionRepository payrollCollectionRepository) {
    this.logger = logger;
    this.payrollPaymentRepository = payrollPaymentRepository;
    this.payrollCollectionRepository = payrollCollectionRepository;
  }

  public PayrollPaymentPage fetchPayrollPayments(final String identifier, final Pageable pageable) {

    final Page<PayrollPaymentEntity> payrollPaymentEntities =
        this.payrollPaymentRepository.findByCollectionIdentifier(identifier, pageable);

    final PayrollPaymentPage payrollPaymentPage = new PayrollPaymentPage();
    payrollPaymentPage.setTotalPages(payrollPaymentEntities.getTotalPages());
    payrollPaymentPage.setTotalElements(payrollPaymentEntities.getTotalElements());
    if (payrollPaymentEntities.hasContent()) {
      payrollPaymentEntities.forEach(payrollPaymentEntity -> payrollPaymentPage.add(PayrollPaymentMapper.map(payrollPaymentEntity)));
    }

    return payrollPaymentPage;
  }

  public List<PayrollCollectionHistory> fetchPayrollCollectionHistory() {
    final List<PayrollCollectionHistory> payrollCollectionHistories = new ArrayList<>();

    this.payrollCollectionRepository.findAll().forEach(payrollCollectionEntity -> {
      final PayrollCollectionHistory payrollCollectionHistory = new PayrollCollectionHistory();
      payrollCollectionHistory.setIdentifier(payrollCollectionEntity.getIdentifier());
      payrollCollectionHistory.setSourceAccountNumber(payrollCollectionEntity.getSourceAccountNumber());
      payrollCollectionHistory.setCreatedBy(payrollCollectionEntity.getCreatedBy());
      payrollCollectionHistory.setCreatedOn(DateConverter.toIsoString(payrollCollectionEntity.getCreatedOn()));
      payrollCollectionHistories.add(payrollCollectionHistory);
    });

    return payrollCollectionHistories;
  }
}
