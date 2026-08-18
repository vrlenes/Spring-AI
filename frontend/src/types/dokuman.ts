export interface Dokuman {
  id: number
  dosyaAdi: string
  baslik: string
  kategori: string | null
  yuklenmeTarihi: string
  chunkSayisi: number
}
