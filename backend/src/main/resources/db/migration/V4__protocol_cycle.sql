ALTER TABLE tickets ADD COLUMN protocol_no INTEGER;
ALTER TABLE tickets ADD COLUMN protocol_cycle INTEGER;
UPDATE tickets SET protocol_cycle = 1 WHERE protocol_cycle IS NULL;
UPDATE tickets SET protocol_no = (SELECT COUNT(*) FROM tickets older WHERE older.id <= tickets.id) WHERE protocol_no IS NULL;
ALTER TABLE tickets ALTER COLUMN protocol_no SET NOT NULL;
ALTER TABLE tickets ALTER COLUMN protocol_cycle SET NOT NULL;
CREATE TABLE attendance_cycle (id SMALLINT PRIMARY KEY, cycle_no INTEGER NOT NULL, next_no INTEGER NOT NULL);
INSERT INTO attendance_cycle (id, cycle_no, next_no) SELECT 1, 1, COALESCE(MAX(protocol_no), 0) + 1 FROM tickets;
