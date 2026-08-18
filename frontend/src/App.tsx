import { useRef, useState } from 'react'
import karatayLogo from '@/assets/karatay-logo.png'
import { MessageBubble } from '@/components/chat/MessageBubble'
import { TypingDots } from '@/components/chat/TypingDots'
import { EmptyState } from '@/components/chat/EmptyState'
import { ChatComposer } from '@/components/chat/ChatComposer'
import { Sidebar } from '@/components/chat/Sidebar'
import { Button } from '@/components/ui/button'
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
        if (olay.type === 'kaynaklar') {
          setMesajlar((onceki) =>
            onceki.map((m) => (m.id === asistanId ? { ...m, kaynaklar: olay.kaynaklar } : m)),
          )
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
    <div className="flex h-screen w-full bg-background">
      <Sidebar onYeniKonusma={yeniKonusmaBaslat} />

      <main className="flex min-w-0 flex-1 flex-col">
        <header className="flex shrink-0 items-center justify-between border-b px-4 py-3 md:hidden">
          <div className="flex items-center gap-2">
            <img src={karatayLogo} alt="Karatay Belediyesi" className="h-8 w-auto" />
            <span className="text-[13px] font-semibold">AI Asistanı</span>
          </div>
          <Button variant="ghost" size="sm" onClick={yeniKonusmaBaslat} className="text-[12.5px]">
            Yeni Konuşma
          </Button>
        </header>

        <div ref={viewportRef} className="min-h-0 flex-1 overflow-y-auto">
          {mesajlar.length === 0 ? (
            <EmptyState onSelect={mesajGonder} />
          ) : (
            <div className="mx-auto flex max-w-2xl flex-col gap-4 p-4">
              {mesajlar.map((m) =>
                m.role === 'asistan' && m.id === streamingId && m.content === '' ? (
                  <TypingDots key={m.id} />
                ) : (
                  <MessageBubble key={m.id} message={m} streaming={m.id === streamingId} />
                ),
              )}
            </div>
          )}
        </div>

        <div className="mx-auto w-full max-w-2xl">
          <ChatComposer disabled={gonderiliyor} onSend={mesajGonder} />
        </div>
      </main>
    </div>
  )
}

export default App
