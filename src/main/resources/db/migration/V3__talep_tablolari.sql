CREATE TABLE talep (
    id                  BIGSERIAL PRIMARY KEY,
    takip_no            VARCHAR(20) NOT NULL UNIQUE,
    vatandas_ad         VARCHAR(150),
    iletisim            VARCHAR(100),
    mahalle             VARCHAR(100),
    konu_metni          TEXT NOT NULL,
    kategori            VARCHAR(60),
    mudurluk_id         BIGINT REFERENCES mudurluk(id),
    durum               VARCHAR(30) NOT NULL,
    oncelik             VARCHAR(20) NOT NULL,
    olusturma_tarihi    TIMESTAMP NOT NULL,
    guncelleme_tarihi   TIMESTAMP
);

CREATE INDEX idx_talep_durum ON talep(durum);
CREATE INDEX idx_talep_mudurluk_id ON talep(mudurluk_id);
CREATE INDEX idx_talep_olusturma_tarihi ON talep(olusturma_tarihi);

CREATE TABLE talep_notu (
    id          BIGSERIAL PRIMARY KEY,
    talep_id    BIGINT NOT NULL REFERENCES talep(id) ON DELETE CASCADE,
    personel    VARCHAR(120),
    notu        TEXT NOT NULL,
    tarih       TIMESTAMP NOT NULL
);

CREATE INDEX idx_talep_notu_talep_id ON talep_notu(talep_id);
