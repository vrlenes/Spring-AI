import { useRef, useState } from 'react'
import { Building2 } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { MessageBubble } from '@/components/chat/MessageBubble'
import { TypingDots } from '@/components/chat/TypingDots'
import { EmptyState } from '@/components/chat/EmptyState'
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
    <div className="flex h-screen items-center justify-center bg-muted p-4 sm:p-8">
      <div className="flex h-full max-h-[880px] w-full max-w-2xl flex-col overflow-hidden rounded-2xl border bg-background shadow-xl">
        <header className="flex shrink-0 items-center justify-between border-b px-4 py-3">
          <div className="flex items-center gap-2.5">
            <span className="flex size-7 items-center justify-center rounded-full bg-primary text-primary-foreground">
              <Building2 className="size-3.5" />
            </span>
            <div className="leading-tight">
              <h1 className="text-[13px] font-semibold tracking-tight">Karatay Belediyesi</h1>
              <p className="text-[11px] text-muted-foreground">AI Asistanı</p>
            </div>
          </div>
          <Button variant="ghost" size="sm" onClick={yeniKonusmaBaslat} className="text-[12.5px]">
            Yeni Konuşma
          </Button>
        </header>

        <div ref={viewportRef} className="min-h-0 flex-1 overflow-y-auto">
          {mesajlar.length === 0 ? (
            <EmptyState onSelect={mesajGonder} />
          ) : (
            <div className="flex flex-col gap-4 p-4">
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

        <ChatComposer disabled={gonderiliyor} onSend={mesajGonder} />
      </div>
    </div>
  )
}

export default App
