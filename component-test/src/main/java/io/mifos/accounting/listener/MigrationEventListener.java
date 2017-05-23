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
public class MigrationEventListener {

  private final Logger logger;
  private final EventRecorder eventRecorder;

  @SuppressWarnings("SpringJavaAutowiringInspection")
  @Autowired
  public MigrationEventListener(final @Qualifier(AbstractAccountingTest.TEST_LOGGER) Logger logger, final EventRecorder eventRecorder) {
    super();
    this.logger = logger;
    this.eventRecorder = eventRecorder;
  }

  @JmsListener(
      destination = EventConstants.DESTINATION,
      selector = EventConstants.SELECTOR_INITIALIZE,
      subscription = EventConstants.DESTINATION
  )
  public void onInitialization(@Header(TenantHeaderFilter.TENANT_HEADER) final String tenant,
                               final String payload) {
    this.logger.debug("Provisioning finished.");
    this.eventRecorder.event(tenant, EventConstants.INITIALIZE, payload, String.class);
  }
}
