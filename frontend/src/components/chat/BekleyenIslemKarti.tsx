import { useState } from 'react'
import { CheckCircle2, Clock, Loader2, XCircle } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { bekleyenIslemiIptalEt, bekleyenIslemiOnayla } from '@/lib/pendingActions'
import type { BekleyenIslem } from '@/types/chat'

type Durum = 'bekliyor' | 'isleniyor' | 'onaylandi' | 'iptalEdildi' | 'hata'

export function BekleyenIslemKarti({ bekleyenIslem }: { bekleyenIslem: BekleyenIslem }) {
  const [durum, setDurum] = useState<Durum>('bekliyor')
  const [hataMesaji, setHataMesaji] = useState<string | null>(null)

  async function onayla() {
    setDurum('isleniyor')
    try {
      await bekleyenIslemiOnayla(bekleyenIslem.id)
      setDurum('onaylandi')
    } catch (e) {
      setHataMesaji(e instanceof Error ? e.message : 'İşlem onaylanamadı.')
      setDurum('hata')
    }
  }

  async function iptalEt() {
    setDurum('isleniyor')
    try {
      await bekleyenIslemiIptalEt(bekleyenIslem.id)
      setDurum('iptalEdildi')
    } catch {
      setHataMesaji('İşlem iptal edilemedi.')
      setDurum('hata')
    }
  }

  return (
    <div className="mt-2.5 max-w-sm rounded-lg border bg-muted/40 px-3 py-2.5 text-[12.5px]">
      <p className="mb-2 flex items-center gap-1.5 font-medium text-foreground">
        <Clock className="size-3.5 shrink-0 text-muted-foreground" />
        {bekleyenIslem.takipNo} için onay bekleniyor
      </p>
      <p className="mb-2.5 leading-relaxed text-muted-foreground">{bekleyenIslem.aciklama}</p>

      {durum === 'bekliyor' && (
        <div className="flex gap-2">
          <Button size="sm" onClick={onayla}>
            Onayla
          </Button>
          <Button size="sm" variant="outline" onClick={iptalEt}>
            İptal
          </Button>
        </div>
      )}

      {durum === 'isleniyor' && (
        <p className="flex items-center gap-1.5 text-muted-foreground">
          <Loader2 className="size-3.5 animate-spin" /> İşleniyor...
        </p>
      )}

      {durum === 'onaylandi' && (
        <p className="flex items-center gap-1.5 font-medium text-emerald-600 dark:text-emerald-400">
          <CheckCircle2 className="size-3.5" /> Onaylandı ve uygulandı
        </p>
      )}

      {durum === 'iptalEdildi' && (
        <p className="flex items-center gap-1.5 font-medium text-muted-foreground">
          <XCircle className="size-3.5" /> İptal edildi
        </p>
      )}

      {durum === 'hata' && (
        <div className="flex flex-col gap-1.5">
          <p className="flex items-center gap-1.5 font-medium text-destructive">
            <XCircle className="size-3.5" /> {hataMesaji}
          </p>
          <div className="flex gap-2">
            <Button size="sm" onClick={onayla}>
              Tekrar Dene
            </Button>
            <Button size="sm" variant="outline" onClick={iptalEt}>
              İptal
            </Button>
          </div>
        </div>
      )}
    </div>
  )
}
