ALTER TABLE application_user ADD COLUMN email VARCHAR(320);
ALTER TABLE application_user ALTER COLUMN password_hash DROP NOT NULL;

CREATE UNIQUE INDEX uk_application_user_email_ci
    ON application_user (LOWER(email))
    WHERE email IS NOT NULL;

CREATE TABLE account_activation_token (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    used_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_activation_user FOREIGN KEY (user_id) REFERENCES application_user(id) ON DELETE CASCADE
);

CREATE INDEX idx_activation_user_id ON account_activation_token(user_id);
