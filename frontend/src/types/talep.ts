export type TalepDurumu = 'YENI' | 'ATANDI' | 'ISLEMDE' | 'COZULDU' | 'REDDEDILDI'
export type TalepOnceligi = 'DUSUK' | 'NORMAL' | 'YUKSEK' | 'ACIL'

export interface TalepOzeti {
  takipNo: string
  vatandasAd: string | null
  mahalle: string | null
  konuMetni: string
  kategori: string | null
  mudurlukAdi: string | null
  durum: TalepDurumu
  oncelik: TalepOnceligi
  olusturmaTarihi: string
}

export interface TalepNotuOzeti {
  personel: string
  notu: string
  tarih: string
}

export interface TalepDetay {
  takipNo: string
  vatandasAd: string | null
  iletisim: string | null
  mahalle: string | null
  konuMetni: string
  kategori: string | null
  mudurlukAdi: string | null
  durum: TalepDurumu
  oncelik: TalepOnceligi
  olusturmaTarihi: string
  guncellemeTarihi: string | null
  notlar: TalepNotuOzeti[]
}

export interface Mudurluk {
  ad: string
  sorumlulukAlani: string | null
}

export interface SiniflandirmaOnerisi {
  mudurlukAdi: string | null
  kategori: string | null
  gerekce: string | null
}

export interface TopluSiniflandirmaOnerisi {
  takipNo: string
  konuMetni: string
  oneri: SiniflandirmaOnerisi
}

export interface ResmiYaziTaslagi {
  takipNo: string
  taslak: string
}

export const TALEP_DURUMLARI: TalepDurumu[] = ['YENI', 'ATANDI', 'ISLEMDE', 'COZULDU', 'REDDEDILDI']
export const TALEP_ONCELIKLERI: TalepOnceligi[] = ['DUSUK', 'NORMAL', 'YUKSEK', 'ACIL']
