-- AI toplu siniflandirma ozelligini daha zengin/gercekci bir setle test edebilmek
-- icin ek atanmamis (mudurluk_id NULL, durum YENI) talep kayitlari. Coğu net
-- sinyalli, birkaci ise kasten belirsiz/cift-mudurluklu (proje dokumaninin
-- "kirli veri" stratejisiyle tutarli).

INSERT INTO talep (takip_no, vatandas_ad, iletisim, mahalle, konu_metni, kategori, mudurluk_id, durum, oncelik, olusturma_tarihi, guncelleme_tarihi) VALUES
('TLP-2026-00091', 'Ayla Demirtaş', '0532 456 78 01', 'Fetih', 'sokak köpekleri saldırgan oldu çocuklar korkuyor bir şey yapılsın', NULL, NULL, 'YENI', 'YUKSEK', '2026-08-16 09:00:00', NULL),
('TLP-2026-00092', 'Kemal Sarı', '0533 456 78 02', 'Aziziye', 'aydınlatma direği devrilmiş yola düşmüş tehlikeli', NULL, NULL, 'YENI', 'ACIL', '2026-08-16 10:30:00', NULL),
('TLP-2026-00093', 'Nurten Aksoy', NULL, 'Karatay', 'market önüne tezgah kurmuşlar kaldırımı tamamen kapatıyorlar', NULL, NULL, 'YENI', 'NORMAL', '2026-08-15 14:00:00', NULL),
('TLP-2026-00094', 'Serhat Bulut', '0534 456 78 04', 'Yeni', 'parktaki bank kırılmış oturulmuyor tamir edilsin', NULL, NULL, 'YENI', 'DUSUK', '2026-08-15 11:00:00', NULL),
('TLP-2026-00095', 'Gülten Er', '0535 456 78 05', 'Sahibiata', 'komşu binanın imar durumunu öğrenmek istiyorum arsam yanında', NULL, NULL, 'YENI', 'DUSUK', '2026-08-14 13:00:00', NULL),
('TLP-2026-00096', 'Hakan Türk', '0536 456 78 06', 'Fevzi Çakmak', 'işyerimin ruhsat yenileme süreci ne durumda öğrenebilir miyim', NULL, NULL, 'YENI', 'NORMAL', '2026-08-14 09:30:00', NULL),
('TLP-2026-00097', 'Meryem Aydın', '0537 456 78 07', 'Hacı Şaban', 'yaşlı anneme evde bakım desteği gerekiyor nasıl başvurabilirim', NULL, NULL, 'YENI', 'NORMAL', '2026-08-13 15:00:00', NULL),
('TLP-2026-00098', 'Onur Kaya', NULL, 'Şeyh Sadrettin', 'kütüphanenin çalışma saatlerini öğrenmek istiyorum hafta sonu açık mı', NULL, NULL, 'YENI', 'DUSUK', '2026-08-13 10:00:00', NULL),
('TLP-2026-00099', 'Fadime Koç', '0538 456 78 09', 'Musalla Bağları', 'sokağımızda çöpler iki haftadır toplanmıyor dayanılmaz koku var', NULL, NULL, 'YENI', 'YUKSEK', '2026-08-17 08:00:00', NULL),
('TLP-2026-00100', 'Rıdvan Ateş', '0539 456 78 10', 'Aziziye', 'evimin önündeki su borusu patlamış yol su içinde kaldı', NULL, NULL, 'YENI', 'ACIL', '2026-08-17 07:30:00', NULL),
('TLP-2026-00101', 'İclal Bozkurt', NULL, 'Karatay', 'bu konu kime ait bilmiyorum ama bir bakılsın lütfen mağdur oluyoruz', NULL, NULL, 'YENI', 'DUSUK', '2026-08-12 16:00:00', NULL),
('TLP-2026-00102', 'Turgay Yıldırım', '0530 456 78 12', 'Fetih', 'hem çukur hem başıboş köpek sorunu var sokakta ikisi de tehlikeli', NULL, NULL, 'YENI', 'YUKSEK', '2026-08-16 18:00:00', NULL),
('TLP-2026-00103', 'Zübeyde Aslan', '0531 456 78 13', 'Şeyh Sadrettin', 'yandaki dükkandan gürültü ve pis koku geliyor ruhsatsız çalışıyor galiba', NULL, NULL, 'YENI', 'NORMAL', '2026-08-15 21:00:00', NULL),
('TLP-2026-00104', 'Naci Polat', NULL, 'Yeni', 'mahallede yürüyüş ve bisiklet yolu yapılmasını istiyoruz', NULL, NULL, 'YENI', 'DUSUK', '2026-08-11 12:00:00', NULL);
