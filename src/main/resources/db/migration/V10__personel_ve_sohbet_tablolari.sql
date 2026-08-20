CREATE TABLE personel (
    id                  BIGSERIAL PRIMARY KEY,
    kullanici_adi       VARCHAR(60) NOT NULL UNIQUE,
    sifre_hash          VARCHAR(100) NOT NULL,
    ad_soyad            VARCHAR(150) NOT NULL,
    olusturma_tarihi    TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE sohbet (
    id                  VARCHAR(36) PRIMARY KEY,
    personel_id         BIGINT NOT NULL REFERENCES personel(id) ON DELETE CASCADE,
    mod                 VARCHAR(20) NOT NULL,
    baslik              VARCHAR(200),
    olusturma_tarihi    TIMESTAMP NOT NULL,
    guncelleme_tarihi   TIMESTAMP NOT NULL
);

CREATE INDEX idx_sohbet_personel_id ON sohbet(personel_id);

CREATE TABLE sohbet_mesaji (
    id                  BIGSERIAL PRIMARY KEY,
    sohbet_id           VARCHAR(36) NOT NULL REFERENCES sohbet(id) ON DELETE CASCADE,
    rol                 VARCHAR(20) NOT NULL,
    icerik              TEXT NOT NULL,
    kaynaklar           TEXT,
    araclar             TEXT,
    bekleyen_islem      TEXT,
    olusturma_tarihi    TIMESTAMP NOT NULL
);

CREATE INDEX idx_sohbet_mesaji_sohbet_id ON sohbet_mesaji(sohbet_id);
