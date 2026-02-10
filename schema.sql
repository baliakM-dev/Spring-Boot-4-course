
    create table beer (
        beer_style tinyint check ((beer_style between 0 and 8)),
        price decimal(38,2) not null,
        quantity_on_hand integer,
        version integer,
        created_at datetime(6),
        updated_at datetime(6),
        beer_id VARCHAR(36) not null,
        beer_name varchar(50) not null,
        upc varchar(50) not null,
        primary key (beer_id)
    ) engine=InnoDB;

    create table customers (
        version integer,
        created_at datetime(6),
        updated_at datetime(6),
        customer_id VARCHAR(36) not null,
        name varchar(100) not null,
        primary key (customer_id)
    ) engine=InnoDB;

    alter table beer 
       add constraint uk_beer_name unique (beer_name);

    alter table customers 
       add constraint uk_customer_name unique (name);

    create table beer (
        beer_style tinyint check ((beer_style between 0 and 8)),
        price decimal(38,2) not null,
        quantity_on_hand integer,
        version integer,
        created_at datetime(6),
        updated_at datetime(6),
        beer_id VARCHAR(36) not null,
        beer_name varchar(50) not null,
        upc varchar(50) not null,
        primary key (beer_id)
    ) engine=InnoDB;

    create table customers (
        version integer,
        created_at datetime(6),
        updated_at datetime(6),
        customer_id VARCHAR(36) not null,
        name varchar(100) not null,
        primary key (customer_id)
    ) engine=InnoDB;

    alter table beer 
       add constraint uk_beer_name unique (beer_name);

    alter table customers 
       add constraint uk_customer_name unique (name);

    create table beer (
        beer_style tinyint check ((beer_style between 0 and 8)),
        price decimal(38,2) not null,
        quantity_on_hand integer,
        version integer,
        created_at datetime(6),
        updated_at datetime(6),
        beer_id VARCHAR(36) not null,
        beer_name varchar(50) not null,
        upc varchar(50) not null,
        primary key (beer_id)
    ) engine=InnoDB;

    create table customers (
        version integer,
        created_at datetime(6),
        updated_at datetime(6),
        customer_id VARCHAR(36) not null,
        name varchar(100) not null,
        primary key (customer_id)
    ) engine=InnoDB;

    alter table beer 
       add constraint uk_beer_name unique (beer_name);

    alter table customers 
       add constraint uk_customer_name unique (name);

    create table beer (
        beer_style tinyint check ((beer_style between 0 and 8)),
        price decimal(38,2) not null,
        quantity_on_hand integer,
        version integer,
        created_at datetime(6),
        updated_at datetime(6),
        beer_id VARCHAR(36) not null,
        beer_name varchar(50) not null,
        upc varchar(50) not null,
        primary key (beer_id)
    ) engine=InnoDB;

    create table customers (
        version integer,
        created_at datetime(6),
        updated_at datetime(6),
        customer_id VARCHAR(36) not null,
        name varchar(100) not null,
        primary key (customer_id)
    ) engine=InnoDB;

    alter table beer 
       add constraint uk_beer_name unique (beer_name);

    alter table customers 
       add constraint uk_customer_name unique (name);

    create table beer (
        beer_style tinyint check ((beer_style between 0 and 8)),
        price decimal(38,2) not null,
        quantity_on_hand integer,
        version integer,
        created_at datetime(6),
        updated_at datetime(6),
        beer_id VARCHAR(36) not null,
        beer_name varchar(50) not null,
        upc varchar(50) not null,
        primary key (beer_id)
    ) engine=InnoDB;

    create table customers (
        version integer,
        created_at datetime(6),
        updated_at datetime(6),
        customer_id VARCHAR(36) not null,
        name varchar(100) not null,
        primary key (customer_id)
    ) engine=InnoDB;

    alter table beer 
       add constraint uk_beer_name unique (beer_name);

    alter table customers 
       add constraint uk_customer_name unique (name);

    create table beer (
        beer_style tinyint check ((beer_style between 0 and 8)),
        price decimal(38,2) not null,
        quantity_on_hand integer,
        version integer,
        created_at datetime(6),
        updated_at datetime(6),
        beer_id VARCHAR(36) not null,
        beer_name varchar(50) not null,
        upc varchar(50) not null,
        primary key (beer_id)
    ) engine=InnoDB;

    create table customers (
        version integer,
        created_at datetime(6),
        updated_at datetime(6),
        customer_id VARCHAR(36) not null,
        name varchar(100) not null,
        primary key (customer_id)
    ) engine=InnoDB;

    alter table beer 
       add constraint uk_beer_name unique (beer_name);

    alter table customers 
       add constraint uk_customer_name unique (name);

    create table beer (
        beer_style tinyint check ((beer_style between 0 and 8)),
        price decimal(38,2) not null,
        quantity_on_hand integer,
        version integer,
        created_at datetime(6),
        updated_at datetime(6),
        beer_id VARCHAR(36) not null,
        beer_name varchar(50) not null,
        upc varchar(50) not null,
        primary key (beer_id)
    ) engine=InnoDB;

    create table customers (
        version integer,
        created_at datetime(6),
        updated_at datetime(6),
        customer_id VARCHAR(36) not null,
        name varchar(100) not null,
        primary key (customer_id)
    ) engine=InnoDB;

    alter table beer 
       add constraint uk_beer_name unique (beer_name);

    alter table customers 
       add constraint uk_customer_name unique (name);
