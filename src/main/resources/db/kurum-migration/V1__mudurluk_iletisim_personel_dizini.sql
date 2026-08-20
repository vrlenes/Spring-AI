CREATE TABLE mudurluk_iletisim (
    id BIGSERIAL PRIMARY KEY,
    mudurluk_adi VARCHAR(150) NOT NULL,
    telefon VARCHAR(30),
    eposta VARCHAR(150),
    adres VARCHAR(255)
);

CREATE TABLE personel_dizini (
    id BIGSERIAL PRIMARY KEY,
    ad_soyad VARCHAR(150) NOT NULL,
    unvan VARCHAR(100),
    mudurluk_adi VARCHAR(150),
    telefon VARCHAR(30),
    eposta VARCHAR(150)
);

INSERT INTO mudurluk_iletisim (mudurluk_adi, telefon, eposta, adres) VALUES
    ('Fen İşleri Müdürlüğü', '0332 000 10 01', 'fenisleri@karatay.bel.tr', 'Karatay Belediyesi Hizmet Binası, Kat 2'),
    ('Park ve Bahçeler Müdürlüğü', '0332 000 10 02', 'parkbahce@karatay.bel.tr', 'Karatay Belediyesi Hizmet Binası, Kat 2'),
    ('Temizlik İşleri Müdürlüğü', '0332 000 10 03', 'temizlik@karatay.bel.tr', 'Karatay Belediyesi Hizmet Binası, Kat 1'),
    ('Zabıta Müdürlüğü', '0332 000 10 04', 'zabita@karatay.bel.tr', 'Karatay Belediyesi Hizmet Binası, Kat 1'),
    ('İmar ve Şehircilik Müdürlüğü', '0332 000 10 05', 'imar@karatay.bel.tr', 'Karatay Belediyesi Hizmet Binası, Kat 3'),
    ('Ruhsat ve Denetim Müdürlüğü', '0332 000 10 06', 'ruhsat@karatay.bel.tr', 'Karatay Belediyesi Hizmet Binası, Kat 3'),
    ('Veteriner İşleri Müdürlüğü', '0332 000 10 07', 'veteriner@karatay.bel.tr', 'Karatay Belediyesi Hizmet Binası, Kat 1'),
    ('Su ve Kanalizasyon Müdürlüğü', '0332 000 10 08', 'sukanal@karatay.bel.tr', 'Karatay Belediyesi Hizmet Binası, Kat 2'),
    ('Sosyal Yardım İşleri Müdürlüğü', '0332 000 10 09', 'sosyalyardim@karatay.bel.tr', 'Karatay Belediyesi Hizmet Binası, Kat 1'),
    ('Kültür ve Sosyal İşler Müdürlüğü', '0332 000 10 10', 'kultur@karatay.bel.tr', 'Karatay Belediyesi Hizmet Binası, Kat 2');

INSERT INTO personel_dizini (ad_soyad, unvan, mudurluk_adi, telefon, eposta) VALUES
    ('Ahmet Yılmaz', 'Fen İşleri Müdürü', 'Fen İşleri Müdürlüğü', '0332 000 10 01', 'ahmet.yilmaz@karatay.bel.tr'),
    ('Ayşe Kaya', 'İmar ve Şehircilik Müdürü', 'İmar ve Şehircilik Müdürlüğü', '0332 000 10 05', 'ayse.kaya@karatay.bel.tr'),
    ('Mehmet Demir', 'Ruhsat ve Denetim Müdürü', 'Ruhsat ve Denetim Müdürlüğü', '0332 000 10 06', 'mehmet.demir@karatay.bel.tr'),
    ('Fatma Şahin', 'Zabıta Amiri', 'Zabıta Müdürlüğü', '0332 000 10 04', 'fatma.sahin@karatay.bel.tr'),
    ('Ali Çelik', 'Park ve Bahçeler Müdürü', 'Park ve Bahçeler Müdürlüğü', '0332 000 10 02', 'ali.celik@karatay.bel.tr');
