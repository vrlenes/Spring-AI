import { FileText } from 'lucide-react'
import type { EkOnizleme as EkOnizlemeTipi } from '@/types/chat'

interface EkOnizlemeProps {
  ek: EkOnizlemeTipi
}

export function EkOnizleme({ ek }: EkOnizlemeProps) {
  if (ek.mimeTipi.startsWith('image/')) {
    return (
      <a href={ek.url} target="_blank" rel="noreferrer" className="mb-1.5 block w-fit">
        <img src={ek.url} alt={ek.dosyaAdi} className="max-h-48 rounded-lg border object-cover" />
      </a>
    )
  }

  return (
    <a
      href={ek.url}
      target="_blank"
      rel="noreferrer"
      className="mb-1.5 flex w-fit items-center gap-1.5 rounded-lg border bg-background/60 px-2.5 py-1.5 text-[12px] hover:bg-background"
    >
      <FileText className="size-3.5 shrink-0 text-muted-foreground" />
      <span className="max-w-48 truncate">{ek.dosyaAdi}</span>
    </a>
  )
}
