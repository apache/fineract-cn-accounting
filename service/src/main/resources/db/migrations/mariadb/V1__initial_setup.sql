--
-- Copyright 2017 The Mifos Initiative.
--
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
--
--    http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.
--

CREATE TABLE thoth_ledgers (
  id               BIGINT        NOT NULL AUTO_INCREMENT,
  a_type           VARCHAR(32)   NOT NULL,
  identifier       VARCHAR(32)    NOT NULL,
  a_name           VARCHAR(256)  NOT NULL,
  description      VARCHAR(2048) NULL,
  parent_ledger_id BIGINT        NULL,
  created_on       TIMESTAMP(3)  NOT NULL,
  created_by       VARCHAR(32)    NOT NULL,
  last_modified_on TIMESTAMP(3)  NULL,
  last_modified_by VARCHAR(32)    NULL,
  CONSTRAINT thoth_ledgers_pk PRIMARY KEY (id),
  CONSTRAINT thoth_ledgers_identifier_uq UNIQUE (identifier),
  CONSTRAINT thoth_ledgers_parent_fk FOREIGN KEY (parent_ledger_id) REFERENCES thoth_ledgers (id)
);

CREATE TABLE thoth_accounts (
  id                    BIGINT         NOT NULL AUTO_INCREMENT,
  a_type                VARCHAR(32)    NOT NULL,
  identifier            VARCHAR(32)    NOT NULL,
  a_name                VARCHAR(256)   NOT NULL,
  holders               VARCHAR(256)   NULL,
  signature_authorities VARCHAR(256)   NULL,
  balance               NUMERIC(15, 5) NOT NULL,
  reference_account_id  BIGINT         NULL,
  ledger_id             BIGINT         NOT NULL,
  a_state               VARCHAR(32)    NOT NULL,
  created_on            TIMESTAMP(3)   NOT NULL,
  created_by            VARCHAR(32)    NOT NULL,
  last_modified_on      TIMESTAMP(3)   NULL,
  last_modified_by      VARCHAR(32)    NULL,
  CONSTRAINT thoth_accounts_pk PRIMARY KEY (id),
  CONSTRAINT thoth_accounts_identifier_uq UNIQUE (identifier),
  CONSTRAINT thoth_reference_accounts_fk FOREIGN KEY (reference_account_id) REFERENCES thoth_accounts (id),
  CONSTRAINT thoth_accounts_ledgers_fk FOREIGN KEY (ledger_id) REFERENCES thoth_ledgers (id)
);

CREATE TABLE thoth_account_entries (
  id               BIGINT         NOT NULL AUTO_INCREMENT,
  account_id       BIGINT         NULL,
  a_type           VARCHAR(32)    NOT NULL,
  transaction_date TIMESTAMP(3)   NOT NULL,
  message          VARCHAR(2048)  NULL,
  amount           NUMERIC(15, 5) NOT NULL,
  balance          NUMERIC(15, 5) NOT NULL,
  CONSTRAINT thoth_account_entries_pk PRIMARY KEY (id),
  CONSTRAINT thoth_account_entries_accounts_fk FOREIGN KEY (account_id) REFERENCES thoth_accounts (id)
);

CREATE TABLE thoth_commands (
  id         BIGINT       NOT NULL AUTO_INCREMENT,
  account_id BIGINT       NOT NULL,
  a_type     VARCHAR(32)  NOT NULL,
  a_comment  VARCHAR(32)  NULL,
  created_by VARCHAR(32)   NOT NULL,
  created_on TIMESTAMP(3) NULL,
  CONSTRAINT thoth_commands_pk PRIMARY KEY (id),
  CONSTRAINT thoth_commands_accounts_fk FOREIGN KEY (account_id) REFERENCES thoth_accounts (id)
    ON UPDATE RESTRICT
);