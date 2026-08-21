-- V1'de sadece 5/10 mudurlugun personeli vardi - kalan 5 mudurluk icin
-- personelAra araci "bulunamadi" donuyordu (canli testte fark edildi,
-- kullanicinin "hepsi olsun ki tek tek test ederiz" talebi).
INSERT INTO personel_dizini (ad_soyad, unvan, mudurluk_adi, telefon, eposta) VALUES
    ('Hasan Öztürk', 'Temizlik İşleri Müdürü', 'Temizlik İşleri Müdürlüğü', '0332 000 10 03', 'hasan.ozturk@karatay.bel.tr'),
    ('Zeynep Aydın', 'Su ve Kanalizasyon Müdürü', 'Su ve Kanalizasyon Müdürlüğü', '0332 000 10 08', 'zeynep.aydin@karatay.bel.tr'),
    ('Emre Koç', 'Veteriner İşleri Müdürü', 'Veteriner İşleri Müdürlüğü', '0332 000 10 07', 'emre.koc@karatay.bel.tr'),
    ('Elif Yıldız', 'Sosyal Yardım İşleri Müdürü', 'Sosyal Yardım İşleri Müdürlüğü', '0332 000 10 09', 'elif.yildiz@karatay.bel.tr'),
    ('Serkan Aksoy', 'Kültür ve Sosyal İşler Müdürü', 'Kültür ve Sosyal İşler Müdürlüğü', '0332 000 10 10', 'serkan.aksoy@karatay.bel.tr');
