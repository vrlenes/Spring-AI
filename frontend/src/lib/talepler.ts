import type {
  Mudurluk,
  SiniflandirmaOnerisi,
  TalepDetay,
  TalepDurumu,
  TalepOnceligi,
  TalepOzeti,
  TopluSiniflandirmaOnerisi,
} from '@/types/talep'

export interface TalepFiltre {
  durum?: string
  anahtarKelime?: string
  mudurluk?: string
  mahalle?: string
  gunSayisi?: number
  atanmamis?: boolean
}

export async function talepleriGetir(filtre: TalepFiltre): Promise<TalepOzeti[]> {
  const params = new URLSearchParams()
  if (filtre.durum) params.set('durum', filtre.durum)
  if (filtre.anahtarKelime) params.set('anahtarKelime', filtre.anahtarKelime)
  if (filtre.mudurluk) params.set('mudurluk', filtre.mudurluk)
  if (filtre.mahalle) params.set('mahalle', filtre.mahalle)
  if (filtre.gunSayisi) params.set('gunSayisi', String(filtre.gunSayisi))
  if (filtre.atanmamis) params.set('atanmamis', 'true')

  const yanit = await fetch(`/api/talepler?${params.toString()}`)
  if (!yanit.ok) {
    throw new Error(`Talepler getirilemedi (HTTP ${yanit.status})`)
  }
  return yanit.json()
}

export async function talepDetayGetir(takipNo: string): Promise<TalepDetay | null> {
  const yanit = await fetch(`/api/talepler/${encodeURIComponent(takipNo)}`)
  if (yanit.status === 404) return null
  if (!yanit.ok) {
    throw new Error(`Talep detayı getirilemedi (HTTP ${yanit.status})`)
  }
  return yanit.json()
}

export async function mudurlukleriGetir(): Promise<Mudurluk[]> {
  const yanit = await fetch('/api/mudurlukler')
  if (!yanit.ok) {
    throw new Error(`Müdürlükler getirilemedi (HTTP ${yanit.status})`)
  }
  return yanit.json()
}

async function yaz<T>(yol: string, govde: unknown): Promise<T> {
  const yanit = await fetch(yol, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(govde),
  })
  if (!yanit.ok) {
    const hataGovdesi = await yanit.json().catch(() => null)
    throw new Error(hataGovdesi?.hata ?? `İşlem başarısız oldu (HTTP ${yanit.status})`)
  }
  return yanit.json()
}

export function talebiMudurlugeAta(takipNo: string, mudurlukAdi: string): Promise<TalepDetay> {
  return yaz(`/api/talepler/${encodeURIComponent(takipNo)}/mudurluk`, { mudurlukAdi })
}

export function talepDurumGuncelle(takipNo: string, durum: TalepDurumu): Promise<TalepDetay> {
  return yaz(`/api/talepler/${encodeURIComponent(takipNo)}/durum`, { durum })
}

export function talepOncelikGuncelle(takipNo: string, oncelik: TalepOnceligi): Promise<TalepDetay> {
  return yaz(`/api/talepler/${encodeURIComponent(takipNo)}/oncelik`, { oncelik })
}

export function talebeNotEkle(takipNo: string, notMetni: string, personel: string): Promise<TalepDetay> {
  return yaz(`/api/talepler/${encodeURIComponent(takipNo)}/notlar`, { notMetni, personel })
}

export function talepKategoriGuncelle(takipNo: string, kategori: string): Promise<TalepDetay> {
  return yaz(`/api/talepler/${encodeURIComponent(takipNo)}/kategori`, { kategori })
}

export async function aiOnerisiGetir(takipNo: string): Promise<SiniflandirmaOnerisi> {
  const yanit = await fetch(`/api/talepler/${encodeURIComponent(takipNo)}/ai-oneri`)
  if (!yanit.ok) {
    throw new Error(`AI önerisi alınamadı (HTTP ${yanit.status})`)
  }
  return yanit.json()
}

export async function topluAiOnerisiGetir(limit?: number): Promise<TopluSiniflandirmaOnerisi[]> {
  const params = limit ? `?limit=${limit}` : ''
  const yanit = await fetch(`/api/talepler/ai-oneri-toplu${params}`)
  if (!yanit.ok) {
    throw new Error(`Toplu AI önerisi alınamadı (HTTP ${yanit.status})`)
  }
  return yanit.json()
}
