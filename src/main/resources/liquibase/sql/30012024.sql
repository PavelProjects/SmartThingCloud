create sequence main_id_sequence;

create function getnextid() returns char(8) as
    $$ declare
      str text :=  '0123456789abcdefghijklmnopqrstuvwxyz';
      val bigint;
      id_ text;
      mod int;
      begin
      val:=nextval('main_id_sequence');
      id_:='';
      while (length(id_) < 8) loop
        mod = val % 36;
        id_:=substring(str,mod+1,1)||id_;
        val = val / 36;
      end loop;
      return id_;
      return 'null';
      end;   $$
language plpgsql;

create table smt_user (
    id char(8) primary key default getnextid(),
    login varchar(32) not null unique,
    password varchar(128) not null,
    creation_date timestamp with time zone not null default now()
);

create table smt_user_role (
    id char(8) primary key default getnextid(),
    creation_date timestamp with time zone not null default now(),
    user_id char(8) not null references smt_user(id),
    role varchar(32) not null,
    unique(user_id, role)
);

create table smt_gateway (
    id char(8) primary key default getnextid(),
    creation_date timestamp with time zone not null default now(),
    name varchar(32) not null,
    description varchar(32)
);

create table smt_gateway_config (
    id char(8) primary key default getnextid(),
    creation_date timestamp with time zone not null default now(),
    gateway_id char(8) not null references smt_gateway(id),
    broker_ip varchar(128) not null,
    broker_port int not null,
    queue_in varchar(64) not null,
    queue_out varchar(64) not null
);

create table smt_gateway_owner (
    id char(8) primary key default getnextid(),
    creation_date timestamp with time zone not null default now(),
    user_id char(8) not null references smt_user(id),
    gateway_id char(8) not null references smt_gateway(id),
    unique(user_id, gateway_id)
);

create table smt_gateway_request (
    id char(8) primary key default getnextid(),
    sent_date timestamp with time zone not null default now(),
    receive_date timestamp with time zone,
    user_id char(8) not null references smt_user(id),
    gateway_id char(8) not null references smt_gateway(id),
    target char(32),
    message text,
    result text,
    finished boolean not null default false,
    success boolean
);

--rollback drop table _user, _user_credits, _comment, _notification, _task, _user_task, dict_notify_type, dict_task_status;
 -- drop sequence main_id_sequence;
 -- drop function getnextid;
