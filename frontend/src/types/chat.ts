export type SohbetModu = 'GENEL' | 'TALEP' | 'IMAR' | 'RUHSAT' | 'OTOMATIK'

export type GeriBildirim = 'OLUMLU' | 'OLUMSUZ'

export type AracGrubu = 'RAG' | 'TALEP' | 'KURUM_DIZIN'

// SADECE RAG kaynagi kullanildiginda dolu - cevabin kaynaga sadik olup
// olmadigina dair bagimsiz bir ikinci model kontrolunun sonucu (bkz. backend
// KaynakDogrulamaService).
export interface KaynakDogrulamaSonucu {
  dogrulandi: boolean
  not: string | null
}

export interface Kaynak {
  baslik: string
  parcaNo: number
  benzerlik: number | null
  maddeNo: number | null
}

// "tip" alani hangi kart bilesenin render edilecegini belirler (bkz.
// YapisalVeriKarti.tsx) - modelin yazdigi serbest metne degil, backend'in
// tool sonucundan urettigi ham veriye dayanir (ayni kaynaklar/araclar
// ilkesi). "veri"nin gercek sekli "tip"e gore degisir (backend'deki
// YapisalVeriPaketi ile ayni sozlesme) - TypeScript'in string literal'i
// generic string'den ayirt edemedigi icin (discriminated union sinirlamasi)
// burada tip guvenli bir union yerine "unknown" tutulup YapisalVeriKarti
// icinde "tip"e gore acikca cast ediliyor.
export interface YapisalVeriPaketi {
  tip: string
  veri: unknown
}

export type BekleyenIslemTuru = 'MUDURLUGE_ATA' | 'DURUM_GUNCELLE' | 'ONCELIK_GUNCELLE' | 'NOT_EKLE'

export interface BekleyenIslem {
  id: string
  tur: BekleyenIslemTuru
  takipNo: string
  aciklama: string
}

// "url" hem yerel (gonderim aninda URL.createObjectURL(dosya)) hem sunucudan
// (gecmis yuklendiginde /api/sohbetler/{sohbetId}/mesajlar/{id}/ek) gelebilir
// - EkOnizleme/MessageBubble baytlarin nereden geldigiyle ilgilenmez.
export interface EkOnizleme {
  url: string
  mimeTipi: string
  dosyaAdi: string
}

export interface ChatMessage {
  id: string
  role: 'kullanici' | 'asistan'
  content: string
  kaynaklar?: Kaynak[]
  bekleyenIslem?: BekleyenIslem
  araclar?: string[]
  yapisalVeri?: YapisalVeriPaketi
  ek?: EkOnizleme
  // Sadece sohbet OTOMATIK modundaysa dolu - ModYonlendirmeService'in bu
  // mesaj icin sectigi gercek mod (bkz. backend ChatResponse.algilananMod).
  algilananMod?: SohbetModu
  // Backend'deki kalici sohbet_mesaji kaydinin id'si - gecmisten yuklenen
  // mesajlarda hemen dolu, yeni akan bir mesajda ise akis bitip gecmis
  // yeniden cekilene kadar bos kalir (bkz. useSohbetStore.mesajGonder).
  // Geri bildirim butonlari bu alan olmadan calisamaz.
  mesajId?: number
  geriBildirim?: GeriBildirim
  dogrulama?: KaynakDogrulamaSonucu
  // Akis basladiktan (HTTP 200 sonrasi) bir hata olursa dolar - bkz. backend
  // ChatService.akisliYanitla'daki "hata" SSE olayi. Mesaj icerigi bu ana
  // kadar akan token'lari (varsa) korur, hata ayri bir alanda gosterilir.
  hata?: string
}

export type ChatStreamEvent =
  | { type: 'conversationId'; conversationId: string }
  | { type: 'token'; text: string }
  | { type: 'kaynaklar'; kaynaklar: Kaynak[] }
  | { type: 'bekleyenIslem'; bekleyenIslem: BekleyenIslem }
  | { type: 'araclar'; araclar: string[] }
  | { type: 'yapisalVeri'; yapisalVeri: YapisalVeriPaketi }
  | { type: 'algilananMod'; mod: SohbetModu }
  | { type: 'dogrulama'; dogrulama: KaynakDogrulamaSonucu }
  | { type: 'hata'; mesaj: string }

export interface SohbetOzeti {
  id: string
  mod: SohbetModu
  baslik: string | null
  guncellemeTarihi: string
}

export interface SohbetMesajOzeti {
  id: number
  rol: 'KULLANICI' | 'ASISTAN'
  icerik: string
  kaynaklar: Kaynak[] | null
  araclar: string[] | null
  bekleyenIslem: BekleyenIslem | null
  yapisalVeri: YapisalVeriPaketi | null
  ekMimeTipi: string | null
  ekDosyaAdi: string | null
  geriBildirim: GeriBildirim | null
  olusturmaTarihi: string
}
