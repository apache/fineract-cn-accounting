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
package io.mifos.accounting.listener;

import io.mifos.accounting.AbstractAccountingTest;
import io.mifos.accounting.api.v1.EventConstants;
import io.mifos.core.lang.config.TenantHeaderFilter;
import io.mifos.core.test.listener.EventRecorder;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@SuppressWarnings("unused")
@Component
public class AccountEventListener {

  private final Logger logger;
  private final EventRecorder eventRecorder;

  @SuppressWarnings("SpringJavaAutowiringInspection")
  @Autowired
  public AccountEventListener(final @Qualifier(AbstractAccountingTest.TEST_LOGGER) Logger logger, final EventRecorder eventRecorder) {
    super();
    this.logger = logger;
    this.eventRecorder = eventRecorder;
  }

  @JmsListener(
      destination = EventConstants.DESTINATION,
      selector = EventConstants.SELECTOR_POST_ACCOUNT,
      subscription = EventConstants.DESTINATION
  )
  public void onCreateAccount(@Header(TenantHeaderFilter.TENANT_HEADER) final String tenant,
                           final String payload) {
    this.logger.debug("Account created.");
    this.eventRecorder.event(tenant, EventConstants.POST_ACCOUNT, payload, String.class);
  }

  @JmsListener(
      destination = EventConstants.DESTINATION,
      selector = EventConstants.SELECTOR_PUT_ACCOUNT,
      subscription = EventConstants.DESTINATION
  )
  public void onChangeAccount(@Header(TenantHeaderFilter.TENANT_HEADER) final String tenant,
                          final String payload) {
    this.logger.debug("Account modified.");
    this.eventRecorder.event(tenant, EventConstants.PUT_ACCOUNT, payload, String.class);
  }

  @JmsListener(
      destination = EventConstants.DESTINATION,
      selector = EventConstants.SELECTOR_CLOSE_ACCOUNT,
      subscription = EventConstants.DESTINATION
  )
  public void onCloseAccount(@Header(TenantHeaderFilter.TENANT_HEADER) final String tenant,
                            final String payload) {
    this.logger.debug("Account closed.");
    this.eventRecorder.event(tenant, EventConstants.CLOSE_ACCOUNT, payload, String.class);
  }

  @JmsListener(
      destination = EventConstants.DESTINATION,
      selector = EventConstants.SELECTOR_LOCK_ACCOUNT,
      subscription = EventConstants.DESTINATION
  )
  public void onLockAccount(@Header(TenantHeaderFilter.TENANT_HEADER) final String tenant,
                           final String payload) {
    this.logger.debug("Account locked.");
    this.eventRecorder.event(tenant, EventConstants.LOCK_ACCOUNT, payload, String.class);
  }

  @JmsListener(
      destination = EventConstants.DESTINATION,
      selector = EventConstants.SELECTOR_UNLOCK_ACCOUNT,
      subscription = EventConstants.DESTINATION
  )
  public void onUnlockAccount(@Header(TenantHeaderFilter.TENANT_HEADER) final String tenant,
                             final String payload) {
    this.logger.debug("Account unlocked.");
    this.eventRecorder.event(tenant, EventConstants.UNLOCK_ACCOUNT, payload, String.class);
  }

  @JmsListener(
      destination = EventConstants.DESTINATION,
      selector = EventConstants.SELECTOR_REOPEN_ACCOUNT,
      subscription = EventConstants.DESTINATION
  )
  public void onReopenAccount(@Header(TenantHeaderFilter.TENANT_HEADER) final String tenant,
                             final String payload) {
    this.logger.debug("Account reopened.");
    this.eventRecorder.event(tenant, EventConstants.REOPEN_ACCOUNT, payload, String.class);
  }

  @JmsListener(
      destination = EventConstants.DESTINATION,
      selector = EventConstants.SELECTOR_DELETE_ACCOUNT,
      subscription = EventConstants.DESTINATION
  )
  public void onDeleteAccount(@Header(TenantHeaderFilter.TENANT_HEADER) final String tenant,
                              final String payload) {
    this.logger.debug("Account deleted.");
    this.eventRecorder.event(tenant, EventConstants.DELETE_ACCOUNT, payload, String.class);
  }
}
