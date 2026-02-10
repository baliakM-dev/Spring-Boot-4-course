drop table if exists beer cascade ;
drop table if exists customers cascade;

create table beer
(
    beer_style       tinyint check ((beer_style between 0 and 9)),
    price            numeric(38, 2) not null,
    quantity_on_hand integer,
    version          integer,
    created_at       timestamp(6),
    updated_at       timestamp(6),
    beer_id          VARCHAR(36)    not null,
    beer_name        varchar(50)    not null,
    upc              varchar(50)    not null,
    primary key (beer_id)
);
create table customers
(
    version     integer,
    created_at  timestamp(6),
    updated_at  timestamp(6),
    customer_id VARCHAR(36)  not null,
    name        varchar(100) not null,
    primary key (customer_id),
    constraint uk_customer_name unique (name)
);
