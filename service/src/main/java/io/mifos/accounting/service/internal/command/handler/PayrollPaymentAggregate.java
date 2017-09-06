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
package io.mifos.accounting.service.internal.command.handler;

import com.google.common.collect.Sets;
import io.mifos.accounting.api.v1.EventConstants;
import io.mifos.accounting.api.v1.domain.Creditor;
import io.mifos.accounting.api.v1.domain.Debtor;
import io.mifos.accounting.api.v1.domain.JournalEntry;
import io.mifos.accounting.api.v1.domain.PayrollCollectionSheet;
import io.mifos.accounting.service.ServiceConstants;
import io.mifos.accounting.service.internal.command.CreateJournalEntryCommand;
import io.mifos.accounting.service.internal.command.ProcessPayrollPaymentCommand;
import io.mifos.accounting.service.internal.repository.PayrollCollectionEntity;
import io.mifos.accounting.service.internal.repository.PayrollCollectionRepository;
import io.mifos.accounting.service.internal.repository.PayrollPaymentEntity;
import io.mifos.accounting.service.internal.repository.PayrollPaymentRepository;
import io.mifos.accounting.service.internal.service.helper.CustomerAdapter;
import io.mifos.core.api.util.UserContextHolder;
import io.mifos.core.command.annotation.Aggregate;
import io.mifos.core.command.annotation.CommandHandler;
import io.mifos.core.command.annotation.EventEmitter;
import io.mifos.core.command.gateway.CommandGateway;
import io.mifos.core.lang.DateConverter;
import org.apache.commons.lang.RandomStringUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.UUID;

@Aggregate
public class PayrollPaymentAggregate {

  private final Logger logger;
  private final PayrollCollectionRepository payrollCollectionRepository;
  private final PayrollPaymentRepository payrollPaymentRepository;
  private final CustomerAdapter customerAdapter;
  private final CommandGateway commandGateway;

  @Autowired
  public PayrollPaymentAggregate(@Qualifier(ServiceConstants.LOGGER_NAME) final Logger logger,
                                 final PayrollCollectionRepository payrollCollectionRepository,
                                 final PayrollPaymentRepository payrollPaymentRepository,
                                 final CustomerAdapter customerAdapter,
                                 final CommandGateway commandGateway) {
    super();
    this.logger = logger;
    this.payrollCollectionRepository = payrollCollectionRepository;
    this.payrollPaymentRepository = payrollPaymentRepository;
    this.customerAdapter = customerAdapter;
    this.commandGateway = commandGateway;
  }

  @Transactional
  @CommandHandler
  @EventEmitter(selectorName = EventConstants.SELECTOR_NAME, selectorValue = EventConstants.POST_PAYROLL_PAYMENT)
  public String process(final ProcessPayrollPaymentCommand processPayrollPaymentCommand) {
    final PayrollCollectionSheet payrollCollectionSheet = processPayrollPaymentCommand.payrollCollectionSheet();

    final PayrollCollectionEntity payrollCollectionEntity = new PayrollCollectionEntity();
    payrollCollectionEntity.setIdentifier(RandomStringUtils.randomAlphanumeric(32));
    payrollCollectionEntity.setSourceAccountNumber(payrollCollectionSheet.getSourceAccountNumber());
    payrollCollectionEntity.setCreatedBy(UserContextHolder.checkedGetUser());
    payrollCollectionEntity.setCreatedOn(LocalDateTime.now(Clock.systemUTC()));

    this.payrollCollectionRepository.save(payrollCollectionEntity);

    final MathContext mathContext = new MathContext(2, RoundingMode.HALF_EVEN);

    payrollCollectionSheet.getPayrollPayments().forEach(payrollPayment ->
        this.customerAdapter.findPayrollDistribution(payrollPayment.getCustomerIdentifier()).ifPresent(payrollDistribution -> {
          final PayrollPaymentEntity payrollPaymentEntity = new PayrollPaymentEntity();
          payrollPaymentEntity.setCollectionIdentifier(payrollCollectionEntity.getIdentifier());
          payrollPaymentEntity.setCustomerIdentifier(payrollPayment.getCustomerIdentifier());
          payrollPaymentEntity.setEmployer(payrollPayment.getEmployer());
          payrollPaymentEntity.setSalary(payrollPayment.getSalary());
          this.payrollPaymentRepository.save(payrollPaymentEntity);

          final JournalEntry journalEntry = new JournalEntry();
          journalEntry.setTransactionIdentifier("acct-sala-" + UUID.randomUUID().toString());
          journalEntry.setTransactionDate(DateConverter.toIsoString(LocalDateTime.now(Clock.systemUTC())));
          journalEntry.setTransactionType("SALA");
          journalEntry.setClerk(payrollCollectionEntity.getCreatedBy());
          journalEntry.setNote("Payroll Distribution");

          final Debtor debtor = new Debtor();
          debtor.setAccountNumber(payrollCollectionSheet.getSourceAccountNumber());
          debtor.setAmount(payrollPayment.getSalary().toString());
          journalEntry.setDebtors(Sets.newHashSet(debtor));

          final HashSet<Creditor> creditors = new HashSet<>();
          journalEntry.setCreditors(creditors);

          payrollDistribution.getPayrollAllocations().forEach(payrollAllocation -> {
            final Creditor allocationCreditor = new Creditor();
            allocationCreditor.setAccountNumber(payrollAllocation.getAccountNumber());
            if (!payrollAllocation.getProportional()) {
              allocationCreditor.setAmount(payrollAllocation.getAmount().toString());
            } else {
              final BigDecimal value = payrollPayment.getSalary().multiply(
                  payrollAllocation.getAmount().divide(BigDecimal.valueOf(100.00D), mathContext)
              ).round(mathContext);
              allocationCreditor.setAmount(value.toString());
            }
            creditors.add(allocationCreditor);
          });

          final BigDecimal currentCreditorSum =
              BigDecimal.valueOf(creditors.stream().mapToDouble(value -> Double.valueOf(value.getAmount())).sum());

          final Creditor mainCreditor = new Creditor();
          mainCreditor.setAccountNumber(payrollDistribution.getMainAccountNumber());
          mainCreditor.setAmount(payrollPayment.getSalary().subtract(currentCreditorSum).toString());
          creditors.add(mainCreditor);

          this.commandGateway.process(new CreateJournalEntryCommand(journalEntry));
        })
    );
    return payrollCollectionSheet.getSourceAccountNumber();
  }
}
