import { DurumRozeti, OncelikMetni } from '@/components/talep/rozetler'
import type { YapisalVeriPaketi } from '@/types/chat'
import type { Mudurluk, TalepDetay, TalepIstatistik, TalepOzeti } from '@/types/talep'

const DURUM_ETIKETLERI: Record<string, string> = {
  YENI: 'Yeni',
  ATANDI: 'Atandı',
  ISLEMDE: 'İşlemde',
  COZULDU: 'Çözüldü',
  REDDEDILDI: 'Reddedildi',
}

// Modelin sohbette yazdigi metne degil, tool'un dondurdugu ham veriye
// dayanir (bkz. TalepTools.kaydetYapisalVeri, ChatService) - ayni
// kaynaklar/araclar ilkesi. "tip" alanina gore hangi kart render edilecegi
// burada secilir; yeni bir tip icin buraya yeni bir dal eklemek yeterli.
function TalepListesiTablosu({ talepler }: { talepler: TalepOzeti[] }) {
  if (talepler.length === 0) return null

  return (
    <div className="mt-2.5 overflow-x-auto rounded-lg border">
      <table className="w-full text-left text-[12px]">
        <thead className="bg-muted/50 text-[10.5px] tracking-wide text-muted-foreground uppercase">
          <tr>
            <th className="px-2.5 py-1.5 font-medium">Takip No</th>
            <th className="px-2.5 py-1.5 font-medium">Konu</th>
            <th className="px-2.5 py-1.5 font-medium">Müdürlük</th>
            <th className="px-2.5 py-1.5 font-medium">Durum</th>
            <th className="px-2.5 py-1.5 font-medium">Öncelik</th>
          </tr>
        </thead>
        <tbody>
          {talepler.map((t) => (
            <tr key={t.takipNo} className="border-t">
              <td className="px-2.5 py-1.5 align-top font-mono text-[11px] whitespace-nowrap">{t.takipNo}</td>
              <td className="max-w-[260px] px-2.5 py-1.5 align-top break-words">{t.konuMetni}</td>
              <td className="px-2.5 py-1.5 align-top text-muted-foreground">{t.mudurlukAdi ?? '(atanmamış)'}</td>
              <td className="px-2.5 py-1.5 align-top whitespace-nowrap">
                <DurumRozeti durum={t.durum} />
              </td>
              <td className="px-2.5 py-1.5 align-top whitespace-nowrap">
                <OncelikMetni oncelik={t.oncelik} />
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

function TalepDetayKarti({ detay }: { detay: TalepDetay }) {
  return (
    <div className="mt-2.5 max-w-md rounded-lg border p-3 text-[12px]">
      <div className="flex items-center justify-between gap-2">
        <span className="font-mono text-[11px] font-medium text-foreground">{detay.takipNo}</span>
        <div className="flex shrink-0 items-center gap-1.5">
          <DurumRozeti durum={detay.durum} />
          <OncelikMetni oncelik={detay.oncelik} />
        </div>
      </div>
      <p className="mt-1.5 break-words text-foreground">{detay.konuMetni}</p>
      <div className="mt-2 grid grid-cols-2 gap-x-3 gap-y-1 text-muted-foreground">
        <span>Müdürlük: {detay.mudurlukAdi ?? '(atanmamış)'}</span>
        <span>Mahalle: {detay.mahalle ?? '-'}</span>
        <span>Kategori: {detay.kategori ?? '(sınıflandırılmamış)'}</span>
        <span>Vatandaş: {detay.vatandasAd ?? '-'}</span>
      </div>
      {detay.notlar.length > 0 && (
        <div className="mt-2 border-t pt-2">
          <p className="mb-1 text-[10.5px] font-medium tracking-wide text-muted-foreground uppercase">Notlar</p>
          <div className="flex flex-col gap-1">
            {detay.notlar.map((n, i) => (
              <p key={i} className="break-words text-muted-foreground">
                <span className="text-foreground">{n.personel}:</span> {n.notu}
              </p>
            ))}
          </div>
        </div>
      )}
    </div>
  )
}

export function GunlukTrendGrafigi({ trend }: { trend: TalepIstatistik['gunlukTrend'] }) {
  if (trend.length === 0) return null
  const maks = Math.max(1, ...trend.map((g) => g.sayi))
  const TARIH_KISA = new Intl.DateTimeFormat('tr-TR', { day: '2-digit', month: '2-digit' })

  return (
    <div className="mt-2.5 border-t pt-2.5">
      <p className="mb-1.5 text-[10.5px] font-medium tracking-wide text-muted-foreground uppercase">
        Günlük Talep Trendi
      </p>
      <div className="flex h-14 items-end gap-[3px]">
        {trend.map((g) => (
          <div key={g.tarih} className="flex flex-1 flex-col items-center gap-1" title={`${g.tarih}: ${g.sayi} talep`}>
            <div
              className="w-full rounded-t bg-primary/60"
              style={{ height: `${Math.max(2, (g.sayi / maks) * 100)}%` }}
            />
          </div>
        ))}
      </div>
      <div className="mt-1 flex justify-between text-[10px] text-muted-foreground">
        <span>{TARIH_KISA.format(new Date(trend[0].tarih))}</span>
        <span>{TARIH_KISA.format(new Date(trend[trend.length - 1].tarih))}</span>
      </div>
    </div>
  )
}

function IstatistikKarti({ istatistik }: { istatistik: TalepIstatistik }) {
  return (
    <div className="mt-2.5 max-w-md rounded-lg border p-3 text-[12px]">
      <p className="mb-2 text-[11px] text-muted-foreground">
        Son {istatistik.gunSayisi} gün{istatistik.mudurlukAdi ? ` · ${istatistik.mudurlukAdi}` : ''} · Toplam{' '}
        <span className="font-semibold text-foreground">{istatistik.toplamTalep}</span>
        {istatistik.ortalamaCozumSuresiSaat != null && (
          <>
            {' '}
            · Ort. çözüm süresi{' '}
            <span className="font-semibold text-foreground">{istatistik.ortalamaCozumSuresiSaat.toFixed(1)} saat</span>
          </>
        )}
      </p>
      <div className="grid grid-cols-2 gap-1.5">
        {Object.entries(istatistik.durumDagilimi).map(([durum, sayi]) => (
          <div key={durum} className="flex items-center justify-between rounded-lg bg-muted/40 px-2.5 py-1.5">
            <span className="text-muted-foreground">{DURUM_ETIKETLERI[durum] ?? durum}</span>
            <span className="font-semibold text-foreground">{sayi}</span>
          </div>
        ))}
      </div>
      <GunlukTrendGrafigi trend={istatistik.gunlukTrend} />
    </div>
  )
}

function MudurlukListesiKarti({ mudurlukler }: { mudurlukler: Mudurluk[] }) {
  if (mudurlukler.length === 0) return null

  return (
    <div className="mt-2.5 flex max-w-md flex-col gap-1.5">
      {mudurlukler.map((m) => (
        <div key={m.ad} className="rounded-lg border bg-muted/30 px-2.5 py-1.5 text-[12px]">
          <p className="font-medium text-foreground">{m.ad}</p>
          {m.sorumlulukAlani && <p className="mt-0.5 break-words text-[11px] text-muted-foreground">{m.sorumlulukAlani}</p>}
        </div>
      ))}
    </div>
  )
}

export function YapisalVeriKarti({ paket }: { paket: YapisalVeriPaketi }) {
  switch (paket.tip) {
    case 'TALEP_LISTESI':
      return <TalepListesiTablosu talepler={paket.veri as TalepOzeti[]} />
    case 'TALEP_DETAY':
      return <TalepDetayKarti detay={paket.veri as TalepDetay} />
    case 'TALEP_ISTATISTIK':
      return <IstatistikKarti istatistik={paket.veri as TalepIstatistik} />
    case 'MUDURLUK_LISTESI':
      return <MudurlukListesiKarti mudurlukler={paket.veri as Mudurluk[]} />
    default:
      return null
  }
}
