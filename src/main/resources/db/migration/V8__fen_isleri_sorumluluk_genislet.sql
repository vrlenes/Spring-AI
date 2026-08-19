-- Test edilirken bulundu: "yol çalışmasından sonra duvarımda çatlak oluştu"
-- gibi talepler, AI siniflandirmasinda yanlislikla İmar ve Şehircilik
-- Müdürlüğü'ne yönlendiriliyordu - cunku Fen İşleri'nin sorumluluk metninde
-- kendi yol çalışmalarının NEDEN OLDUĞU hasarlardan bahsedilmiyordu, model
-- sadece "duvar/bina" kelimesine takılıp yapı/imar ile iliskilendiriyordu.
-- Gercek belediyelerde bu tur hasar sikayetleri de Fen İşleri'ne gider.
UPDATE mudurluk
SET sorumluluk_alani = sorumluluk_alani ||
    ' Yol, kaldırım veya altyapı çalışmaları sırasında/sonrasında komşu bina, duvar ya da bahçe duvarlarında oluşan hasarların değerlendirilmesi ve giderilmesi.'
WHERE ad = 'Fen İşleri Müdürlüğü';
