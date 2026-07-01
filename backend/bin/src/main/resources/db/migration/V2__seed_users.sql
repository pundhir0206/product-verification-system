-- Default users for first login. CHANGE THESE PASSWORDS after first login in a real deployment.
-- admin   / admin123     (role: ADMIN)
-- operator / operator123 (role: OPERATOR)

INSERT INTO users (username, password_hash, role) VALUES
    ('admin', '$2b$10$.urf0RN03ce/T7DUMjWs6OHvqnnLahjakmd/ezY9OHP7rnBt2lH56', 'ADMIN'),
    ('operator', '$2b$10$IdiGLo/eHI9sHIHCfZbT9Ow.EL/uzYWk23CGdhe5QmlH1WGFUvcU6', 'OPERATOR');
