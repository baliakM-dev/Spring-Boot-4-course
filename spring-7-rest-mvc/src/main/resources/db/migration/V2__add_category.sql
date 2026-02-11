drop table if exists beer_category;
drop table if exists category;

create table category
(
    category_id varchar(36) not null primary key,
    description varchar(255),
    created_at  timestamp(6),
    updated_at  timestamp(6),
    version     integer default null
);

create table beer_category
(
    beer_id     varchar(36) not null,
    category_id varchar(36) not null,
    primary key (beer_id, category_id),
    constraint fk_beer_category_beer foreign key (beer_id) references beer (beer_id) on delete cascade,
    constraint fk_beer_category_category foreign key (category_id) references category (category_id) on delete cascade
);