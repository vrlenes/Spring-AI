import { useEffect, useState } from 'react'
import { AracPaneli } from '@/components/chat/AracPaneli'
import { DokumanPanel } from '@/components/chat/DokumanPanel'
import { TalepDetayPaneli } from '@/components/talep/TalepDetayPaneli'
import { TalepListesiPaneli } from '@/components/talep/TalepListesiPaneli'
import { mudurlukleriGetir } from '@/lib/talepler'
import { useSohbetStore } from '@/stores/useSohbetStore'
import type { Mudurluk } from '@/types/talep'

// Sag panel, aktif moda gore icerik degistirir. TALEP modunda varsayilan
// gorunum artik bos bir placeholder degil, dogrudan REST uzerinden calisan
// aranabilir/coklu-secimli talep listesi (bkz. TalepListesiPaneli - "toplu
// talep islemleri"). Sohbette yeni bir bekleyen islem (PendingAction)
// olustugunda o talebin detayi otomatik one gecer; kullanici detay panelini
// kapatirsa listeye geri donulur.
export function SagPanel() {
  const { aktifMod, mesajlar } = useSohbetStore()
  const [mudurlukler, setMudurlukler] = useState<Mudurluk[]>([])
  const [secilenTakipNo, setSecilenTakipNo] = useState<string | null>(null)

  useEffect(() => {
    if (aktifMod === 'TALEP') {
      mudurlukleriGetir()
        .then(setMudurlukler)
        .catch(() => {})
    }
  }, [aktifMod])

  const sonBekleyenIslemTakipNo = [...mesajlar].reverse().find((m) => m.bekleyenIslem)?.bekleyenIslem?.takipNo

  useEffect(() => {
    if (sonBekleyenIslemTakipNo) {
      setSecilenTakipNo(sonBekleyenIslemTakipNo)
    }
  }, [sonBekleyenIslemTakipNo])

  if (aktifMod !== 'TALEP') {
    return (
      <aside className="hidden w-72 shrink-0 flex-col border-l bg-muted/30 lg:flex">
        <AracPaneli />
        <DokumanPanel mod={aktifMod} />
      </aside>
    )
  }

  return (
    <aside className="hidden w-96 shrink-0 flex-col border-l bg-background lg:flex">
      {secilenTakipNo ? (
        <TalepDetayPaneli
          takipNo={secilenTakipNo}
          mudurlukler={mudurlukler}
          onKapat={() => setSecilenTakipNo(null)}
          onDegisti={() => {}}
        />
      ) : (
        <TalepListesiPaneli mudurlukler={mudurlukler} onSec={setSecilenTakipNo} />
      )}
    </aside>
  )
}
