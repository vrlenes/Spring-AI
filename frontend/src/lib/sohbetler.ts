import type { GeriBildirim, SohbetMesajOzeti, SohbetOzeti } from '@/types/chat'

export async function sohbetleriGetir(): Promise<SohbetOzeti[]> {
  const yanit = await fetch('/api/sohbetler')
  if (!yanit.ok) {
    throw new Error(`Sohbet geçmişi alınamadı (HTTP ${yanit.status})`)
  }
  return yanit.json()
}

export async function sohbetMesajlariniGetir(sohbetId: string): Promise<SohbetMesajOzeti[]> {
  const yanit = await fetch(`/api/sohbetler/${encodeURIComponent(sohbetId)}/mesajlar`)
  if (!yanit.ok) {
    throw new Error(`Sohbet geçmişi alınamadı (HTTP ${yanit.status})`)
  }
  return yanit.json()
}

// deger null gonderilirse mevcut geri bildirim geri alinir (toggle-off).
export async function geriBildirimVer(
  sohbetId: string,
  mesajId: number,
  deger: GeriBildirim | null,
): Promise<SohbetMesajOzeti> {
  const yanit = await fetch(
    `/api/sohbetler/${encodeURIComponent(sohbetId)}/mesajlar/${mesajId}/geri-bildirim`,
    {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ deger }),
    },
  )
  if (!yanit.ok) {
    throw new Error(`Geri bildirim gönderilemedi (HTTP ${yanit.status})`)
  }
  return yanit.json()
}
