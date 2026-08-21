import { ClipboardList, Contact, FileSearch } from 'lucide-react'
import { useSohbetStore } from '@/stores/useSohbetStore'
import { cn } from '@/lib/utils'
import type { AracGrubu } from '@/types/chat'

interface AracTanimi {
  arac: AracGrubu
  baslik: string
  aciklama: string
  Icon: typeof FileSearch
}

const ARACLAR: AracTanimi[] = [
  { arac: 'RAG', baslik: 'Mevzuat Arama', aciklama: 'Yüklenmiş belgelerde arama yapar (belgeAra).', Icon: FileSearch },
  {
    arac: 'TALEP',
    baslik: 'Talep Araçları',
    aciklama: 'Talep listeleme, atama, durum/öncelik güncelleme.',
    Icon: ClipboardList,
  },
  {
    arac: 'KURUM_DIZIN',
    baslik: 'Kurum Dizini',
    aciklama: 'Müdürlük iletişim bilgisi ve personel arama.',
    Icon: Contact,
  },
]

// AI Studio'nun "Run settings" panelindeki tool toggle listesinden ilham
// alindi - burada kapatmak SERT bir engelleme degil, ayni TALEP modunun
// belgeAra'yi yasaklamasi gibi sistem promptuna eklenen bir kural (bkz.
// backend ChatService.sistemPromptuOlustur). Konusma bazli degil, oturum
// bazli - yeni konusma acinca sifirlanmaz.
export function AracPaneli() {
  const kapaliAraclar = useSohbetStore((s) => s.kapaliAraclar)
  const aracToggle = useSohbetStore((s) => s.aracToggle)

  return (
    <div className="flex flex-col gap-1.5 border-b px-4 py-3">
      <p className="text-[11px] font-medium tracking-wide text-muted-foreground uppercase">Araçlar</p>
      {ARACLAR.map(({ arac, baslik, aciklama, Icon }) => {
        const acik = !kapaliAraclar.has(arac)
        return (
          <button
            key={arac}
            type="button"
            onClick={() => aracToggle(arac)}
            title={aciklama}
            className="flex items-center gap-2 rounded-lg px-1.5 py-1 text-left transition-colors hover:bg-muted/60"
          >
            <Icon className={cn('size-3.5 shrink-0', acik ? 'text-foreground' : 'text-muted-foreground')} />
            <span className={cn('min-w-0 flex-1 truncate text-[12px]', acik ? 'text-foreground' : 'text-muted-foreground')}>
              {baslik}
            </span>
            <span
              className={cn(
                'relative h-4.5 w-8 shrink-0 rounded-full transition-colors',
                acik ? 'bg-primary' : 'bg-muted-foreground/30',
              )}
            >
              <span
                className={cn(
                  'absolute top-0.5 size-3.5 rounded-full bg-background shadow transition-transform',
                  acik ? 'translate-x-3.5' : 'translate-x-0.5',
                )}
              />
            </span>
          </button>
        )
      })}
    </div>
  )
}
