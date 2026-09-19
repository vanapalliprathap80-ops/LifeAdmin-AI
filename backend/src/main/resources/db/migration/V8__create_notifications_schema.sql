-- V8: Create Notifications Schema
-- Tracks sent notifications to avoid spamming the user

CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    action_id UUID, -- If this relates to a specific action/deadline
    type VARCHAR(50) NOT NULL, -- e.g. EMAIL, IN_APP
    category VARCHAR(50) NOT NULL, -- e.g. DEADLINE_REMINDER, OVERDUE, OBLIGATION_DETECTED
    status VARCHAR(50) NOT NULL DEFAULT 'SENT', -- SENT, FAILED, DELIVERED, READ
    message_content TEXT NOT NULL,
    sent_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    
    CONSTRAINT fk_notifications_user 
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_notifications_action 
        FOREIGN KEY (action_id) REFERENCES actions (id) ON DELETE SET NULL
);

CREATE INDEX idx_notifications_user ON notifications(user_id);
CREATE INDEX idx_notifications_action ON notifications(action_id);
