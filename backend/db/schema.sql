-- Baseline schema for the demo backend.
--
-- Why this file is "baseline" (not fully complete):
-- - The JPA entity mappings in `backend/src/main/java/com/example/demo/entity/` define additional columns
--   (for example, aircraft live-position fields and the Flight `live_info_json` blob).
-- - Spring Boot is configured with `spring.jpa.hibernate.ddl-auto=update`, so Hibernate may add/update
--   columns at runtime to match the entity definitions.
--
-- Initialization:
-- - `docker-compose.yml` mounts this file into the MySQL container's init directory.
-- - This file is executed only on fresh database volumes.

CREATE TABLE celebrities (
    id INT AUTO_INCREMENT PRIMARY KEY,
    -- Public figure name (required for demo seed data).
    name VARCHAR(100) NOT NULL,
    industry VARCHAR(50) -- e.g., 'Music', 'Tech', 'Hollywood'
);

CREATE TABLE aircraft (
    id INT AUTO_INCREMENT PRIMARY KEY,
    -- External identifier used by the app when bridging external API data to DB rows.
    tail_number VARCHAR(20) UNIQUE NOT NULL,
    model VARCHAR(100),
    -- Nullable ownership link; if a celebrity is deleted, ownership is cleared.
    owner_id INTEGER,
    FOREIGN KEY (owner_id) REFERENCES celebrities(id) ON DELETE SET NULL
);

CREATE TABLE airports (
    -- Primary key is the airport code used by flight/origin/destination fields.
    ap_code VARCHAR(4) PRIMARY KEY, -- airport codes
    name VARCHAR(255) NOT NULL,
    city VARCHAR(100),
    country VARCHAR(100)
);

CREATE TABLE flights (
    id INT AUTO_INCREMENT PRIMARY KEY,
    aircraft_id INTEGER,
    -- FK to `aircraft.id`; deleting an aircraft deletes all its flights/snapshots.
    origin_ap_code VARCHAR(4),
    destination_ap_code VARCHAR(4),
    -- Departure is required; arrival/co2 may be unknown depending on the upstream payload.
    departure_time DATETIME NOT NULL,
    arrival_time DATETIME,
    estimated_co2_kg DECIMAL(10, 2),
    FOREIGN KEY (aircraft_id) REFERENCES aircraft(id) ON DELETE CASCADE,
    FOREIGN KEY (origin_ap_code) REFERENCES airports(ap_code),
    FOREIGN KEY (destination_ap_code) REFERENCES airports(ap_code)
);

CREATE TABLE users (
    u_id INT AUTO_INCREMENT PRIMARY KEY,
    -- Username/password fields for demo
    uname VARCHAR(100),
    upass VARCHAR(255)
);

CREATE TABLE user_tracked_flights (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    callsign VARCHAR(32) NOT NULL,
    tail_number VARCHAR(32),
    origin_ap_code VARCHAR(8),
    destination_ap_code VARCHAR(8),
    altitude_ft INT,
    velocity_kts INT,
    lat DOUBLE,
    lng DOUBLE,
    bearing DOUBLE,
    estimated_co2_kg DOUBLE,
    category VARCHAR(32),
    celebrity VARCHAR(120),
    updated_at DATETIME NOT NULL,
    UNIQUE KEY uk_user_tracked_callsign (user_id, callsign),
    FOREIGN KEY (user_id) REFERENCES users(u_id) ON DELETE CASCADE
);

INSERT INTO users (uname, upass)
SELECT 'admin', '1234'
WHERE NOT EXISTS (
    SELECT 1
    FROM users
    WHERE uname = 'admin'
);