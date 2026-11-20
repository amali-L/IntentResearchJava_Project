CREATE DATABASE IF NOT EXISTS intent_management CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE intent_management;
CREATE TABLE IF NOT EXISTS users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(80) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  role ENUM('ADMIN','OPERATOR','VIEWER') NOT NULL DEFAULT 'OPERATOR',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS intents (
  id VARCHAR(64) PRIMARY KEY,
  raw_text TEXT NOT NULL,
  service_type VARCHAR(80) NOT NULL,
  status VARCHAR(32) NOT NULL,
  target_latency_ms DOUBLE NOT NULL,
  required_bandwidth_mbps DOUBLE NOT NULL,
  max_packet_loss_percent DOUBLE NOT NULL,
  priority VARCHAR(32) NOT NULL,
  explanation TEXT,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS deployments (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  intent_id VARCHAR(64) NOT NULL,
  allocated_bandwidth_mbps DOUBLE NOT NULL,
  deployment_status VARCHAR(32) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (intent_id) REFERENCES intents(id)
);
CREATE TABLE IF NOT EXISTS monitoring_metrics (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  intent_id VARCHAR(64) NOT NULL,
  latency_ms DOUBLE NOT NULL,
  throughput_mbps DOUBLE NOT NULL,
  packet_loss_percent DOUBLE NOT NULL,
  measured_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (intent_id) REFERENCES intents(id)
);
CREATE TABLE IF NOT EXISTS conflict_events (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  intent_id VARCHAR(64) NOT NULL,
  conflict_type VARCHAR(80) NOT NULL,
  explanation TEXT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (intent_id) REFERENCES intents(id)
);
CREATE TABLE IF NOT EXISTS optimization_runs (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  algorithm VARCHAR(40) NOT NULL,
  initial_loss DOUBLE,
  final_loss DOUBLE,
  iterations INT NOT NULL,
  configuration_json JSON,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
