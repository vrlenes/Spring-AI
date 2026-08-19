import { cn } from '@/lib/utils'
import type { TalepDurumu, TalepOnceligi } from '@/types/talep'

const DURUM_RENKLERI: Record<TalepDurumu, string> = {
  YENI: 'bg-blue-500/10 text-blue-600 dark:text-blue-400',
  ATANDI: 'bg-amber-500/10 text-amber-600 dark:text-amber-400',
  ISLEMDE: 'bg-violet-500/10 text-violet-600 dark:text-violet-400',
  COZULDU: 'bg-emerald-500/10 text-emerald-600 dark:text-emerald-400',
  REDDEDILDI: 'bg-muted text-muted-foreground',
}

const ONCELIK_RENKLERI: Record<TalepOnceligi, string> = {
  DUSUK: 'text-muted-foreground',
  NORMAL: 'text-foreground',
  YUKSEK: 'text-amber-600 dark:text-amber-400',
  ACIL: 'text-destructive font-semibold',
}

export function DurumRozeti({ durum }: { durum: TalepDurumu }) {
  return (
    <span className={cn('inline-flex shrink-0 items-center rounded-full px-2 py-0.5 text-[11px] font-medium', DURUM_RENKLERI[durum])}>
      {durum}
    </span>
  )
}

export function OncelikMetni({ oncelik }: { oncelik: TalepOnceligi }) {
  return <span className={cn('text-[12px]', ONCELIK_RENKLERI[oncelik])}>{oncelik}</span>
}
