CREATE SEQUENCE categories_id_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE products_id_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE orders_id_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE order_items_id_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE categories (
    id BIGINT DEFAULT nextval('categories_id_seq') PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    CONSTRAINT ck_categories_id CHECK (id > 0),
    CONSTRAINT ck_categories_name CHECK (name ~ '[^[:space:]]')
);

CREATE TABLE products (
    id BIGINT DEFAULT nextval('products_id_seq') PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(2000),
    price NUMERIC(12, 2) NOT NULL,
    category_id BIGINT NOT NULL,
    stock_quantity INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT ck_products_id CHECK (id > 0),
    CONSTRAINT ck_products_name CHECK (char_length(name) >= 3 AND name ~ '[^[:space:]]'),
    CONSTRAINT ck_products_price CHECK (price BETWEEN 0.01 AND 9999999999.99),
    CONSTRAINT ck_products_stock CHECK (stock_quantity >= 0),
    CONSTRAINT fk_products_category FOREIGN KEY (category_id)
        REFERENCES categories (id) ON DELETE RESTRICT
);

CREATE TABLE orders (
    id BIGINT DEFAULT nextval('orders_id_seq') PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_orders_id CHECK (id > 0)
);

CREATE TABLE order_items (
    id BIGINT DEFAULT nextval('order_items_id_seq') PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    product_name VARCHAR(120) NOT NULL,
    unit_price NUMERIC(12, 2) NOT NULL,
    quantity INTEGER NOT NULL,
    CONSTRAINT ck_order_items_id CHECK (id > 0),
    CONSTRAINT ck_order_items_name CHECK (char_length(product_name) >= 3 AND product_name ~ '[^[:space:]]'),
    CONSTRAINT ck_order_items_price CHECK (unit_price BETWEEN 0.01 AND 9999999999.99),
    CONSTRAINT ck_order_items_quantity CHECK (quantity > 0),
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id)
        REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_order_items_product FOREIGN KEY (product_id)
        REFERENCES products (id) ON DELETE RESTRICT
);

ALTER SEQUENCE categories_id_seq OWNED BY categories.id;
ALTER SEQUENCE products_id_seq OWNED BY products.id;
ALTER SEQUENCE orders_id_seq OWNED BY orders.id;
ALTER SEQUENCE order_items_id_seq OWNED BY order_items.id;
