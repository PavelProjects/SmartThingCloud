create table smt_gateway_token (
    id char(8) primary key default getnextid(),
    gateway_id char(8) not null references smt_gateway(id) unique,
    owner_id char(8) not null references smt_user(id),
    creation_date timestamp with time zone not null
);