begin;

--drop table _user, _user_credits, _comment, _notification, _task, _user_task, dict_notify_type, dict_task_status;
--drop sequence main_id_sequence;
--drop function getnextid;

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

create table smt_gateway (
    id char(8) primary key default getnextid(),
    creation_date timestamp with time zone not null default now(),
    name varchar(32) not null,
    description varchar(32) not null
);

create table smt_gateway_owner (
    id char(8) primary key default getnextid(),
    creation_date timestamp with time zone not null default now(),
    user_id char(8) not null references smt_user(id),
    gateway_id char(8) not null references smt_gateway(id),
    unique(user_id, gateway_id)
);


create table smt_token_info (
    id char(8) primary key default getnextid(),
    creation_date timestamp with time zone not null default now(),
    deactivation_date timestamp with time zone,
    active boolean not null default false,
    type char(16) not null,
    owner_id char(8) not null references smt_user(id),
    gateway_id char(8) references smt_gateway(id)
);

commit;