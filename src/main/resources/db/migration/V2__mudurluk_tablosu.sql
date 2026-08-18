CREATE TABLE mudurluk (
    id                BIGSERIAL PRIMARY KEY,
    ad                VARCHAR(120) NOT NULL UNIQUE,
    sorumluluk_alani  TEXT,
    aktif             BOOLEAN NOT NULL DEFAULT TRUE
);
