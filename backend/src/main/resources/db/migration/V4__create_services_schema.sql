CREATE TABLE services (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    current_plan VARCHAR(255),
    price DECIMAL(10, 2),
    billing_frequency VARCHAR(50),
    renewal_date DATE,
    official_url VARCHAR(2000),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
