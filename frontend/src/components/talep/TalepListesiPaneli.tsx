import { useEffect, useState, type FormEvent } from 'react'
import { Loader2, Search } from 'lucide-react'
import { GunlukTrendGrafigi } from '@/components/chat/YapisalVeriKarti'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { DurumRozeti, OncelikMetni } from '@/components/talep/rozetler'
import {
  talepIstatistikGetir,
  talepleriGetir,
  talepleriTopluDurumGuncelle,
  talepleriTopluMudurlugeAta,
} from '@/lib/talepler'
import { TALEP_DURUMLARI } from '@/types/talep'
import type { Mudurluk, TalepDurumu, TalepIstatistik, TalepOzeti, TopluIslemSonucu } from '@/types/talep'

function IstatistikOzeti() {
  const [istatistik, setIstatistik] = useState<TalepIstatistik | null>(null)

  useEffect(() => {
    talepIstatistikGetir(14)
      .then(setIstatistik)
      .catch(() => {})
  }, [])

  if (!istatistik) return null

  return (
    <div className="border-b p-3">
      <p className="text-[11px] text-muted-foreground">
        Son {istatistik.gunSayisi} gün · Toplam <span className="font-semibold text-foreground">{istatistik.toplamTalep}</span>
        {istatistik.ortalamaCozumSuresiSaat != null && (
          <>
            {' '}
            · Ort. çözüm <span className="font-semibold text-foreground">{istatistik.ortalamaCozumSuresiSaat.toFixed(1)} sa</span>
          </>
        )}
      </p>
      <GunlukTrendGrafigi trend={istatistik.gunlukTrend} />
    </div>
  )
}

interface TalepListesiPaneliProps {
  mudurlukler: Mudurluk[]
  onSec: (takipNo: string) => void
}

// TALEP modunda sohbetten bagimsiz, dogrudan arama/filtreleme + coklu secim
// yapabilen yonetim listesi. Yazma islemleri TalepController'in "toplu" uclarina
// gider - orada da (tekli uclarla ayni ilke) PendingAction'a gerek yok, cunku
// aksiyonu baslatan LLM degil, burada listeyi goren kullanicinin kendisi.
export function TalepListesiPaneli({ mudurlukler, onSec }: TalepListesiPaneliProps) {
  const [talepler, setTalepler] = useState<TalepOzeti[]>([])
  const [yukleniyor, setYukleniyor] = useState(true)
  const [hata, setHata] = useState<string | null>(null)
  const [aramaMetni, setAramaMetni] = useState('')
  const [anahtarKelime, setAnahtarKelime] = useState('')
  const [durumFiltre, setDurumFiltre] = useState('')
  const [secililer, setSecililer] = useState<Set<string>>(new Set())
  const [topluDurum, setTopluDurum] = useState<TalepDurumu | ''>('')
  const [topluMudurluk, setTopluMudurluk] = useState('')
  const [topluIslemDevamEdiyor, setTopluIslemDevamEdiyor] = useState(false)
  const [topluSonuc, setTopluSonuc] = useState<TopluIslemSonucu[] | null>(null)

  async function yukle() {
    setYukleniyor(true)
    setHata(null)
    try {
      const sonuc = await talepleriGetir({
        durum: durumFiltre || undefined,
        anahtarKelime: anahtarKelime || undefined,
        limit: 20,
      })
      setTalepler(sonuc)
      setSecililer(new Set())
    } catch (e) {
      setHata(e instanceof Error ? e.message : 'Talepler yüklenemedi.')
    } finally {
      setYukleniyor(false)
    }
  }

  useEffect(() => {
    yukle()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [durumFiltre, anahtarKelime])

  function aramaGonder(e: FormEvent) {
    e.preventDefault()
    setAnahtarKelime(aramaMetni)
  }

  function secimDegistir(takipNo: string) {
    setSecililer((onceki) => {
      const yeni = new Set(onceki)
      if (yeni.has(takipNo)) yeni.delete(takipNo)
      else yeni.add(takipNo)
      return yeni
    })
  }

  function tumunuSec() {
    setSecililer(secililer.size === talepler.length ? new Set() : new Set(talepler.map((t) => t.takipNo)))
  }

  async function topluDurumUygula() {
    if (!topluDurum || secililer.size === 0) return
    setTopluIslemDevamEdiyor(true)
    setTopluSonuc(null)
    try {
      const sonuc = await talepleriTopluDurumGuncelle([...secililer], topluDurum)
      setTopluSonuc(sonuc)
      setTopluDurum('')
      await yukle()
    } catch (e) {
      setHata(e instanceof Error ? e.message : 'Toplu işlem başarısız oldu.')
    } finally {
      setTopluIslemDevamEdiyor(false)
    }
  }

  async function topluMudurlukUygula() {
    if (!topluMudurluk || secililer.size === 0) return
    setTopluIslemDevamEdiyor(true)
    setTopluSonuc(null)
    try {
      const sonuc = await talepleriTopluMudurlugeAta([...secililer], topluMudurluk)
      setTopluSonuc(sonuc)
      setTopluMudurluk('')
      await yukle()
    } catch (e) {
      setHata(e instanceof Error ? e.message : 'Toplu işlem başarısız oldu.')
    } finally {
      setTopluIslemDevamEdiyor(false)
    }
  }

  const hepsiSecili = talepler.length > 0 && secililer.size === talepler.length

  return (
    <div className="flex flex-1 flex-col overflow-hidden">
      <IstatistikOzeti />
      <div className="space-y-2 border-b p-3">
        <form onSubmit={aramaGonder} className="flex gap-1.5">
          <Input
            value={aramaMetni}
            onChange={(e) => setAramaMetni(e.target.value)}
            placeholder="Konu, kategori, takip no ara"
            className="h-8 text-[12.5px]"
          />
          <Button type="submit" size="icon-sm" variant="outline">
            <Search className="size-3.5" />
          </Button>
        </form>
        <select
          value={durumFiltre}
          onChange={(e) => setDurumFiltre(e.target.value)}
          className="h-8 w-full rounded-lg border border-input bg-transparent px-2 text-[12.5px]"
        >
          <option value="">Açık talepler (varsayılan)</option>
          {TALEP_DURUMLARI.map((d) => (
            <option key={d} value={d}>
              {d}
            </option>
          ))}
        </select>
      </div>

      {hata && <p className="px-3 pt-2 text-[12px] text-destructive">{hata}</p>}

      {secililer.size > 0 && (
        <div className="space-y-2 border-b bg-primary/5 p-3">
          <p className="text-[11px] font-medium text-foreground">{secililer.size} talep seçildi</p>
          <div className="flex gap-1.5">
            <select
              value={topluDurum}
              onChange={(e) => setTopluDurum(e.target.value as TalepDurumu)}
              className="h-8 flex-1 rounded-lg border border-input bg-transparent px-2 text-[12px]"
            >
              <option value="">Durum seç</option>
              {TALEP_DURUMLARI.map((d) => (
                <option key={d} value={d}>
                  {d}
                </option>
              ))}
            </select>
            <Button size="sm" disabled={!topluDurum || topluIslemDevamEdiyor} onClick={topluDurumUygula}>
              Uygula
            </Button>
          </div>
          <div className="flex gap-1.5">
            <select
              value={topluMudurluk}
              onChange={(e) => setTopluMudurluk(e.target.value)}
              className="h-8 flex-1 rounded-lg border border-input bg-transparent px-2 text-[12px]"
            >
              <option value="">Müdürlük seç</option>
              {mudurlukler.map((m) => (
                <option key={m.ad} value={m.ad}>
                  {m.ad}
                </option>
              ))}
            </select>
            <Button size="sm" disabled={!topluMudurluk || topluIslemDevamEdiyor} onClick={topluMudurlukUygula}>
              Ata
            </Button>
          </div>
          {topluIslemDevamEdiyor && <p className="text-[11px] text-muted-foreground">İşleniyor...</p>}
          {topluSonuc && (
            <p className="text-[11px] text-muted-foreground">
              {topluSonuc.filter((s) => s.basarili).length}/{topluSonuc.length} başarılı
              {topluSonuc.some((s) => !s.basarili) && (
                <> · Hata: {topluSonuc.filter((s) => !s.basarili).map((s) => s.takipNo).join(', ')}</>
              )}
            </p>
          )}
        </div>
      )}

      <div className="flex-1 overflow-y-auto">
        {yukleniyor && (
          <div className="flex items-center justify-center p-6">
            <Loader2 className="size-4 animate-spin text-muted-foreground" />
          </div>
        )}
        {!yukleniyor && talepler.length === 0 && (
          <p className="p-4 text-[12px] text-muted-foreground">Kriterlere uyan talep bulunamadı.</p>
        )}
        {!yukleniyor && talepler.length > 0 && (
          <table className="w-full text-left text-[11.5px]">
            <thead className="sticky top-0 bg-muted/50 text-[10px] tracking-wide text-muted-foreground uppercase">
              <tr>
                <th className="w-7 px-2 py-1.5">
                  <input type="checkbox" checked={hepsiSecili} onChange={tumunuSec} />
                </th>
                <th className="px-2 py-1.5 font-medium">Talep</th>
                <th className="px-2 py-1.5 font-medium">Durum</th>
              </tr>
            </thead>
            <tbody>
              {talepler.map((t) => (
                <tr key={t.takipNo} className="border-t hover:bg-muted/40">
                  <td className="px-2 py-1.5 align-top">
                    <input type="checkbox" checked={secililer.has(t.takipNo)} onChange={() => secimDegistir(t.takipNo)} />
                  </td>
                  <td className="cursor-pointer px-2 py-1.5 align-top" onClick={() => onSec(t.takipNo)}>
                    <p className="font-mono text-[10.5px] text-muted-foreground">{t.takipNo}</p>
                    <p className="break-words text-foreground">{t.konuMetni}</p>
                    <p className="text-[10.5px] text-muted-foreground">{t.mudurlukAdi ?? '(atanmamış)'}</p>
                  </td>
                  <td className="cursor-pointer px-2 py-1.5 align-top whitespace-nowrap" onClick={() => onSec(t.takipNo)}>
                    <div className="flex flex-col items-start gap-1">
                      <DurumRozeti durum={t.durum} />
                      <OncelikMetni oncelik={t.oncelik} />
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  )
}
