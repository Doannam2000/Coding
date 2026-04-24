create table if not exists bot_tasks (
  id bigserial primary key,
  repo text not null,
  title text not null,
  prompt text not null,
  config_md_path text default 'docs/ai-spec.md',
  cron_expr text,
  status text not null default 'queued',
  current_step int not null default 0,
  branch text,
  pr_url text,
  created_by bigint not null,
  approved_by bigint,
  next_run_at timestamptz,
  last_error text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index if not exists idx_bot_tasks_status_next_run
  on bot_tasks(status, next_run_at);

create table if not exists bot_task_steps (
  id bigserial primary key,
  task_id bigint not null references bot_tasks(id) on delete cascade,
  step_no int not null,
  title text not null,
  instruction text not null,
  status text not null default 'pending',
  retries int not null default 0,
  test_command text,
  build_command text,
  started_at timestamptz,
  finished_at timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique(task_id, step_no)
);

create index if not exists idx_bot_task_steps_state
  on bot_task_steps(task_id, status, step_no);

create table if not exists bot_task_logs (
  id bigserial primary key,
  task_id bigint not null references bot_tasks(id) on delete cascade,
  step_no int,
  level text not null,
  message text not null,
  raw jsonb,
  created_at timestamptz not null default now()
);

create table if not exists bot_settings (
  key text primary key,
  value text not null,
  updated_at timestamptz not null default now()
);

insert into bot_settings(key, value)
values ('max_retries_per_step', '2')
on conflict (key) do nothing;

insert into bot_settings(key, value)
values ('worker_concurrency', '1')
on conflict (key) do nothing;

insert into bot_settings(key, value)
values ('task_timeout_minutes', '30')
on conflict (key) do nothing;

insert into bot_settings(key, value)
values ('retry_delay_minutes', '1')
on conflict (key) do nothing;
