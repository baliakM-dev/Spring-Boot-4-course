drop table if exists beer_category;
drop table if exists category;

create table categories
(
    category_id varchar(36) not null primary key,
    description varchar(255) not null,
    created_at  timestamp(6),
    updated_at  timestamp(6),
    version     integer default null,
    constraint uq_category_description unique (description)
);

create table beer_category
(
    beer_id     varchar(36) not null,
    category_id varchar(36) not null,
    primary key (beer_id, category_id),
    constraint fk_beer_category_beer foreign key (beer_id) references beers (beer_id) on delete cascade,
    constraint fk_beer_category_category foreign key (category_id) references categories (category_id) on delete cascade
);