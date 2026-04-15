-- =============================================================================
-- V1__create_order_schema.sql
-- NOTE: This file uses Flyway naming conventions (V1__ prefix, db/migration path).
--       The project's active migration tool is Liquibase (db/changelog/).
--       This script is a standalone SQL reference and is not executed automatically.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- customers
-- -----------------------------------------------------------------------------
CREATE TABLE customers (
    id          UUID         NOT NULL,
    email       VARCHAR(254) NOT NULL,
    full_name   VARCHAR(255) NOT NULL,
    phone       VARCHAR(30),
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_customers PRIMARY KEY (id),
    CONSTRAINT uq_customers_email UNIQUE (email)
);

-- -----------------------------------------------------------------------------
-- orders
-- -----------------------------------------------------------------------------
CREATE TABLE orders (
    id               UUID           NOT NULL,
    order_number     VARCHAR(12)    NOT NULL,
    customer_id      UUID           NOT NULL,
    status           VARCHAR(20)    NOT NULL,
    total_amount     DECIMAL(12, 2) NOT NULL,
    shipping_street  VARCHAR(255),
    shipping_city    VARCHAR(100),
    shipping_state   VARCHAR(100),
    shipping_zip_code VARCHAR(20),
    shipping_country VARCHAR(100),
    version          BIGINT         NOT NULL DEFAULT 0,
    created_at       TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP      NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_orders         PRIMARY KEY (id),
    CONSTRAINT uq_orders_order_number UNIQUE (order_number),
    CONSTRAINT fk_orders_customer_id  FOREIGN KEY (customer_id) REFERENCES customers (id)
);

CREATE INDEX idx_orders_order_number ON orders (order_number);
CREATE INDEX idx_orders_customer_id  ON orders (customer_id);
CREATE INDEX idx_orders_status       ON orders (status);

-- -----------------------------------------------------------------------------
-- order_items
-- -----------------------------------------------------------------------------
CREATE TABLE order_items (
    id           UUID           NOT NULL,
    order_id     UUID           NOT NULL,
    product_id   VARCHAR(255)   NOT NULL,
    product_name VARCHAR(255)   NOT NULL,
    quantity     INT            NOT NULL,
    unit_price   DECIMAL(10, 2) NOT NULL,
    subtotal     DECIMAL(10, 2) NOT NULL,
    created_at   TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP      NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_order_items         PRIMARY KEY (id),
    CONSTRAINT fk_order_items_order_id FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT chk_order_items_quantity CHECK (quantity > 0)
);

CREATE INDEX idx_order_items_order_id ON order_items (order_id);
