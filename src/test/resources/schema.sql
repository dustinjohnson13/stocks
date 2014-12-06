CREATE TABLE IF NOT EXISTS yahoo_stock_ticker(
    id INTEGER PRIMARY KEY GENERATED ALWAYS AS IDENTITY (start with 1),
    ticker VARCHAR(16) NOT NULL,
    name VARCHAR(100) NOT NULL,
    exchange VARCHAR(3) NOT NULL,
    category VARCHAR(50) DEFAULT NULL,
    country VARCHAR(50) NOT NULL,
    category_number INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS yahoo_stock_ticker_data(
  id INTEGER PRIMARY KEY GENERATED ALWAYS AS IDENTITY (start with 1),
  ticker_id INTEGER NOT NULL,
  date date NOT NULL,
  open INTEGER NOT NULL,
  close INTEGER NOT NULL,
  high INTEGER NOT NULL,
  low INTEGER NOT NULL,
  volume BIGINT NOT NULL,
  adjusted_close INTEGER NOT NULL,
  CONSTRAINT fk_ticker_id FOREIGN KEY (ticker_id) REFERENCES yahoo_stock_ticker(id)
);