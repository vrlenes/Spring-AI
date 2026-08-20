import type { Personel } from '@/types/auth'

export async function girisYap(kullaniciAdi: string, sifre: string): Promise<Personel> {
  const yanit = await fetch('/api/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ kullaniciAdi, sifre }),
  })
  if (!yanit.ok) {
    const hataGovdesi = await yanit.json().catch(() => null)
    throw new Error(hataGovdesi?.hata ?? `Giriş başarısız oldu (HTTP ${yanit.status})`)
  }
  return yanit.json()
}

export async function cikisYap(): Promise<void> {
  await fetch('/api/auth/logout', { method: 'POST' })
}

export async function mevcutPersoneliGetir(): Promise<Personel | null> {
  const yanit = await fetch('/api/auth/me')
  if (yanit.status === 401) return null
  if (!yanit.ok) {
    throw new Error(`Oturum bilgisi alınamadı (HTTP ${yanit.status})`)
  }
  return yanit.json()
}
