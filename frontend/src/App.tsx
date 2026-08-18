import { useRef, useState } from 'react'
import { Button } from '@/components/ui/button'
import { MessageBubble } from '@/components/chat/MessageBubble'
import { TypingDots } from '@/components/chat/TypingDots'
import { ChatComposer } from '@/components/chat/ChatComposer'
import { streamChat } from '@/lib/chatStream'
import type { ChatMessage } from '@/types/chat'

function App() {
  const [mesajlar, setMesajlar] = useState<ChatMessage[]>([])
  const [gonderiliyor, setGonderiliyor] = useState(false)
  const [streamingId, setStreamingId] = useState<string | null>(null)
  const conversationIdRef = useRef<string | null>(null)
  const viewportRef = useRef<HTMLDivElement>(null)

  function enAltaKaydir() {
    requestAnimationFrame(() => {
      viewportRef.current?.scrollTo({ top: viewportRef.current.scrollHeight, behavior: 'smooth' })
    })
  }

  async function mesajGonder(mesaj: string) {
    const kullaniciMesaji: ChatMessage = { id: crypto.randomUUID(), role: 'kullanici', content: mesaj }
    const asistanId = crypto.randomUUID()

    setMesajlar((onceki) => [...onceki, kullaniciMesaji, { id: asistanId, role: 'asistan', content: '' }])
    setGonderiliyor(true)
    setStreamingId(asistanId)
    enAltaKaydir()

    try {
      await streamChat({ conversationId: conversationIdRef.current, mesaj }, (olay) => {
        if (olay.type === 'conversationId') {
          conversationIdRef.current = olay.conversationId
          return
        }
        setMesajlar((onceki) =>
          onceki.map((m) => (m.id === asistanId ? { ...m, content: m.content + olay.text } : m)),
        )
        enAltaKaydir()
      })
    } catch {
      setMesajlar((onceki) =>
        onceki.map((m) =>
          m.id === asistanId ? { ...m, content: 'Bir hata oluştu, lütfen tekrar deneyin.' } : m,
        ),
      )
    } finally {
      setGonderiliyor(false)
      setStreamingId(null)
    }
  }

  function yeniKonusmaBaslat() {
    conversationIdRef.current = null
    setMesajlar([])
  }

  return (
    <div className="flex h-screen flex-col bg-muted/20">
      <header className="flex shrink-0 items-center justify-between border-b bg-background px-4 py-3">
        <h1 className="text-[13px] font-semibold tracking-tight">Karatay Belediyesi AI Asistanı</h1>
        <Button variant="ghost" size="sm" onClick={yeniKonusmaBaslat} className="text-[12.5px]">
          Yeni Konuşma
        </Button>
      </header>

      <div ref={viewportRef} className="min-h-0 flex-1 overflow-y-auto">
        <div className="mx-auto flex max-w-3xl flex-col gap-4 p-4">
          {mesajlar.length === 0 && (
            <p className="mt-16 text-center text-[13px] text-muted-foreground">
              Mevzuat sorularınızı veya vatandaş talepleriyle ilgili sorularınızı yazabilirsiniz.
            </p>
          )}
          {mesajlar.map((m) =>
            m.role === 'asistan' && m.id === streamingId && m.content === '' ? (
              <TypingDots key={m.id} />
            ) : (
              <MessageBubble key={m.id} message={m} streaming={m.id === streamingId} />
            ),
          )}
        </div>
      </div>

      <div className="mx-auto w-full max-w-3xl">
        <ChatComposer disabled={gonderiliyor} onSend={mesajGonder} />
      </div>
    </div>
  )
}

export default App
