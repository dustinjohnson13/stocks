TRUNCATE SCHEMA public AND COMMIT;

CREATE TABLE IF NOT EXISTS security(
    id INTEGER PRIMARY KEY GENERATED ALWAYS AS IDENTITY (start with 1),
    symbol VARCHAR(16) NOT NULL,
    exchange VARCHAR(3) NOT NULL,
    CONSTRAINT unique_symbol_exchange UNIQUE (symbol, exchange)
);

CREATE TABLE IF NOT EXISTS security_details(
    id INTEGER PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    category VARCHAR(50) DEFAULT NULL,
    country VARCHAR(50) NOT NULL,
    category_number INTEGER NOT NULL,
    CONSTRAINT fk_id_security_id FOREIGN KEY (id) REFERENCES security(id)
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
  CONSTRAINT fk_security_id FOREIGN KEY (security_id) REFERENCES security(id),
  CONSTRAINT unique_date_security UNIQUE (date, security_id)
);

CREATE TABLE IF NOT EXISTS security_daily_metrics(
  id INTEGER PRIMARY KEY GENERATED ALWAYS AS IDENTITY (start with 1),
  fifty_two_week_high INTEGER NOT NULL,
  fifty_two_week_low INTEGER NOT NULL,
  five_day_simple_moving_average INTEGER,
  ten_day_simple_moving_average INTEGER,
  twenty_day_simple_moving_average INTEGER,
  fifty_day_simple_moving_average INTEGER,
  hundred_day_simple_moving_average INTEGER,
  two_hundred_day_simple_moving_average INTEGER,
  five_day_exponential_moving_average INTEGER,
  ten_day_exponential_moving_average INTEGER,
  twenty_day_exponential_moving_average INTEGER,
  fifty_day_exponential_moving_average INTEGER,
  hundred_day_exponential_moving_average INTEGER,
  two_hundred_day_exponential_moving_average INTEGER,
  macd INTEGER,
  macd_signal INTEGER,
  fast_stochastic_oscillator_k INTEGER,
  fast_stochastic_oscillator_d INTEGER,
  slow_stochastic_oscillator_k INTEGER,
  slow_stochastic_oscillator_d INTEGER,
  relative_strength_index INTEGER,
  williams_r INTEGER,
  bollinger_bands_lower INTEGER,
  bollinger_bands_upper INTEGER,
  commodity_channel_index INTEGER,
  date date NOT NULL,
  CONSTRAINT fk_fifty_two_week_high FOREIGN KEY (fifty_two_week_high) REFERENCES security_daily_data(id),
  CONSTRAINT fk_fifty_two_week_low FOREIGN KEY (fifty_two_week_low) REFERENCES security_daily_data(id)
);
CREATE INDEX daily_metrics_date_idx ON security_daily_metrics(date);
