CREATE TABLE IF NOT EXISTS security(
    id INTEGER PRIMARY KEY GENERATED ALWAYS AS IDENTITY (start with 1),
    symbol VARCHAR(16) NOT NULL,
    name VARCHAR(100) NOT NULL,
    exchange VARCHAR(3) NOT NULL,
    category VARCHAR(50) DEFAULT NULL,
    country VARCHAR(50) NOT NULL,
    category_number INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS security_daily_data(
  id INTEGER PRIMARY KEY GENERATED ALWAYS AS IDENTITY (start with 1),
  security_id INTEGER NOT NULL,
  date date NOT NULL,
  open INTEGER NOT NULL,
  close INTEGER NOT NULL,
  high INTEGER NOT NULL,
  low INTEGER NOT NULL,
  volume BIGINT NOT NULL,
  adjusted_close INTEGER NOT NULL,
  CONSTRAINT fk_security_id FOREIGN KEY (security_id) REFERENCES security(id)
);