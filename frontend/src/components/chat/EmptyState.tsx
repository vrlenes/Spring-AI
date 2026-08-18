import karatayLogo from '@/assets/karatay-logo.png'

const ORNEK_SORULAR = [
  'Resmi yazışmalarda imza yetkisi kime aittir?',
  'İmar planlarında kim yetkilidir?',
  'Belediyenin görev ve sorumlulukları nelerdir?',
  'Sen kimsin, neler yapabilirsin?',
]

interface EmptyStateProps {
  onSelect: (soru: string) => void
}

export function EmptyState({ onSelect }: EmptyStateProps) {
  return (
    <div className="flex h-full flex-col items-center justify-center gap-4 px-6 text-center">
      <img src={karatayLogo} alt="" className="h-14 w-auto" />
      <div className="space-y-1">
        <p className="text-[14px] font-medium">Nasıl yardımcı olabilirim?</p>
        <p className="max-w-sm text-[12.5px] text-muted-foreground">
          Sol paneldeki yüklü mevzuat belgeleriyle ilgili sorularınızı yazabilirsiniz.
        </p>
      </div>
      <div className="flex flex-col gap-1.5">
        {ORNEK_SORULAR.map((soru) => (
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
