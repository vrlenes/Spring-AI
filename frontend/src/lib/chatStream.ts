import type { AracGrubu, ChatStreamEvent, SohbetModu } from '@/types/chat'

function dosyaliGovdeOlustur(istek: StreamChatRequest) {
  const form = new FormData()
  if (istek.conversationId) form.append('conversationId', istek.conversationId)
  form.append('mesaj', istek.mesaj)
  form.append('mod', istek.mod)
  istek.kapaliAraclar?.forEach((arac) => form.append('kapaliAraclar', arac))
  if (istek.dosya) form.append('dosya', istek.dosya)
  return form
}

interface StreamChatRequest {
  conversationId: string | null
  mesaj: string
  mod: SohbetModu
  kapaliAraclar?: AracGrubu[]
  dosya?: File
}

/**
 * POST /api/chat/stream bir SSE gövdesi döndürüyor. Native EventSource sadece
 * GET destekler, bu yüzden akışı fetch + ReadableStream ile elle ayrıştırıyoruz.
 * `dosya` varsa istek gövdesi FormData olur (backend'deki ayrı multipart
 * handler'a düşer, bkz. ChatController.chatStreamEkli) - Content-Type header'ı
 * elle set edilmez, tarayıcı multipart sınırını kendi ekler (dokumanlar.ts'teki
 * dokumanYukle ile ayni desen). SSE ayrıştırma döngüsü istek gövdesinden
 * bağımsız, hiç değişmedi.
 */
export async function streamChat(
  istek: StreamChatRequest,
  onEvent: (event: ChatStreamEvent) => void,
): Promise<void> {
  const yanit = istek.dosya
    ? await fetch('/api/chat/stream', { method: 'POST', body: dosyaliGovdeOlustur(istek) })
    : await fetch('/api/chat/stream', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          conversationId: istek.conversationId,
          mesaj: istek.mesaj,
          mod: istek.mod,
          kapaliAraclar: istek.kapaliAraclar,
        }),
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
      } else if (olayAdi === 'yapisalVeri') {
        try {
          onEvent({ type: 'yapisalVeri', yapisalVeri: JSON.parse(veri) })
        } catch {
          // sunucudan bozuk JSON gelirse yapisal veriyi sessizce yok say
        }
      } else if (olayAdi === 'algilananMod') {
        onEvent({ type: 'algilananMod', mod: veri as SohbetModu })
      } else if (olayAdi === 'dogrulama') {
        try {
          onEvent({ type: 'dogrulama', dogrulama: JSON.parse(veri) })
        } catch {
          // sunucudan bozuk JSON gelirse dogrulama rozetini sessizce yok say
        }
      } else if (olayAdi === 'hata') {
        onEvent({ type: 'hata', mesaj: veri })
      }
    }
  }
}
