import { useEffect, useRef, useState } from 'react'
import { Building2, ChevronDown, ClipboardList, FileCheck2, Sparkles, Wand2, type LucideIcon } from 'lucide-react'
import { cn } from '@/lib/utils'
import { useSohbetStore } from '@/stores/useSohbetStore'
import type { SohbetModu } from '@/types/chat'

interface ModTanimi {
  mod: SohbetModu
  baslik: string
  aciklama: string
  Icon: LucideIcon
}

const MODLAR: ModTanimi[] = [
  {
    mod: 'OTOMATIK',
    baslik: 'Otomatik',
    aciklama: 'Sorunuzu okuyup hangi asistana ait olduğuna kendisi karar verir.',
    Icon: Wand2,
  },
  { mod: 'GENEL', baslik: 'Genel', aciklama: 'Mevzuat sorularını yanıtlar, genel sohbet eder.', Icon: Sparkles },
  {
    mod: 'TALEP',
    baslik: 'Talep',
    aciklama: 'Vatandaş taleplerini listeler, sınıflandırır, yönetir.',
    Icon: ClipboardList,
  },
  {
    mod: 'IMAR',
    baslik: 'İmar',
    aciklama: 'İmar kanunu ve yönetmeliklerine dair sorulara cevap verir.',
    Icon: Building2,
  },
  {
    mod: 'RUHSAT',
    baslik: 'Ruhsat',
    aciklama: 'İşyeri/yapı ruhsatı mevzuatına dair sorulara cevap verir.',
    Icon: FileCheck2,
  },
]

// EmptyState'teki buyuk mod kartlari SADECE bos ekranda gorunur - bir
// konusma basladiktan sonra modu degistirebilecek baska hicbir yer yoktu.
// Bu, her zaman gorunen kucuk acilir menu o boslugu dolduruyor - mod
// degisince konusma KAPANMAZ, ayni sohbette devam eder (bkz.
// useSohbetStore.modDegistir).
export function ModSwitcher() {
  const [acik, setAcik] = useState(false)
  const kutuRef = useRef<HTMLDivElement>(null)
  const aktifMod = useSohbetStore((s) => s.aktifMod)
  const modDegistir = useSohbetStore((s) => s.modDegistir)
  const aktif = MODLAR.find((m) => m.mod === aktifMod) ?? MODLAR[1]

  useEffect(() => {
    if (!acik) return
    function disariTiklandi(e: MouseEvent) {
      if (kutuRef.current && !kutuRef.current.contains(e.target as Node)) {
        setAcik(false)
      }
    }
    document.addEventListener('mousedown', disariTiklandi)
    return () => document.removeEventListener('mousedown', disariTiklandi)
  }, [acik])

  return (
    <div ref={kutuRef} className="relative">
      <button
        type="button"
        onClick={() => setAcik((a) => !a)}
        className="flex items-center gap-1.5 rounded-full border bg-muted/40 px-2.5 py-1 text-[12px] font-medium text-foreground hover:bg-muted"
      >
        <aktif.Icon className="size-3.5 text-primary" />
        {aktif.baslik}
        <ChevronDown className={cn('size-3 text-muted-foreground transition-transform', acik && 'rotate-180')} />
      </button>
      {acik && (
        <div className="absolute top-full left-0 z-20 mt-1 w-64 rounded-lg border bg-popover p-1 shadow-lg">
          {MODLAR.map(({ mod, baslik, aciklama, Icon }) => (
            <button
              key={mod}
              type="button"
              onClick={() => {
                modDegistir(mod)
                setAcik(false)
              }}
              className={cn(
                'flex w-full items-start gap-2 rounded-md px-2 py-1.5 text-left',
                mod === aktifMod ? 'bg-muted' : 'hover:bg-muted/60',
              )}
            >
              <Icon className="mt-0.5 size-3.5 shrink-0 text-primary" />
              <span className="min-w-0">
                <span className={cn('block text-[12.5px]', mod === aktifMod ? 'font-medium text-foreground' : 'text-foreground')}>
                  {baslik}
                </span>
                <span className="block text-[11px] leading-snug text-muted-foreground">{aciklama}</span>
              </span>
            </button>
          ))}
        </div>
      )}
    </div>
  )
}
