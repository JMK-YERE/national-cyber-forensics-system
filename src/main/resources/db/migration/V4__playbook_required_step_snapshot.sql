-- Preserve whether a snapshotted playbook step was marked required in its catalog version.
ALTER TABLE response_playbook_step_runs
    ADD COLUMN IF NOT EXISTS required BOOLEAN NOT NULL DEFAULT FALSE;
