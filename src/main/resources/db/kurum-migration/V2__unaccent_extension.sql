-- Turkce karakter (i/İ, ş/s, ü/u, ğ/g vb.) duyarsiz arama icin - modelin
-- olusturdugu sorgu metni her zaman dogru aksanli Turkce olmayabilir (canli
-- testte yakalandi: "Fen Isleri" sorgusu "Fen İşleri Müdürlüğü" ile
-- eslesmiyordu, model bulamayinca kendi bilgisinden uydurmustu).
CREATE EXTENSION IF NOT EXISTS unaccent;
