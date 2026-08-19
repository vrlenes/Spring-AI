import karatayLogo from '@/assets/karatay-logo.png'

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

interface EmptyStateProps {
  onSelect: (soru: string) => void
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

export function EmptyState({ onSelect }: EmptyStateProps) {
  return (
    <div className="flex h-full flex-col items-center justify-center gap-5 px-6 text-center">
      <img src={karatayLogo} alt="" className="h-14 w-auto" />
      <div className="space-y-1">
        <p className="text-[14px] font-medium">Nasıl yardımcı olabilirim?</p>
        <p className="max-w-sm text-[12.5px] text-muted-foreground">
          Sol paneldeki yüklü mevzuat belgeleriyle ilgili sorular sorabilir, ya da vatandaş
          taleplerini listeleyip yönetebilirsiniz. Bir talebi atamak veya güncellemek için önce
          onu listeleyin/arayın - sonuçlarda gördüğünüz takip numarasını kullanarak işlem
          yapabilirsiniz.
        </p>
      </div>
      <div className="flex max-w-md flex-col gap-4">
        <OrnekGrubu baslik="Mevzuat Soruları" ornekler={MEVZUAT_SORULARI} onSelect={onSelect} />
        <OrnekGrubu baslik="Talep Yönetimi" ornekler={TALEP_ORNEKLERI} onSelect={onSelect} />
      </div>
    </div>
  )
}
