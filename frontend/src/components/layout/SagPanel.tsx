import { useEffect, useState } from 'react'
import { ClipboardList } from 'lucide-react'
import { DokumanPanel } from '@/components/chat/DokumanPanel'
import { TalepDetayPaneli } from '@/components/talep/TalepDetayPaneli'
import { mudurlukleriGetir } from '@/lib/talepler'
import { useSohbetStore } from '@/stores/useSohbetStore'
import type { Mudurluk } from '@/types/talep'

// Sag panel, aktif moda gore icerik degistirir - bu, "Talep Yonetimi" ayri
// tam sayfa sekmesinin bu iskelet turunda kalkip chat + sag panel hibrit
// modeline tasinmasinin karsiligi (bkz. plan). TALEP modunda gosterilecek
// takipNo, sohbetteki EN SON bekleyen islemden (PendingAction) turetiliyor -
// aci bir talep listesi/arama widget'i bu turda YOK, sonraki bir detay
// turunda eklenebilir.
export function SagPanel() {
  const { aktifMod, mesajlar } = useSohbetStore()
  const [mudurlukler, setMudurlukler] = useState<Mudurluk[]>([])

  useEffect(() => {
    if (aktifMod === 'TALEP') {
      mudurlukleriGetir()
        .then(setMudurlukler)
        .catch(() => {})
    }
  }, [aktifMod])

  if (aktifMod !== 'TALEP') {
    return (
      <aside className="hidden w-72 shrink-0 flex-col border-l bg-muted/30 lg:flex">
        <DokumanPanel mod={aktifMod} />
      </aside>
    )
  }

  const sonBekleyenIslem = [...mesajlar].reverse().find((m) => m.bekleyenIslem)?.bekleyenIslem

  if (!sonBekleyenIslem) {
    return (
      <aside className="hidden w-72 shrink-0 flex-col items-center justify-center gap-2 border-l bg-muted/30 px-6 text-center lg:flex">
        <ClipboardList className="size-6 text-muted-foreground" />
        <p className="text-[12px] text-muted-foreground">
          Bir talep işlemi başlatıldığında (atama, durum güncelleme vb.) detayları burada görünecek.
        </p>
      </aside>
    )
  }

  return (
    <TalepDetayPaneli
      takipNo={sonBekleyenIslem.takipNo}
      mudurlukler={mudurlukler}
      onKapat={() => {}}
      onDegisti={() => {}}
    />
  )
}
