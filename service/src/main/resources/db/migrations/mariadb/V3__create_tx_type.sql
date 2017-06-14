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

CREATE TABLE thoth_tx_types (
  id               BIGINT        NOT NULL AUTO_INCREMENT,
  identifier       VARCHAR(32)   NOT NULL,
  a_name           VARCHAR(256)  NOT NULL,
  description      VARCHAR(2048) NULL,
  CONSTRAINT thoth_tx_types_pk PRIMARY KEY (id),
  CONSTRAINT thoth_tx_types_identifier_uq UNIQUE (identifier)
);

INSERT INTO thoth_tx_types (identifier, a_name) VALUES ('ACCC', 'Account Closing');
INSERT INTO thoth_tx_types (identifier, a_name) VALUES ('ACCO', 'Account Opening');
INSERT INTO thoth_tx_types (identifier, a_name) VALUES ('ACCT', 'Account Transfer');
INSERT INTO thoth_tx_types (identifier, a_name) VALUES ('ACDT', 'ACH Credit');
INSERT INTO thoth_tx_types (identifier, a_name) VALUES ('ADBT', 'ACH Debit');
INSERT INTO thoth_tx_types (identifier, a_name) VALUES ('ADJT', 'Adjustments');
INSERT INTO thoth_tx_types (identifier, a_name) VALUES ('APAC', 'ACH Pre-Authorised');
INSERT INTO thoth_tx_types (identifier, a_name) VALUES ('ARET', 'ACH Return');
INSERT INTO thoth_tx_types (identifier, a_name) VALUES ('AREV', 'ACH Reversal');
INSERT INTO thoth_tx_types (identifier, a_name) VALUES ('ARPD', 'ARP Debit');
INSERT INTO thoth_tx_types (identifier, a_name) VALUES ('ASET', 'ACH Settlement');
INSERT INTO thoth_tx_types (identifier, a_name) VALUES ('ATXN', 'ACH Transaction');
INSERT INTO thoth_tx_types (identifier, a_name) VALUES ('BACT', 'Branch Account Transfer');
INSERT INTO thoth_tx_types (identifier, a_name) VALUES ('BBDD', 'SEPA B2B Direct Debit');
INSERT INTO thoth_tx_types (identifier, a_name) VALUES ('BCDP', 'Branch Deposit');
INSERT INTO thoth_tx_types (identifier, a_name) VALUES ('BCHQ', 'Branch Cheque');
INSERT INTO thoth_tx_types (identifier, a_name) VALUES ('BCWD', 'Branch Withdrawal');
INSERT INTO thoth_tx_types (identifier, a_name) VALUES ('CAJT', 'Credit Adjustments');
INSERT INTO thoth_tx_types (identifier, a_name) VALUES ('CASH', 'Cash Letter');
INSERT INTO thoth_tx_types (identifier, a_name) VALUES ('CCCH', 'Certified Customer Cheque');
INSERT INTO thoth_tx_types (identifier, a_name) VALUES ('CCHQ', 'Cheque');
INSERT INTO thoth_tx_types (identifier, a_name) VALUES ('CDIS', 'Controlled Disbursement');
INSERT INTO thoth_tx_types (identifier, a_name) VALUES ('CDPT', 'Cash Deposit');
INSERT INTO thoth_tx_types (identifier, a_name) VALUES ('CHRG', 'Charges');
INSERT INTO thoth_tx_types (identifier, a_name) VALUES ('CQRV', 'Cheque Reversal');
INSERT INTO thoth_tx_types (identifier, a_name) VALUES ('CRCQ', 'Crossed Cheque');
INSERT INTO thoth_tx_types (identifier, a_name) VALUES ('CWDL', 'Cash Withdrawal');
INSERT INTO thoth_tx_types (identifier, a_name) VALUES ('DAJT', 'Debit Adjustments');
INSERT INTO thoth_tx_types (identifier, a_name) VALUES ('DDWN', 'Drawdown');
INSERT INTO thoth_tx_types (identifier, a_name) VALUES ('DMCT', 'Domestic Credit Transfer');
INSERT INTO thoth_tx_types (identifier, a_name) VALUES ('DSBR', 'Controlled Disbursement');
INSERT INTO thoth_tx_types (identifier, a_name) VALUES ('ERTA', 'Exchange Rate Adjustment');
INSERT INTO thoth_tx_types (identifier, a_name) VALUES ('FEES', 'Fees');
INSERT INTO thoth_tx_types (identifier, a_name) VALUES ('ICCT', 'Intra Company Transfer');
INSERT INTO thoth_tx_types (identifier, a_name) VALUES ('INTR', 'Interests');
INSERT INTO thoth_tx_types (identifier, a_name) VALUES ('MIXD', 'Mixed Deposit');
INSERT INTO thoth_tx_types (identifier, a_name) VALUES ('MSCD', 'Miscellaneous Deposit');
INSERT INTO thoth_tx_types (identifier, a_name) VALUES ('NTAV', 'Not Available');
INSERT INTO thoth_tx_types (identifier, a_name) VALUES ('OPCQ', 'Open Cheque');
INSERT INTO thoth_tx_types (identifier, a_name) VALUES ('ORCQ', 'Order Cheque');
INSERT INTO thoth_tx_types (identifier, a_name) VALUES ('OTHR', 'Other');
INSERT INTO thoth_tx_types (identifier, a_name) VALUES ('PADD', 'Pre-Authorised Direct Debit');
INSERT INTO thoth_tx_types (identifier, a_name) VALUES ('PMDD', 'Direct Debit Payment');
INSERT INTO thoth_tx_types (identifier, a_name) VALUES ('POSC', 'Credit Card Payment');
INSERT INTO thoth_tx_types (identifier, a_name) VALUES ('POSD', 'Point-of-Sale Payment Debit Card');
INSERT INTO thoth_tx_types (identifier, a_name) VALUES ('POSP', 'Point-of-Sale Payment');
INSERT INTO thoth_tx_types (identifier, a_name) VALUES ('PPAY', 'Principal Payment');
INSERT INTO thoth_tx_types (identifier, a_name) VALUES ('PSTE', 'Posting Error');
INSERT INTO thoth_tx_types (identifier, a_name) VALUES ('RCDD', 'Reversal Due To Payment Cancellation Request');
INSERT INTO thoth_tx_types (identifier, a_name) VALUES ('RIMB', 'Reimbursement');
INSERT INTO thoth_tx_types (identifier, a_name) VALUES ('RPCR', 'Reversal Due To Payment Cancellation Request');
INSERT INTO thoth_tx_types (identifier, a_name) VALUES ('SMRT', 'Smart-Card Payment');
INSERT INTO thoth_tx_types (identifier, a_name) VALUES ('TAXE', 'Taxes');
INSERT INTO thoth_tx_types (identifier, a_name) VALUES ('YTDA', 'YTD Adjustment');
INSERT INTO thoth_tx_types (identifier, a_name) VALUES ('ZABA', 'Zero Balancing');
