--
-- Licensed to the Apache Software Foundation (ASF) under one
-- or more contributor license agreements.  See the NOTICE file
-- distributed with this work for additional information
-- regarding copyright ownership.  The ASF licenses this file
-- to you under the Apache License, Version 2.0 (the
-- "License"); you may not use this file except in compliance
-- with the License.  You may obtain a copy of the License at
--
--   http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing,
-- software distributed under the License is distributed on an
-- "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
-- KIND, either express or implied.  See the License for the
-- specific language governing permissions and limitations
-- under the License.
--

CREATE TABLE thoth_payroll_collections (
  id                    BIGSERIAL    NOT NULL,
  identifier            VARCHAR(32)  NOT NULL,
  source_account_number VARCHAR(34)  NOT NULL,
  created_by            VARCHAR(32)  NOT NULL,
  created_on            TIMESTAMP(3) NOT NULL,
  CONSTRAINT thoth_payroll_collections_pk PRIMARY KEY (id),
  CONSTRAINT thoth_pay_col_identifier_uq UNIQUE (identifier)
);

CREATE TABLE thoth_payroll_payments (
  id                    BIGSERIAL        NOT NULL,
  collection_identifier VARCHAR(32)   NOT NULL,
  customer_identifier   VARCHAR(34)   NOT NULL,
  employer              VARCHAR(256)  NOT NULL,
  salary                NUMERIC(15,5) NOT NULL,
  CONSTRAINT thoth_payroll_payments_pk PRIMARY KEY (id)
);

INSERT INTO thoth_tx_types (identifier, a_name) VALUES ('SALA', 'Payroll/Salary Payment');