-- Optimistic locking prevents concurrent responders from silently overwriting playbook state.
ALTER TABLE response_playbook_runs
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE response_playbook_step_runs
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
