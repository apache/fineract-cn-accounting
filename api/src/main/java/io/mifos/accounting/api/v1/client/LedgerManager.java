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
package io.mifos.accounting.api.v1.client;

import io.mifos.accounting.api.v1.domain.Account;
import io.mifos.accounting.api.v1.domain.AccountCommand;
import io.mifos.accounting.api.v1.domain.AccountEntryPage;
import io.mifos.accounting.api.v1.domain.AccountPage;
import io.mifos.accounting.api.v1.domain.ChartOfAccountEntry;
import io.mifos.accounting.api.v1.domain.JournalEntry;
import io.mifos.accounting.api.v1.domain.Ledger;
import io.mifos.accounting.api.v1.domain.LedgerPage;
import io.mifos.accounting.api.v1.domain.TransactionType;
import io.mifos.accounting.api.v1.domain.TransactionTypePage;
import io.mifos.accounting.api.v1.domain.TrialBalance;
import io.mifos.core.api.annotation.ThrowsException;
import io.mifos.core.api.annotation.ThrowsExceptions;
import io.mifos.core.api.util.CustomFeignClientsConfiguration;
import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.validation.Valid;
import java.util.List;

@SuppressWarnings("unused")
@FeignClient(value = "accounting-v1", path = "/accounting/v1", configuration = CustomFeignClientsConfiguration.class)
public interface LedgerManager {

  @RequestMapping(
      value = "/ledgers",
      method = RequestMethod.POST,
      produces = {MediaType.APPLICATION_JSON_VALUE},
      consumes = {MediaType.APPLICATION_JSON_VALUE}
  )
  @ThrowsExceptions({
      @ThrowsException(status = HttpStatus.CONFLICT, exception = LedgerAlreadyExistsException.class)
  })
  void createLedger(@RequestBody final Ledger ledger);

  @RequestMapping(
      value = "/ledgers",
      method = RequestMethod.GET,
      produces = {MediaType.ALL_VALUE},
      consumes = {MediaType.APPLICATION_JSON_VALUE}
  )
  LedgerPage fetchLedgers(@RequestParam(value = "includeSubLedgers", required = false, defaultValue = "false") final boolean includeSubLedgers,
                          @RequestParam(value = "term", required = false) final String term,
                          @RequestParam(value = "type", required = false) final String type,
                          @RequestParam(value = "pageIndex", required = false) final Integer pageIndex,
                          @RequestParam(value = "size", required = false) final Integer size,
                          @RequestParam(value = "sortColumn", required = false) final String sortColumn,
                          @RequestParam(value = "sortDirection", required = false) final String sortDirection);

  @RequestMapping(
      value = "/ledgers/{identifier}",
      method = RequestMethod.GET,
      produces = {MediaType.ALL_VALUE},
      consumes = {MediaType.APPLICATION_JSON_VALUE}
  )
  @ThrowsException(status = HttpStatus.NOT_FOUND, exception = LedgerNotFoundException.class)
  Ledger findLedger(@PathVariable("identifier") final String identifier);

  @RequestMapping(
      value = "/ledgers/{identifier}",
      method = RequestMethod.POST,
      produces = {MediaType.APPLICATION_JSON_VALUE},
      consumes = {MediaType.APPLICATION_JSON_VALUE}
  )
  @ThrowsExceptions({
      @ThrowsException(status = HttpStatus.NOT_FOUND, exception = LedgerNotFoundException.class),
      @ThrowsException(status = HttpStatus.CONFLICT, exception = LedgerAlreadyExistsException.class)
  })
  void addSubLedger(@PathVariable("identifier") final String identifier, @RequestBody final Ledger subLedger);

  @RequestMapping(
      value = "/ledgers/{identifier}",
      method = RequestMethod.PUT,
      produces = {MediaType.APPLICATION_JSON_VALUE},
      consumes = {MediaType.APPLICATION_JSON_VALUE}
  )
  @ThrowsExceptions({
      @ThrowsException(status = HttpStatus.NOT_FOUND, exception = LedgerNotFoundException.class)
  })
  void modifyLedger(@PathVariable("identifier") final String identifier, @RequestBody final Ledger subLedger);

  @RequestMapping(
      value = "/ledgers/{identifier}",
      method = RequestMethod.DELETE,
      produces = {MediaType.ALL_VALUE},
      consumes = {MediaType.APPLICATION_JSON_VALUE}
  )
  @ThrowsExceptions({
      @ThrowsException(status = HttpStatus.NOT_FOUND, exception = LedgerNotFoundException.class),
      @ThrowsException(status = HttpStatus.CONFLICT, exception = LedgerReferenceExistsException.class)
  })
  void deleteLedger(@PathVariable("identifier") final String identifier);

  @RequestMapping(
      value = "/ledgers/{identifier}/accounts",
      method = RequestMethod.GET,
      produces = {MediaType.ALL_VALUE},
      consumes = {MediaType.APPLICATION_JSON_VALUE}
  )
  @ThrowsException(status = HttpStatus.NOT_FOUND, exception = LedgerNotFoundException.class)
  AccountPage fetchAccountsOfLedger(@PathVariable("identifier") final String identifier,
                                    @RequestParam(value = "pageIndex", required = false) final Integer pageIndex,
                                    @RequestParam(value = "size", required = false) final Integer size,
                                    @RequestParam(value = "sortColumn", required = false) final String sortColumn,
                                    @RequestParam(value = "sortDirection", required = false) final String sortDirection);

  @RequestMapping(
      value = "/accounts",
      method = RequestMethod.POST,
      produces = {MediaType.APPLICATION_JSON_VALUE},
      consumes = {MediaType.APPLICATION_JSON_VALUE}
  )
  @ThrowsExceptions({
      @ThrowsException(status = HttpStatus.CONFLICT, exception = AccountAlreadyExistsException.class)
  })
  void createAccount(@RequestBody final Account account);

  @RequestMapping(
      value = "/accounts",
      method = RequestMethod.GET,
      produces = {MediaType.ALL_VALUE},
      consumes = {MediaType.APPLICATION_JSON_VALUE}
  )
  AccountPage fetchAccounts(@RequestParam(value = "includeClosed", required = false, defaultValue = "false") final boolean includeClosed,
                            @RequestParam(value = "term", required = false) final String term,
                            @RequestParam(value = "type", required = false) final String type,
                            @RequestParam(value = "pageIndex", required = false) final Integer pageIndex,
                            @RequestParam(value = "size", required = false) final Integer size,
                            @RequestParam(value = "sortColumn", required = false) final String sortColumn,
                            @RequestParam(value = "sortDirection", required = false) final String sortDirection);

  @RequestMapping(
      value = "/accounts/{identifier}",
      method = RequestMethod.GET,
      produces = {MediaType.ALL_VALUE},
      consumes = {MediaType.APPLICATION_JSON_VALUE}
  )
  @ThrowsException(status = HttpStatus.NOT_FOUND, exception = AccountNotFoundException.class)
  Account findAccount(@PathVariable("identifier") final String identifier);

  @RequestMapping(
      value = "/accounts/{identifier}",
      method = RequestMethod.PUT,
      produces = {MediaType.APPLICATION_JSON_VALUE},
      consumes = {MediaType.APPLICATION_JSON_VALUE}
  )
  @ThrowsExceptions({
      @ThrowsException(status = HttpStatus.NOT_FOUND, exception = AccountNotFoundException.class)
  })
  void modifyAccount(@PathVariable("identifier") final String identifier,
                     @RequestBody final Account account);


  @RequestMapping(
      value = "/accounts/{identifier}",
      method = RequestMethod.DELETE,
      produces = {MediaType.ALL_VALUE},
      consumes = {MediaType.APPLICATION_JSON_VALUE}
  )
  @ThrowsExceptions({
      @ThrowsException(status = HttpStatus.NOT_FOUND, exception = AccountNotFoundException.class),
      @ThrowsException(status = HttpStatus.CONFLICT, exception = AccountReferenceException.class)
  })
  void deleteAccount(@PathVariable("identifier") final String identifier);

  @RequestMapping(
      value = "/accounts/{identifier}/entries",
      method = RequestMethod.GET,
      produces = {MediaType.ALL_VALUE},
      consumes = {MediaType.APPLICATION_JSON_VALUE}
  )
  @ThrowsException(status = HttpStatus.NOT_FOUND, exception = AccountNotFoundException.class)
  AccountEntryPage fetchAccountEntries(@PathVariable("identifier") final String identifier,
                                       @RequestParam(value = "dateRange", required = false) final String dateRange,
                                       @RequestParam(value = "pageIndex", required = false) final Integer pageIndex,
                                       @RequestParam(value = "size", required = false) final Integer size,
                                       @RequestParam(value = "sortColumn", required = false) final String sortColumn,
                                       @RequestParam(value = "sortDirection", required = false) final String sortDirection);

  @RequestMapping(
          value = "/accounts/{identifier}/commands",
          method = RequestMethod.GET,
          produces = MediaType.ALL_VALUE,
          consumes = MediaType.APPLICATION_JSON_VALUE
  )
  @ThrowsException(status = HttpStatus.NOT_FOUND, exception = AccountNotFoundException.class)
  List<AccountCommand> fetchAccountCommands(@PathVariable("identifier") final String identifier);

  @RequestMapping(
      value = "/accounts/{identifier}/commands",
      method = RequestMethod.POST,
      produces = {MediaType.APPLICATION_JSON_VALUE},
      consumes = {MediaType.APPLICATION_JSON_VALUE}
  )
  @ThrowsException(status = HttpStatus.NOT_FOUND, exception = AccountNotFoundException.class)
  void accountCommand(@PathVariable("identifier") final String identifier, @RequestBody final AccountCommand accountCommand);

  @RequestMapping(
      value = "/journal",
      method = RequestMethod.POST,
      produces = {MediaType.APPLICATION_JSON_VALUE},
      consumes = {MediaType.APPLICATION_JSON_VALUE}
  )
  @ThrowsException(status = HttpStatus.BAD_REQUEST, exception = JournalEntryValidationException.class)
  void createJournalEntry(@RequestBody final JournalEntry journalEntry);

  @RequestMapping(
      value = "/journal",
      method = RequestMethod.GET,
      produces = {MediaType.ALL_VALUE},
      consumes = {MediaType.APPLICATION_JSON_VALUE}
  )
  List<JournalEntry> fetchJournalEntries(@RequestParam(value = "dateRange", required = false) final String dateRange);

  @RequestMapping(
      value = "/journal/{transactionIdentifier}",
      method = RequestMethod.GET,
      produces = {MediaType.ALL_VALUE},
      consumes = {MediaType.APPLICATION_JSON_VALUE}
  )
  @ThrowsException(status = HttpStatus.NOT_FOUND, exception = JournalEntryNotFoundException.class)
  JournalEntry findJournalEntry(@PathVariable("transactionIdentifier") final String transactionIdentifier);

  @RequestMapping(
      value = "/trialbalance",
      method = RequestMethod.GET,
      produces = {MediaType.ALL_VALUE},
      consumes = {MediaType.APPLICATION_JSON_VALUE}
  )
  TrialBalance getTrialBalance(
      @RequestParam(value = "includeEmptyEntries", required = false) final boolean includeEmptyEntries);

  @RequestMapping(
      value = "/chartofaccounts",
      method = RequestMethod.GET,
      produces = {MediaType.ALL_VALUE},
      consumes = {MediaType.APPLICATION_JSON_VALUE}
  )
  List<ChartOfAccountEntry> getChartOfAccounts();

  @RequestMapping(
      value = "/transactiontypes",
      method = RequestMethod.POST,
      produces = {MediaType.APPLICATION_JSON_VALUE},
      consumes = {MediaType.APPLICATION_JSON_VALUE}
  )
  @ThrowsExceptions({
      @ThrowsException(status = HttpStatus.CONFLICT, exception = TransactionTypeAlreadyExists.class),
      @ThrowsException(status = HttpStatus.BAD_REQUEST, exception = TransactionTypeValidationException.class)
  })
  void createTransactionType(@RequestBody @Valid final TransactionType transactionType);


  @RequestMapping(
      value = "/transactiontypes",
      method = RequestMethod.GET,
      produces = {MediaType.ALL_VALUE},
      consumes = {MediaType.APPLICATION_JSON_VALUE}
  )
  TransactionTypePage fetchTransactionTypes(@RequestParam(value = "term", required = false) final String term,
                                            @RequestParam(value = "pageIndex", required = false) final Integer pageIndex,
                                            @RequestParam(value = "size", required = false) final Integer size,
                                            @RequestParam(value = "sortColumn", required = false) final String sortColumn,
                                            @RequestParam(value = "sortDirection", required = false) final String sortDirection);

  @RequestMapping(
      value = "/transactiontypes/{code}",
      method = RequestMethod.PUT,
      produces = {MediaType.APPLICATION_JSON_VALUE},
      consumes = {MediaType.APPLICATION_JSON_VALUE}
  )
  @ThrowsExceptions({
      @ThrowsException(status = HttpStatus.NOT_FOUND, exception = TransactionTypeNotFoundException.class),
      @ThrowsException(status = HttpStatus.BAD_REQUEST, exception = TransactionTypeValidationException.class)
  })
  void changeTransactionType(@PathVariable("code") final String code,
                             @RequestBody @Valid final TransactionType transactionType);

  @RequestMapping(
      value = "/transactiontypes/{code}",
      method = RequestMethod.GET,
      produces = {MediaType.ALL_VALUE},
      consumes = {MediaType.APPLICATION_JSON_VALUE}
  )
  @ThrowsExceptions({
      @ThrowsException(status = HttpStatus.NOT_FOUND, exception = TransactionTypeNotFoundException.class)
  })
  TransactionType findTransactionType(@PathVariable("code") final String code);
}
