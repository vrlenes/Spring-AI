-- V8'deki ek cumle, model uzerinde yeterince guclu bir sinyal olmadi (test
-- edildi: hala İmar ve Şehircilik'i seciyordu). Cumleyi basa alip talep
-- metnindeki kelimelerle daha birebir orten bir ifadeye guncelliyoruz.
UPDATE mudurluk
SET sorumluluk_alani =
    'Yol çalışması, kazı veya altyapı çalışması nedeniyle komşu binalarda/duvarlarda oluşan çatlak ve hasarların değerlendirilmesi. ' ||
    'Asfalt yama ve yol bakımı, kaldırım yapımı ve onarımı, yağmur suyu ızgaraları, sokak aydınlatma direkleri, üstyapı çalışmaları, bordür taşı, menfez ve köprü bakımı.'
WHERE ad = 'Fen İşleri Müdürlüğü';
