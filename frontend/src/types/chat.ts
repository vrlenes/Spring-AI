export interface Kaynak {
  baslik: string
  parcaNo: number
  benzerlik: number | null
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
}

export type ChatStreamEvent =
  | { type: 'conversationId'; conversationId: string }
  | { type: 'token'; text: string }
  | { type: 'kaynaklar'; kaynaklar: Kaynak[] }
  | { type: 'bekleyenIslem'; bekleyenIslem: BekleyenIslem }
  | { type: 'araclar'; araclar: string[] }
