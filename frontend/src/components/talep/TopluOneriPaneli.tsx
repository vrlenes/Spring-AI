import { useState } from 'react'
import { CheckCircle2, Sparkles, X, XCircle } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { talebiMudurlugeAta, talepKategoriGuncelle } from '@/lib/talepler'
import type { TopluSiniflandirmaOnerisi } from '@/types/talep'

type SatirDurumu = 'bekliyor' | 'uygulaniyor' | 'uygulandi' | 'reddedildi' | 'hata'

interface TopluOneriPaneliProps {
  oneriler: TopluSiniflandirmaOnerisi[]
  onKapat: () => void
  onDegisti: () => void
}

export function TopluOneriPaneli({ oneriler, onKapat, onDegisti }: TopluOneriPaneliProps) {
  const [durumlar, setDurumlar] = useState<Record<string, SatirDurumu>>({})
  const [tumuUygulaniyor, setTumuUygulaniyor] = useState(false)

  async function birSatiriUygula(item: TopluSiniflandirmaOnerisi) {
    setDurumlar((d) => ({ ...d, [item.takipNo]: 'uygulaniyor' }))
    try {
      if (item.oneri.kategori) {
        await talepKategoriGuncelle(item.takipNo, item.oneri.kategori)
      }
      if (item.oneri.mudurlukAdi) {
        await talebiMudurlugeAta(item.takipNo, item.oneri.mudurlukAdi)
      }
      setDurumlar((d) => ({ ...d, [item.takipNo]: 'uygulandi' }))
      onDegisti()
    } catch {
      setDurumlar((d) => ({ ...d, [item.takipNo]: 'hata' }))
    }
  }

  function birSatiriReddet(takipNo: string) {
    setDurumlar((d) => ({ ...d, [takipNo]: 'reddedildi' }))
  }

  async function tumunuUygula() {
    setTumuUygulaniyor(true)
    for (const item of oneriler) {
      const mevcut = durumlar[item.takipNo]
      if (!mevcut && item.oneri.mudurlukAdi) {
        await birSatiriUygula(item)
      }
    }
    setTumuUygulaniyor(false)
  }

  const uygulanabilirSayisi = oneriler.filter((o) => !durumlar[o.takipNo] && o.oneri.mudurlukAdi).length

  return (
    <div className="flex min-h-0 flex-1 flex-col overflow-y-auto">
      <div className="flex shrink-0 items-center justify-between border-b px-4 py-3">
        <p className="flex items-center gap-1.5 text-[13px] font-semibold">
          <Sparkles className="size-4 text-primary" /> AI Sınıflandırma Önerileri ({oneriler.length})
        </p>
        <div className="flex items-center gap-2">
          <Button size="sm" disabled={uygulanabilirSayisi === 0 || tumuUygulaniyor} onClick={tumunuUygula}>
            Tümünü Uygula ({uygulanabilirSayisi})
          </Button>
          <Button variant="ghost" size="icon-sm" onClick={onKapat}>
            <X className="size-4" />
          </Button>
        </div>
      </div>

      <div className="flex flex-col gap-2 p-4">
        {oneriler.map((item) => {
          const durum = durumlar[item.takipNo] ?? 'bekliyor'
          return (
            <div key={item.takipNo} className="rounded-lg border bg-muted/30 p-3 text-[12.5px]">
              <p className="font-mono text-[11.5px] text-muted-foreground">{item.takipNo}</p>
              <p className="mb-1.5 text-foreground">{item.konuMetni}</p>
              <p className="text-muted-foreground">
                Müdürlük: <span className="text-foreground">{item.oneri.mudurlukAdi ?? 'belirlenemedi'}</span>
                {item.oneri.kategori && (
                  <>
                    {' · '}Kategori: <span className="text-foreground">{item.oneri.kategori}</span>
                  </>
                )}
              </p>
              {item.oneri.gerekce && <p className="text-muted-foreground italic">"{item.oneri.gerekce}"</p>}

              <div className="mt-2">
                {durum === 'bekliyor' && (
                  <div className="flex gap-1.5">
                    <Button
                      size="sm"
                      disabled={!item.oneri.mudurlukAdi}
                      onClick={() => birSatiriUygula(item)}
                    >
                      Uygula
                    </Button>
                    <Button size="sm" variant="outline" onClick={() => birSatiriReddet(item.takipNo)}>
                      Reddet
                    </Button>
                  </div>
                )}
                {durum === 'uygulaniyor' && <p className="text-muted-foreground">İşleniyor...</p>}
                {durum === 'uygulandi' && (
                  <p className="flex items-center gap-1.5 font-medium text-emerald-600 dark:text-emerald-400">
                    <CheckCircle2 className="size-3.5" /> Uygulandı
                  </p>
                )}
                {durum === 'reddedildi' && (
                  <p className="flex items-center gap-1.5 font-medium text-muted-foreground">
                    <XCircle className="size-3.5" /> Reddedildi
                  </p>
                )}
                {durum === 'hata' && <p className="text-destructive">İşlem başarısız oldu.</p>}
              </div>
            </div>
          )
        })}
      </div>
    </div>
  )
}
