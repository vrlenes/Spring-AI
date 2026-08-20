export type SohbetModu = 'GENEL' | 'TALEP'

export interface Kaynak {
  baslik: string
  parcaNo: number
  benzerlik: number | null
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

export interface ChatMessage {
  id: string
  role: 'kullanici' | 'asistan'
  content: string
  kaynaklar?: Kaynak[]
  bekleyenIslem?: BekleyenIslem
  araclar?: string[]
  yapisalVeri?: YapisalVeriPaketi
}

export type ChatStreamEvent =
  | { type: 'conversationId'; conversationId: string }
  | { type: 'token'; text: string }
  | { type: 'kaynaklar'; kaynaklar: Kaynak[] }
  | { type: 'bekleyenIslem'; bekleyenIslem: BekleyenIslem }
  | { type: 'araclar'; araclar: string[] }
  | { type: 'yapisalVeri'; yapisalVeri: YapisalVeriPaketi }

export interface SohbetOzeti {
  id: string
  mod: SohbetModu
  baslik: string | null
  guncellemeTarihi: string
}

export interface SohbetMesajOzeti {
  rol: 'KULLANICI' | 'ASISTAN'
  icerik: string
  kaynaklar: Kaynak[] | null
  araclar: string[] | null
  bekleyenIslem: BekleyenIslem | null
  yapisalVeri: YapisalVeriPaketi | null
  olusturmaTarihi: string
}
