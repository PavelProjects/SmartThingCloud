create table smt_user_token (
    id char(8) primary key default getnextid(),
    creation_date timestamp with time zone not null default now(),
    user_id char(8) not null references smt_user(id)
);