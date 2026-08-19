export async function bekleyenIslemiOnayla(id: string): Promise<void> {
  const yanit = await fetch(`/api/pending-actions/${id}/onayla`, { method: 'POST' })
  if (!yanit.ok) {
    const govde = await yanit.json().catch(() => null)
    throw new Error(govde?.hata ?? `İşlem onaylanamadı (HTTP ${yanit.status})`)
  }
}

export async function bekleyenIslemiIptalEt(id: string): Promise<void> {
  const yanit = await fetch(`/api/pending-actions/${id}/iptal`, { method: 'POST' })
  if (!yanit.ok) {
    throw new Error(`İşlem iptal edilemedi (HTTP ${yanit.status})`)
  }
}
