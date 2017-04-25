ALTER TABLE thoth_ledgers ADD COLUMN show_accounts_in_chart BOOLEAN NOT NULL DEFAULT 1;

ALTER TABLE thoth_ledgers ALTER COLUMN show_accounts_in_chart DROP DEFAULT;