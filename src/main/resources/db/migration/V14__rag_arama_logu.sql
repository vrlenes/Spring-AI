CREATE TABLE rag_arama_logu (
    id BIGSERIAL PRIMARY KEY,
    mod VARCHAR(20),
    sorgu TEXT NOT NULL,
    sonuc_sayisi INT NOT NULL,
    en_iyi_benzerlik DOUBLE PRECISION,
    olusturma_tarihi TIMESTAMP NOT NULL
);
