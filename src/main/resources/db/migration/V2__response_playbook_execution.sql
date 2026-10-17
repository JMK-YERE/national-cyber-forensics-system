-- Persisted response playbook executions. Existing cases remain unchanged.
CREATE TABLE IF NOT EXISTS response_playbook_runs (
    id BIGSERIAL PRIMARY KEY,
    case_id BIGINT NOT NULL,
    playbook_slug VARCHAR(80) NOT NULL,
    playbook_version VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
    started_by BIGINT,
    started_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMPTZ,
    lock_version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_response_playbook_run_status CHECK (status IN ('IN_PROGRESS', 'COMPLETED', 'CANCELLED'))
);

CREATE TABLE IF NOT EXISTS response_playbook_step_runs (
    id BIGSERIAL PRIMARY KEY,
    run_id BIGINT NOT NULL REFERENCES response_playbook_runs(id) ON DELETE RESTRICT,
    step_order INTEGER NOT NULL,
    phase VARCHAR(40) NOT NULL,
    title VARCHAR(200) NOT NULL,
    guidance TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    completion_notes TEXT,
    completed_by BIGINT,
    completed_at TIMESTAMPTZ,
    lock_version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_response_playbook_step_order UNIQUE (run_id, step_order),
    CONSTRAINT ck_response_playbook_step_status CHECK (status IN ('PENDING', 'COMPLETED', 'SKIPPED')),
    CONSTRAINT ck_response_playbook_step_order CHECK (step_order > 0)
);

CREATE INDEX IF NOT EXISTS ix_playbook_runs_case_started ON response_playbook_runs (case_id, started_at DESC);
CREATE INDEX IF NOT EXISTS ix_playbook_steps_run_order ON response_playbook_step_runs (run_id, step_order);

-- Flyway runs before Hibernate on fresh installs, so legacy tables are linked conditionally.
DO $$
BEGIN
    IF to_regclass('public.case_files') IS NOT NULL
       AND NOT EXISTS (
           SELECT 1 FROM pg_constraint
           WHERE conname = 'fk_response_playbook_case'
             AND conrelid = 'public.response_playbook_runs'::regclass
       ) THEN
        ALTER TABLE public.response_playbook_runs
            ADD CONSTRAINT fk_response_playbook_case
            FOREIGN KEY (case_id) REFERENCES public.case_files(id) ON DELETE RESTRICT;
    END IF;

    IF to_regclass('public.users') IS NOT NULL THEN
        IF NOT EXISTS (
            SELECT 1 FROM pg_constraint
            WHERE conname = 'fk_response_playbook_started_by'
              AND conrelid = 'public.response_playbook_runs'::regclass
        ) THEN
            ALTER TABLE public.response_playbook_runs
                ADD CONSTRAINT fk_response_playbook_started_by
                FOREIGN KEY (started_by) REFERENCES public.users(id) ON DELETE SET NULL;
        END IF;

        IF NOT EXISTS (
            SELECT 1 FROM pg_constraint
            WHERE conname = 'fk_response_playbook_completed_by'
              AND conrelid = 'public.response_playbook_step_runs'::regclass
        ) THEN
            ALTER TABLE public.response_playbook_step_runs
                ADD CONSTRAINT fk_response_playbook_completed_by
                FOREIGN KEY (completed_by) REFERENCES public.users(id) ON DELETE SET NULL;
        END IF;
    END IF;
END $$;
