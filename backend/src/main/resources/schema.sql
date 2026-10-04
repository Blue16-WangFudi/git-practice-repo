CREATE TABLE IF NOT EXISTS server_snapshots (
    server_id VARCHAR(128) NOT NULL PRIMARY KEY,
    hostname VARCHAR(255),
    platform_name VARCHAR(255),
    collected_at TIMESTAMP(3) NULL,
    received_at TIMESTAMP(3) NULL,
    cpu_usage_percent DOUBLE,
    load1 DOUBLE,
    memory_used_bytes BIGINT,
    memory_total_bytes BIGINT,
    disk_used_bytes BIGINT,
    disk_total_bytes BIGINT,
    network_rx_bytes BIGINT,
    network_tx_bytes BIGINT,
    containers_json LONGTEXT,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS status_incidents (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    incident_status VARCHAR(32) NOT NULL,
    impact VARCHAR(32) NOT NULL,
    message TEXT,
    started_at TIMESTAMP(3) NOT NULL,
    updated_at TIMESTAMP(3) NOT NULL,
    resolved_at TIMESTAMP(3) NULL
);

CREATE TABLE IF NOT EXISTS status_subscriptions (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(320) NOT NULL UNIQUE,
    subscription_status VARCHAR(32) NOT NULL DEFAULT 'active',
    created_at TIMESTAMP(3) NOT NULL,
    updated_at TIMESTAMP(3) NOT NULL
);

CREATE TABLE IF NOT EXISTS monitored_services (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    service_key VARCHAR(128) NOT NULL UNIQUE,
    service_name VARCHAR(255) NOT NULL,
    group_name VARCHAR(128) NOT NULL,
    endpoint_url VARCHAR(500),
    description VARCHAR(1000),
    service_status VARCHAR(32) NOT NULL DEFAULT 'operational',
    created_at TIMESTAMP(3) NOT NULL,
    updated_at TIMESTAMP(3) NOT NULL
);
