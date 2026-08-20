import { useEffect, useRef } from 'react'
import { Loader2 } from 'lucide-react'
import karatayLogo from '@/assets/karatay-logo.png'
import { LoginPage } from '@/components/auth/LoginPage'
import { MessageBubble } from '@/components/chat/MessageBubble'
import { TypingDots } from '@/components/chat/TypingDots'
import { EmptyState } from '@/components/chat/EmptyState'
import { ChatComposer } from '@/components/chat/ChatComposer'
import { SohbetGecmisiSidebar } from '@/components/chat/SohbetGecmisiSidebar'
import { SagPanel } from '@/components/layout/SagPanel'
import { useAuthStore } from '@/stores/useAuthStore'
import { useSohbetStore } from '@/stores/useSohbetStore'

function AnaLayout() {
  const { mesajlar, gonderiliyor, streamingId, mesajGonder } = useSohbetStore()
  const viewportRef = useRef<HTMLDivElement>(null)

  function enAltaKaydir() {
    requestAnimationFrame(() => {
      viewportRef.current?.scrollTo({ top: viewportRef.current.scrollHeight, behavior: 'smooth' })
    })
  }

  async function gonder(mesaj: string) {
    enAltaKaydir()
    await mesajGonder(mesaj)
  }

  useEffect(() => {
    if (mesajlar.length > 0) enAltaKaydir()
  }, [mesajlar.length])

  return (
    <div className="flex h-screen w-full bg-background">
      <SohbetGecmisiSidebar />

      <main className="flex min-w-0 flex-1 flex-col">
        <header className="flex shrink-0 items-center gap-3 border-b px-4 py-2.5 md:hidden">
          <img src={karatayLogo} alt="Karatay Belediyesi" className="h-8 w-auto" />
        </header>

        <div ref={viewportRef} className="min-h-0 flex-1 overflow-y-auto">
          {mesajlar.length === 0 ? (
            <EmptyState onSelect={gonder} />
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
          <ChatComposer disabled={gonderiliyor} onSend={gonder} />
        </div>
      </main>

      <SagPanel />
    </div>
  )
}

function App() {
  const { personel, kontrolEdiliyor, oturumKontrolEt } = useAuthStore()

  useEffect(() => {
    oturumKontrolEt()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  if (kontrolEdiliyor) {
    return (
      <div className="flex h-screen w-full items-center justify-center bg-background">
        <Loader2 className="size-6 animate-spin text-muted-foreground" />
      </div>
    )
  }

  if (!personel) {
    return <LoginPage />
  }

  return <AnaLayout />
}

export default App
