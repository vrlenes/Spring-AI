import { useEffect, useMemo, useState } from 'react'
import { LogOut, MessageSquare, Moon, Plus, Search, Sun, X } from 'lucide-react'
import karatayLogo from '@/assets/karatay-logo.png'
import { Button } from '@/components/ui/button'
import { useAuthStore } from '@/stores/useAuthStore'
import { useSohbetStore } from '@/stores/useSohbetStore'
import { useTemaStore } from '@/stores/useTemaStore'
import { cn } from '@/lib/utils'
import type { SohbetModu } from '@/types/chat'

const MOD_ETIKETLERI: Record<SohbetModu, string> = {
  GENEL: 'Genel',
  TALEP: 'Talep',
  IMAR: 'İmar',
  RUHSAT: 'Ruhsat',
  OTOMATIK: 'Otomatik',
}

export function SohbetGecmisiSidebar() {
  const { personel, cikisYap } = useAuthStore()
  const { sohbetListesi, aktifSohbetId, sohbetListesiniYukle, yeniKonusma, sohbetSec } = useSohbetStore()
  const { tema, temaDegistir } = useTemaStore()
  const [arama, setArama] = useState('')

  useEffect(() => {
    sohbetListesiniYukle()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const filtrelenmisListe = useMemo(() => {
    const sorgu = arama.trim().toLocaleLowerCase('tr')
    if (!sorgu) return sohbetListesi
    return sohbetListesi.filter((s) => {
      const baslik = (s.baslik ?? 'Yeni konuşma').toLocaleLowerCase('tr')
      const mod = MOD_ETIKETLERI[s.mod].toLocaleLowerCase('tr')
      return baslik.includes(sorgu) || mod.includes(sorgu)
    })
  }, [sohbetListesi, arama])

  return (
    <aside className="hidden w-64 shrink-0 flex-col border-r bg-muted/30 md:flex">
      <div className="flex items-center gap-2.5 border-b px-4 py-4">
        <img src={karatayLogo} alt="Karatay Belediyesi" className="h-9 w-auto" />
        <div className="leading-tight">
          <p className="text-[13px] font-semibold tracking-tight">Karatay Belediyesi</p>
          <p className="text-[11px] text-muted-foreground">AI Asistanı</p>
        </div>
      </div>

      <div className="flex flex-col gap-2 p-3">
        <Button onClick={() => yeniKonusma()} variant="outline" className="w-full justify-start gap-2">
          <Plus className="size-4" />
          Yeni Konuşma
        </Button>

        {sohbetListesi.length > 0 && (
          <div className="relative">
            <Search className="pointer-events-none absolute top-1/2 left-2.5 size-3.5 -translate-y-1/2 text-muted-foreground" />
            <input
              value={arama}
              onChange={(e) => setArama(e.target.value)}
              placeholder="Konuşmalarda ara..."
              autoComplete="off"
              className="w-full rounded-md border bg-background py-1.5 pr-7 pl-8 text-[12.5px] outline-none focus:border-foreground/20"
            />
            {arama && (
              <button
                type="button"
                onClick={() => setArama('')}
                aria-label="Aramayı temizle"
                className="absolute top-1/2 right-1.5 -translate-y-1/2 rounded p-0.5 text-muted-foreground hover:bg-muted hover:text-foreground"
              >
                <X className="size-3.5" />
              </button>
            )}
          </div>
        )}
      </div>

      <div className="min-h-0 flex-1 overflow-y-auto px-2 py-1">
        {sohbetListesi.length === 0 && (
          <p className="px-2 py-3 text-[11.5px] text-muted-foreground">Henüz bir konuşma yok.</p>
        )}
        {sohbetListesi.length > 0 && filtrelenmisListe.length === 0 && (
          <p className="px-2 py-3 text-[11.5px] text-muted-foreground">"{arama}" ile eşleşen bir konuşma yok.</p>
        )}
        <div className="flex flex-col gap-0.5">
          {filtrelenmisListe.map((s) => (
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
        <div className="flex shrink-0 items-center gap-1">
          <button
            type="button"
            onClick={temaDegistir}
            aria-label={tema === 'dark' ? 'Açık temaya geç' : 'Koyu temaya geç'}
            className="rounded-md p-1.5 text-muted-foreground hover:bg-muted hover:text-foreground"
          >
            {tema === 'dark' ? <Sun className="size-3.5" /> : <Moon className="size-3.5" />}
          </button>
          <button
            type="button"
            onClick={cikisYap}
            className="flex items-center gap-1 text-[11.5px] text-muted-foreground hover:text-foreground"
          >
            <LogOut className="size-3.5" /> Çıkış
          </button>
        </div>
      </div>
    </aside>
  )
}
