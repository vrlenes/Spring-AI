import { useEffect } from 'react'
import { LogOut, MessageSquare, Plus } from 'lucide-react'
import karatayLogo from '@/assets/karatay-logo.png'
import { Button } from '@/components/ui/button'
import { useAuthStore } from '@/stores/useAuthStore'
import { useSohbetStore } from '@/stores/useSohbetStore'
import { cn } from '@/lib/utils'
import type { SohbetModu } from '@/types/chat'

const MOD_ETIKETLERI: Record<SohbetModu, string> = { GENEL: 'Genel', TALEP: 'Talep', IMAR: 'İmar', RUHSAT: 'Ruhsat' }

export function SohbetGecmisiSidebar() {
  const { personel, cikisYap } = useAuthStore()
  const { sohbetListesi, aktifSohbetId, sohbetListesiniYukle, yeniKonusma, sohbetSec } = useSohbetStore()

  useEffect(() => {
    sohbetListesiniYukle()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  return (
    <aside className="hidden w-64 shrink-0 flex-col border-r bg-muted/30 md:flex">
      <div className="flex items-center gap-2.5 border-b px-4 py-4">
        <img src={karatayLogo} alt="Karatay Belediyesi" className="h-9 w-auto" />
        <div className="leading-tight">
          <p className="text-[13px] font-semibold tracking-tight">Karatay Belediyesi</p>
          <p className="text-[11px] text-muted-foreground">AI Asistanı</p>
        </div>
      </div>

      <div className="p-3">
        <Button onClick={() => yeniKonusma()} variant="outline" className="w-full justify-start gap-2">
          <Plus className="size-4" />
          Yeni Konuşma
        </Button>
      </div>

      <div className="min-h-0 flex-1 overflow-y-auto px-2 py-1">
        {sohbetListesi.length === 0 && (
          <p className="px-2 py-3 text-[11.5px] text-muted-foreground">Henüz bir konuşma yok.</p>
        )}
        <div className="flex flex-col gap-0.5">
          {sohbetListesi.map((s) => (
            <button
              key={s.id}
              type="button"
              onClick={() => sohbetSec(s.id)}
              className={cn(
                'flex items-center gap-2 rounded-lg px-2.5 py-2 text-left text-[12.5px] transition-colors hover:bg-muted/70',
                s.id === aktifSohbetId && 'bg-muted',
              )}
            >
              <MessageSquare className="size-3.5 shrink-0 text-muted-foreground" />
              <span className="min-w-0 flex-1 truncate">{s.baslik ?? 'Yeni konuşma'}</span>
              <span className="shrink-0 rounded-full bg-muted-foreground/10 px-1.5 py-0.5 text-[10px] text-muted-foreground">
                {MOD_ETIKETLERI[s.mod]}
              </span>
            </button>
          ))}
        </div>
      </div>

      <div className="flex items-center justify-between gap-2 border-t px-4 py-3">
        <span className="min-w-0 truncate text-[12px] text-muted-foreground">{personel?.adSoyad}</span>
        <button
          type="button"
          onClick={cikisYap}
          className="flex shrink-0 items-center gap-1 text-[11.5px] text-muted-foreground hover:text-foreground"
        >
          <LogOut className="size-3.5" /> Çıkış
        </button>
      </div>
    </aside>
  )
}
