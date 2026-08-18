CREATE TABLE dokuman (
    id                BIGSERIAL PRIMARY KEY,
    dosya_adi         VARCHAR(255) NOT NULL,
    baslik            VARCHAR(255) NOT NULL,
    kategori          VARCHAR(80),
    yuklenme_tarihi   TIMESTAMP NOT NULL,
    chunk_sayisi      INT
);
