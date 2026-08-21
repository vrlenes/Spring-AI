#!/usr/bin/env node
// RAG kalitesini olcen toplu degerlendirme scripti.
//
// NE YAPAR: Sabit bir soru setini gercek backend'e (/api/chat) sirayla
// gonderir, her cevaptan KaynakDogrulamaService'in ürettigi "dogrulama"
// alanini (bkz. ChatResponse.dogrulama) okuyup toplu bir dogruluk raporu
// cikarir. Uygulamanin bir parcasi DEGIL - RAG'i etkileyen her degisiklikten
// (yeni belge, embedding modeli, chunk/esik ayari, yeni mod) sonra elle
// calistirilan bir QA araci.
//
// KULLANIM: Backend calisirken (varsayilan http://localhost:8080):
//   node scripts/rag-degerlendirme.js
// Farkli bir adres/kullanici icin:
//   BASE_URL=http://localhost:8080 KULLANICI_ADI=admin SIFRE=admin123 node scripts/rag-degerlendirme.js
//
// YENI MOD/BELGE EKLENINCE: asagidaki SORU_SETI dizisine o moda ait
// birkac gercek soru eklemek/cikarmak gerekir - bu script kendiliginden
// yeni icerigi bilmez.

const BASE_URL = process.env.BASE_URL || 'http://localhost:8080';
const KULLANICI_ADI = process.env.KULLANICI_ADI || 'admin';
const SIFRE = process.env.SIFRE || 'admin123';

const SORU_SETI = [
  // --- GENEL (Belediye Kanunu + Resmi Yazışmalar Yönetmeliği) ---
  { mod: 'GENEL', soru: 'Belediye meclisi ne zaman toplanır?' },
  { mod: 'GENEL', soru: 'Resmi yazışmalarda imza yetkisi kime aittir?' },
  { mod: 'GENEL', soru: 'Belediye başkanının görev ve yetkileri nelerdir?' },
  { mod: 'GENEL', soru: 'Belediye encümeni kimlerden oluşur?' },
  { mod: 'GENEL', soru: 'Resmi yazılarda vekaleten imza nasıl belirtilir?' },
  { mod: 'GENEL', soru: 'Belediyenin görev ve sorumlulukları nelerdir?' },

  // --- IMAR (İmar Kanunu 3194 + Planlı Alanlar İmar Yönetmeliği) ---
  { mod: 'IMAR', soru: 'İmar planında ada ve parsel nasıl belirlenir?' },
  { mod: 'IMAR', soru: 'Parselasyon planı kim tarafından onaylanır?' },
  { mod: 'IMAR', soru: 'Yapı ruhsatı almadan önce imar uygunluğu nasıl kontrol edilir?' },
  { mod: 'IMAR', soru: 'İmar planı değişikliği nasıl yapılır?' },
  { mod: 'IMAR', soru: 'Yapı kullanma izni ne zaman alınır?' },
  { mod: 'IMAR', soru: 'İmar planlarında yoğunluk nasıl belirlenir?' },

  // --- RUHSAT (İşyeri Açma ve Çalışma Ruhsatlarına İlişkin Yönetmelik) ---
  { mod: 'RUHSAT', soru: 'İşyeri açma ruhsatı için hangi belgeler gerekir?' },
  { mod: 'RUHSAT', soru: 'Sıhhi müessese ile gayrisıhhi müessese arasındaki fark nedir?' },
  { mod: 'RUHSAT', soru: 'Ruhsat başvurusu ne kadar sürede sonuçlanır?' },
  { mod: 'RUHSAT', soru: 'Gayrisıhhi müesseseler kaç sınıfa ayrılır?' },
  { mod: 'RUHSAT', soru: 'İşyeri açma ruhsatı kim tarafından verilir?' },
  { mod: 'RUHSAT', soru: 'Ruhsatsız faaliyet gösteren işyerine ne yapılır?' },

  // --- GENEL (yeni eklenen: Belediye Zabıta Yönetmeliği, Hayvanları Koruma
  // Kanunu 5199, Sosyal Hizmetler Kanunu 2828 - paylaşılan havuzda) ---
  { mod: 'GENEL', soru: 'Zabıta kaldırım işgaline karşı hangi cezaları uygulayabilir?' },
  { mod: 'GENEL', soru: 'Başıboş sokak hayvanlarının toplanması kimin sorumluluğundadır?' },
  { mod: 'GENEL', soru: 'Hayvan bakımevi kurma zorunluluğu hangi belediyeler için geçerlidir?' },
  { mod: 'GENEL', soru: 'Muhtaç kişilere sosyal yardım yapılması hangi esaslara dayanır?' },

  // --- GENEL (yeni eklenen: Kabahatler Kanunu 5326, Dilekçe Hakkı Kanunu
  // 3071, Büyükşehir Belediyesi Kanunu 5216, Atık Yönetimi Yönetmeliği,
  // Çevresel Gürültü Yönetmeliği) ---
  { mod: 'GENEL', soru: 'İdari para cezasına itiraz süresi nedir?' },
  { mod: 'GENEL', soru: 'Vatandaşların dilekçe hakkı nasıl düzenlenmiştir?' },
  { mod: 'GENEL', soru: 'Büyükşehir belediyesi ile ilçe belediyesi arasındaki görev paylaşımı nasıldır?' },
  { mod: 'GENEL', soru: 'Atıkların kaynağında ayrıştırılması zorunlu mudur?' },
  { mod: 'GENEL', soru: 'Gürültü şikayetlerinde hangi ölçümler esas alınır?' },
];

function cerezOku(yanit) {
  const setCookie = yanit.headers.get('set-cookie');
  if (!setCookie) return null;
  return setCookie.split(';')[0];
}

async function girisYap() {
  const yanit = await fetch(`${BASE_URL}/api/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ kullaniciAdi: KULLANICI_ADI, sifre: SIFRE }),
  });
  if (!yanit.ok) {
    throw new Error(`Giriş başarısız (HTTP ${yanit.status}) - kullanıcı adı/şifre doğru mu?`);
  }
  const cerez = cerezOku(yanit);
  if (!cerez) {
    throw new Error('Giriş yanıtında oturum çerezi bulunamadı.');
  }
  return cerez;
}

async function soruSor(cerez, mod, soru) {
  const yanit = await fetch(`${BASE_URL}/api/chat`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json; charset=utf-8', Cookie: cerez },
    body: JSON.stringify({ mesaj: soru, mod }),
  });
  if (!yanit.ok) {
    return { hata: `HTTP ${yanit.status}` };
  }
  return yanit.json();
}

function yuzde(pay, payda) {
  if (payda === 0) return 'n/a';
  return `%${Math.round((pay / payda) * 100)}`;
}

async function calistir() {
  console.log(`RAG değerlendirmesi başlıyor - ${SORU_SETI.length} soru, hedef: ${BASE_URL}\n`);

  const cerez = await girisYap();

  const sonuclar = [];
  for (const { mod, soru } of SORU_SETI) {
    process.stdout.write(`[${mod}] ${soru} ... `);
    try {
      const cevap = await soruSor(cerez, mod, soru);
      if (cevap.hata) {
        console.log(`HATA (${cevap.hata})`);
        sonuclar.push({ mod, soru, durum: 'hata' });
        continue;
      }
      const kaynakVar = Array.isArray(cevap.kaynaklar) && cevap.kaynaklar.length > 0;
      const dogrulama = cevap.dogrulama;
      if (!kaynakVar) {
        console.log('kaynak bulunamadı');
        sonuclar.push({ mod, soru, durum: 'kaynaksiz' });
      } else if (!dogrulama) {
        console.log('kaynak var, doğrulama yapılamadı');
        sonuclar.push({ mod, soru, durum: 'dogrulanamadi' });
      } else if (dogrulama.dogrulandi) {
        console.log('✓ doğrulandı');
        sonuclar.push({ mod, soru, durum: 'dogrulandi' });
      } else {
        console.log(`✗ DOĞRULANAMADI - ${dogrulama.not || 'sebep belirtilmemiş'}`);
        sonuclar.push({ mod, soru, durum: 'yanlis', not: dogrulama.not });
      }
    } catch (e) {
      console.log(`HATA (${e.message})`);
      sonuclar.push({ mod, soru, durum: 'hata' });
    }
  }

  const toplam = sonuclar.length;
  const kaynakli = sonuclar.filter((s) => s.durum !== 'kaynaksiz' && s.durum !== 'hata').length;
  const dogrulanan = sonuclar.filter((s) => s.durum === 'dogrulandi').length;
  const yanlislar = sonuclar.filter((s) => s.durum === 'yanlis');
  const kaynaksizlar = sonuclar.filter((s) => s.durum === 'kaynaksiz');
  const hatalar = sonuclar.filter((s) => s.durum === 'hata');

  console.log('\n--- ÖZET ---');
  console.log(`Toplam soru: ${toplam}`);
  console.log(`Kaynak bulundu: ${kaynakli}/${toplam} (${yuzde(kaynakli, toplam)})`);
  console.log(`Kaynakla doğrulandı: ${dogrulanan}/${kaynakli} (${yuzde(dogrulanan, kaynakli)})`);

  if (kaynaksizlar.length > 0) {
    console.log(`\nKaynak bulunamayan sorular:`);
    kaynaksizlar.forEach((s) => console.log(`  - [${s.mod}] ${s.soru}`));
  }
  if (yanlislar.length > 0) {
    console.log(`\nDoğrulanamayan (kaynakla çelişen) cevaplar:`);
    yanlislar.forEach((s) => console.log(`  - [${s.mod}] ${s.soru}\n    → ${s.not || 'sebep belirtilmemiş'}`));
  }
  if (hatalar.length > 0) {
    console.log(`\nHata alan sorular:`);
    hatalar.forEach((s) => console.log(`  - [${s.mod}] ${s.soru}`));
  }
}

calistir().catch((e) => {
  console.error(`\nScript başarısız oldu: ${e.message}`);
  process.exit(1);
});
