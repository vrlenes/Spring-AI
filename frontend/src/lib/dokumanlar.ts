import type { Dokuman } from '@/types/dokuman'

export async function dokumanlariListele(): Promise<Dokuman[]> {
  const yanit = await fetch('/api/dokumanlar')
  if (!yanit.ok) {
    throw new Error(`Dokümanlar listelenemedi (HTTP ${yanit.status})`)
  }
  return yanit.json()
}

export async function dokumanYukle(dosya: File, baslik: string, kategori: string): Promise<Dokuman> {
  const form = new FormData()
  form.append('dosya', dosya)
  form.append('baslik', baslik)
  if (kategori.trim()) {
    form.append('kategori', kategori)
  }

  const yanit = await fetch('/api/dokumanlar', { method: 'POST', body: form })
  if (!yanit.ok) {
    throw new Error(`Doküman yüklenemedi (HTTP ${yanit.status})`)
  }
  return yanit.json()
}

export async function dokumanSil(id: number): Promise<void> {
  const yanit = await fetch(`/api/dokumanlar/${id}`, { method: 'DELETE' })
  if (!yanit.ok) {
    throw new Error(`Doküman silinemedi (HTTP ${yanit.status})`)
  }
}
