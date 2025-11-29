DROP TABLE IF EXISTS password_reset_token;


CREATE TABLE password_reset_token (
    id BIGSERIAL PRIMARY KEY,
    otp VARCHAR(10) NOT NULL,
    reset_token VARCHAR(255),
    expiry_date TIMESTAMP NOT NULL,
    created_date TIMESTAMP NOT NULL,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE
);