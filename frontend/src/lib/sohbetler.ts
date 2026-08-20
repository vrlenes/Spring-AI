import type { SohbetMesajOzeti, SohbetOzeti } from '@/types/chat'

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
