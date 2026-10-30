ALTER TABLE tickets ADD COLUMN channel VARCHAR(16) NOT NULL DEFAULT 'WEB';
ALTER TABLE tickets ADD COLUMN whatsapp_phone VARCHAR(20);
ALTER TABLE tickets ADD COLUMN whatsapp_window_until TIMESTAMP WITH TIME ZONE;
CREATE INDEX idx_ticket_whatsapp_phone ON tickets(whatsapp_phone);

ALTER TABLE comments ADD COLUMN channel VARCHAR(16) NOT NULL DEFAULT 'WEB';
ALTER TABLE comments ADD COLUMN direction VARCHAR(8) NOT NULL DEFAULT 'OUT';
ALTER TABLE comments ADD COLUMN external_id VARCHAR(128);
ALTER TABLE comments ADD COLUMN delivery_status VARCHAR(16);
ALTER TABLE comments ADD COLUMN failure_reason VARCHAR(300);
CREATE UNIQUE INDEX idx_comment_external_id ON comments(external_id);
