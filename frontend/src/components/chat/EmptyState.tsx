import { Building2 } from 'lucide-react'

const ORNEK_SORULAR = [
  'Sen kimsin, neler yapabilirsin?',
  'Resmi yazışmalarda imza yetkisi nasıl düzenlenir?',
  'Bir vatandaş talebini nasıl takip edebilirim?',
]

interface EmptyStateProps {
  onSelect: (soru: string) => void
}

export function EmptyState({ onSelect }: EmptyStateProps) {
  return (
    <div className="flex h-full flex-col items-center justify-center gap-4 px-6 text-center">
      <span className="flex size-11 items-center justify-center rounded-full bg-primary text-primary-foreground">
        <Building2 className="size-5" />
      </span>
      <div className="space-y-1">
        <p className="text-[14px] font-medium">Nasıl yardımcı olabilirim?</p>
        <p className="max-w-sm text-[12.5px] text-muted-foreground">
          Mevzuat/prosedür sorularınızı veya vatandaş talepleriyle ilgili sorularınızı
          yazabilirsiniz.
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
