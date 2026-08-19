import { useEffect, useState } from 'react'
import { Search, Sparkles } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { DurumRozeti, OncelikMetni } from '@/components/talep/rozetler'
import { TalepDetayPaneli } from '@/components/talep/TalepDetayPaneli'
import { TopluOneriPaneli } from '@/components/talep/TopluOneriPaneli'
import { mudurlukleriGetir, talepleriGetir, topluAiOnerisiGetir } from '@/lib/talepler'
import { TALEP_DURUMLARI } from '@/types/talep'
import type { Mudurluk, TalepOzeti, TopluSiniflandirmaOnerisi } from '@/types/talep'

const TARIH_BICIMI = new Intl.DateTimeFormat('tr-TR', { dateStyle: 'short' })

export function TalepPanel() {
  const [talepler, setTalepler] = useState<TalepOzeti[]>([])
  const [mudurlukler, setMudurlukler] = useState<Mudurluk[]>([])
  const [yukleniyor, setYukleniyor] = useState(true)
  const [hata, setHata] = useState<string | null>(null)
  const [secilenTakipNo, setSecilenTakipNo] = useState<string | null>(null)

  const [durumFiltre, setDurumFiltre] = useState('')
  const [mudurlukFiltre, setMudurlukFiltre] = useState('')
  const [kelimeFiltre, setKelimeFiltre] = useState('')
  // Varsayilan olarak sadece atanmamis talepler gosterilir - bunlar personelin
  // ilk bakmasi gereken kayitlar. Diger talepleri gormek icin bu isaret kaldirilir.
  const [sadeceAtanmamis, setSadeceAtanmamis] = useState(true)

  const [topluOneriler, setTopluOneriler] = useState<TopluSiniflandirmaOnerisi[] | null>(null)
  const [topluYukleniyor, setTopluYukleniyor] = useState(false)
  const [topluHata, setTopluHata] = useState<string | null>(null)

  async function topluOneriIste() {
    setTopluYukleniyor(true)
    setTopluHata(null)
    try {
      setTopluOneriler(await topluAiOnerisiGetir())
    } catch (e) {
      setTopluHata(e instanceof Error ? e.message : 'Toplu AI önerisi alınamadı.')
    } finally {
      setTopluYukleniyor(false)
    }
  }

  async function listeyiYenile(atanmamisOverride?: boolean) {
    setYukleniyor(true)
    setHata(null)
    try {
      const sonuc = await talepleriGetir({
        durum: durumFiltre || undefined,
        mudurluk: mudurlukFiltre || undefined,
        anahtarKelime: kelimeFiltre || undefined,
        atanmamis: atanmamisOverride ?? sadeceAtanmamis,
      })
      setTalepler(sonuc)
    } catch (e) {
      setHata(e instanceof Error ? e.message : 'Talepler yüklenemedi.')
    } finally {
      setYukleniyor(false)
    }
  }

  function atanmamisToggle(deger: boolean) {
    setSadeceAtanmamis(deger)
    listeyiYenile(deger)
  }

  useEffect(() => {
    mudurlukleriGetir()
      .then(setMudurlukler)
      .catch(() => {})
    listeyiYenile()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  return (
    <div className="flex min-h-0 flex-1">
      <div className="flex min-w-0 flex-1 flex-col">
        <div className="flex shrink-0 flex-wrap items-center gap-2 border-b px-4 py-3">
          <label className="flex items-center gap-1.5 text-[12.5px] text-foreground select-none">
            <input
              type="checkbox"
              checked={sadeceAtanmamis}
              onChange={(e) => atanmamisToggle(e.target.checked)}
              className="size-3.5 accent-primary"
            />
            Sadece atanmamış
          </label>

          <div className="mx-1 h-5 w-px bg-border" />

          <select
            value={durumFiltre}
            onChange={(e) => setDurumFiltre(e.target.value)}
            className="h-8 rounded-lg border border-input bg-transparent px-2 text-[12.5px]"
          >
            <option value="">Tüm açık talepler</option>
            {TALEP_DURUMLARI.map((d) => (
              <option key={d} value={d}>
                {d}
              </option>
            ))}
          </select>

          <select
            value={mudurlukFiltre}
            onChange={(e) => setMudurlukFiltre(e.target.value)}
            className="h-8 rounded-lg border border-input bg-transparent px-2 text-[12.5px]"
          >
            <option value="">Tüm müdürlükler</option>
            {mudurlukler.map((m) => (
              <option key={m.ad} value={m.ad}>
                {m.ad}
              </option>
            ))}
          </select>

          <Input
            placeholder="Anahtar kelime..."
            value={kelimeFiltre}
            onChange={(e) => setKelimeFiltre(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && listeyiYenile()}
            className="w-48"
          />

          <Button size="sm" onClick={() => listeyiYenile()} disabled={yukleniyor}>
            <Search className="size-3.5" /> Ara
          </Button>

          <Button size="sm" variant="outline" onClick={topluOneriIste} disabled={topluYukleniyor}>
            <Sparkles className="size-3.5" /> {topluYukleniyor ? 'AI düşünüyor...' : 'AI ile Sınıflandır'}
          </Button>

          <span className="ml-auto text-[11.5px] text-muted-foreground">{talepler.length} kayıt</span>
        </div>

        {topluHata && <p className="p-4 text-[12.5px] text-destructive">{topluHata}</p>}

        {topluOneriler ? (
          <TopluOneriPaneli
            oneriler={topluOneriler}
            onKapat={() => setTopluOneriler(null)}
            onDegisti={() => listeyiYenile()}
          />
        ) : (
        <div className="min-h-0 flex-1 overflow-y-auto">
          {hata && <p className="p-4 text-[12.5px] text-destructive">{hata}</p>}
          {!hata && yukleniyor && <p className="p-4 text-[12.5px] text-muted-foreground">Yükleniyor...</p>}
          {!hata && !yukleniyor && talepler.length === 0 && (
            <p className="p-4 text-[12.5px] text-muted-foreground">Kriterlere uyan talep bulunamadı.</p>
          )}

          {!hata && talepler.length > 0 && (
            <table className="w-full text-left text-[12.5px]">
              <thead className="sticky top-0 bg-background text-[11px] tracking-wide text-muted-foreground uppercase">
                <tr className="border-b">
                  <th className="px-4 py-2 font-medium">Takip No</th>
                  <th className="px-4 py-2 font-medium">Konu</th>
                  <th className="px-4 py-2 font-medium">Müdürlük</th>
                  <th className="px-4 py-2 font-medium">Durum</th>
                  <th className="px-4 py-2 font-medium">Öncelik</th>
                  <th className="px-4 py-2 font-medium">Tarih</th>
                </tr>
              </thead>
              <tbody>
                {talepler.map((t) => (
                  <tr
                    key={t.takipNo}
                    onClick={() => setSecilenTakipNo(t.takipNo)}
                    className="cursor-pointer border-b transition-colors last:border-0 hover:bg-muted/40 data-[selected=true]:bg-muted/60"
                    data-selected={t.takipNo === secilenTakipNo}
                  >
                    <td className="px-4 py-2 font-mono text-[11.5px] whitespace-nowrap">{t.takipNo}</td>
                    <td className="max-w-xs truncate px-4 py-2">{t.konuMetni}</td>
                    <td className="px-4 py-2 whitespace-nowrap text-muted-foreground">
                      {t.mudurlukAdi ?? '(atanmamış)'}
                    </td>
                    <td className="px-4 py-2">
                      <DurumRozeti durum={t.durum} />
                    </td>
                    <td className="px-4 py-2">
                      <OncelikMetni oncelik={t.oncelik} />
                    </td>
                    <td className="px-4 py-2 whitespace-nowrap text-muted-foreground">
                      {TARIH_BICIMI.format(new Date(t.olusturmaTarihi))}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
        )}
      </div>

      {secilenTakipNo && (
        <TalepDetayPaneli
          takipNo={secilenTakipNo}
          mudurlukler={mudurlukler}
          onKapat={() => setSecilenTakipNo(null)}
          onDegisti={listeyiYenile}
        />
      )}
    </div>
  )
}
