import type { ChatStreamEvent, SohbetModu } from '@/types/chat'

interface StreamChatRequest {
  conversationId: string | null
  mesaj: string
  mod: SohbetModu
}

/**
 * POST /api/chat/stream bir SSE gövdesi döndürüyor. Native EventSource sadece
 * GET destekler, bu yüzden akışı fetch + ReadableStream ile elle ayrıştırıyoruz.
 */
export async function streamChat(
  istek: StreamChatRequest,
  onEvent: (event: ChatStreamEvent) => void,
): Promise<void> {
  const yanit = await fetch('/api/chat/stream', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(istek),
  })

  if (!yanit.ok || !yanit.body) {
    throw new Error(`İstek başarısız oldu (HTTP ${yanit.status})`)
  }

  const okuyucu = yanit.body.getReader()
  const cozucu = new TextDecoder('utf-8')
  let tampon = ''

  while (true) {
    const { value, done } = await okuyucu.read()
    if (done) break

    tampon += cozucu.decode(value, { stream: true })
    const bloklar = tampon.split('\n\n')
    tampon = bloklar.pop() ?? ''

    for (const blok of bloklar) {
      if (!blok.trim()) continue

      let olayAdi = 'message'
      const dataSatirlari: string[] = []
      for (const satir of blok.split('\n')) {
        if (satir.startsWith('event:')) {
          olayAdi = satir.slice(6).trim()
        } else if (satir.startsWith('data:')) {
          dataSatirlari.push(satir.slice(5))
        }
      }
      const veri = dataSatirlari.join('\n')

      if (olayAdi === 'conversationId') {
        onEvent({ type: 'conversationId', conversationId: veri })
      } else if (olayAdi === 'token') {
        onEvent({ type: 'token', text: veri })
      } else if (olayAdi === 'kaynaklar') {
        try {
          onEvent({ type: 'kaynaklar', kaynaklar: JSON.parse(veri) })
        } catch {
          // sunucudan bozuk JSON gelirse kaynaklari sessizce yok say
        }
      } else if (olayAdi === 'bekleyenIslem') {
        try {
          onEvent({ type: 'bekleyenIslem', bekleyenIslem: JSON.parse(veri) })
        } catch {
          // sunucudan bozuk JSON gelirse bekleyen islemi sessizce yok say
        }
      } else if (olayAdi === 'araclar') {
        try {
          onEvent({ type: 'araclar', araclar: JSON.parse(veri) })
        } catch {
          // sunucudan bozuk JSON gelirse arac listesini sessizce yok say
        }
      }
    }
  }
}
