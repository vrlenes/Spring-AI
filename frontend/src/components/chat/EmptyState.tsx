import { ClipboardList, Sparkles, type LucideIcon } from 'lucide-react'
import karatayLogo from '@/assets/karatay-logo.png'
import { cn } from '@/lib/utils'
import { useSohbetStore } from '@/stores/useSohbetStore'
import type { SohbetModu } from '@/types/chat'

const MEVZUAT_SORULARI = [
  'Resmi yazışmalarda imza yetkisi kime aittir?',
  'İmar planlarında kim yetkilidir?',
  'Belediyenin görev ve sorumlulukları nelerdir?',
]

const TALEP_ORNEKLERI = [
  'Açık talepleri listele',
  'Fen İşleri Müdürlüğü ile ilgili talepleri göster',
  'Başıboş hayvan şikayetlerini ara',
  'Son 30 gündeki talep istatistiklerini göster',
]

interface ModKart {
  mod: SohbetModu
  baslik: string
  aciklama: string
  Icon: LucideIcon
}

// AI Studio'nun "Build with Agents" kart izgarasindan esinlenildi - mod
// secimi artik sol paneldeki kucuk toggle'a ek olarak, bos sohbet
// ekraninda daha belirgin bir "hangi asistanla konusuyorum" secimine
// donusuyor.
const MODLAR: ModKart[] = [
  { mod: 'GENEL', baslik: 'Genel', aciklama: 'Mevzuat sorularını yanıtlar, genel sohbet eder.', Icon: Sparkles },
  {
    mod: 'TALEP',
    baslik: 'Talep',
    aciklama: 'Vatandaş taleplerini listeler, sınıflandırır, yönetir.',
    Icon: ClipboardList,
  },
]

function ModKartlari() {
  const aktifMod = useSohbetStore((s) => s.aktifMod)
  const modDegistir = useSohbetStore((s) => s.modDegistir)

  return (
    <div className="grid w-full max-w-md grid-cols-2 gap-2.5">
      {MODLAR.map(({ mod, baslik, aciklama, Icon }) => {
        const secili = mod === aktifMod
        return (
          <button
            key={mod}
            type="button"
            onClick={() => modDegistir(mod)}
            className={cn(
              'flex flex-col items-start gap-1.5 rounded-xl border p-3 text-left transition-colors',
              secili ? 'border-primary bg-primary/5' : 'border-border hover:bg-muted/50',
            )}
          >
            <span
              className={cn(
                'flex size-7 items-center justify-center rounded-lg',
                secili ? 'bg-primary text-primary-foreground' : 'bg-muted text-muted-foreground',
              )}
            >
              <Icon className="size-4" />
            </span>
            <span className="text-[12.5px] font-medium text-foreground">{baslik}</span>
            <span className="text-[11px] leading-snug text-muted-foreground">{aciklama}</span>
          </button>
        )
      })}
    </div>
  )
}

function OrnekGrubu({
  baslik,
  ornekler,
  onSelect,
}: {
  baslik: string
  ornekler: string[]
  onSelect: (soru: string) => void
}) {
  return (
    <div className="flex flex-col items-center gap-1.5">
      <p className="text-[11px] font-medium tracking-wide text-muted-foreground uppercase">{baslik}</p>
      <div className="flex flex-wrap justify-center gap-1.5">
        {ornekler.map((soru) => (
          <button
            key={soru}
            type="button"
            onClick={() => onSelect(soru)}
            className="rounded-full border bg-background px-3.5 py-1.5 text-[12.5px] text-foreground transition-colors hover:border-primary/40 hover:bg-accent hover:text-accent-foreground"
          >
            {soru}
          </button>
        ))}
      </div>
    </div>
  )
}

interface EmptyStateProps {
  onSelect: (soru: string) => void
}

export function EmptyState({ onSelect }: EmptyStateProps) {
  const aktifMod = useSohbetStore((s) => s.aktifMod)
  const ornekler = aktifMod === 'GENEL' ? MEVZUAT_SORULARI : TALEP_ORNEKLERI
  const ornekBaslik = aktifMod === 'GENEL' ? 'Örnek Sorular' : 'Örnek İstekler'

  return (
    <div className="flex h-full flex-col items-center justify-center gap-5 px-6 text-center">
      <img src={karatayLogo} alt="" className="h-14 w-auto" />
      <div className="space-y-1">
        <p className="text-[14px] font-medium">Nasıl yardımcı olabilirim?</p>
        <p className="max-w-sm text-[12.5px] text-muted-foreground">
          Bir mod seçin: mevzuat sorularını sorabilir ya da vatandaş taleplerini listeleyip
          yönetebilirsiniz.
        </p>
      </div>

      <ModKartlari />

      <div className="flex max-w-md flex-col gap-4">
        <OrnekGrubu baslik={ornekBaslik} ornekler={ornekler} onSelect={onSelect} />
      </div>
    </div>
  )
}
