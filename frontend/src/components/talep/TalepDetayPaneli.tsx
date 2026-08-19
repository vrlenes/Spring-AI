import { useEffect, useState } from 'react'
import { Sparkles, X } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { DurumRozeti, OncelikMetni } from '@/components/talep/rozetler'
import {
  aiOnerisiGetir,
  talebeNotEkle,
  talebiMudurlugeAta,
  talepDetayGetir,
  talepDurumGuncelle,
  talepKategoriGuncelle,
  talepOncelikGuncelle,
} from '@/lib/talepler'
import { TALEP_DURUMLARI, TALEP_ONCELIKLERI } from '@/types/talep'
import type { Mudurluk, SiniflandirmaOnerisi, TalepDetay, TalepDurumu, TalepOnceligi } from '@/types/talep'

const TARIH_BICIMI = new Intl.DateTimeFormat('tr-TR', { dateStyle: 'short', timeStyle: 'short' })

function tarihGoster(iso: string | null) {
  if (!iso) return '-'
  return TARIH_BICIMI.format(new Date(iso))
}

interface TalepDetayPaneliProps {
  takipNo: string
  mudurlukler: Mudurluk[]
  onKapat: () => void
  onDegisti: () => void
}

export function TalepDetayPaneli({ takipNo, mudurlukler, onKapat, onDegisti }: TalepDetayPaneliProps) {
  const [detay, setDetay] = useState<TalepDetay | null>(null)
  const [yukleniyor, setYukleniyor] = useState(true)
  const [hata, setHata] = useState<string | null>(null)
  const [islemDevamEdiyor, setIslemDevamEdiyor] = useState(false)

  const [secilenMudurluk, setSecilenMudurluk] = useState('')
  const [secilenDurum, setSecilenDurum] = useState<TalepDurumu | ''>('')
  const [secilenOncelik, setSecilenOncelik] = useState<TalepOnceligi | ''>('')
  const [notMetni, setNotMetni] = useState('')
  const [personel, setPersonel] = useState('')

  const [oneri, setOneri] = useState<SiniflandirmaOnerisi | null>(null)
  const [oneriYukleniyor, setOneriYukleniyor] = useState(false)
  const [oneriHatasi, setOneriHatasi] = useState<string | null>(null)

  useEffect(() => {
    let iptalEdildi = false
    setYukleniyor(true)
    setHata(null)
    setOneri(null)
    setOneriHatasi(null)
    talepDetayGetir(takipNo)
      .then((d) => {
        if (iptalEdildi) return
        setDetay(d)
        if (d) {
          setSecilenMudurluk(d.mudurlukAdi ?? '')
          setSecilenDurum(d.durum)
          setSecilenOncelik(d.oncelik)
        }
      })
      .catch((e) => !iptalEdildi && setHata(e instanceof Error ? e.message : 'Bilinmeyen hata'))
      .finally(() => !iptalEdildi && setYukleniyor(false))
    return () => {
      iptalEdildi = true
    }
  }, [takipNo])

  async function isleAl(islem: () => Promise<TalepDetay>) {
    setIslemDevamEdiyor(true)
    setHata(null)
    try {
      const guncel = await islem()
      setDetay(guncel)
      onDegisti()
    } catch (e) {
      setHata(e instanceof Error ? e.message : 'İşlem başarısız oldu.')
    } finally {
      setIslemDevamEdiyor(false)
    }
  }

  async function aiOnerisiIste() {
    setOneriYukleniyor(true)
    setOneriHatasi(null)
    try {
      setOneri(await aiOnerisiGetir(takipNo))
    } catch (e) {
      setOneriHatasi(e instanceof Error ? e.message : 'AI önerisi alınamadı.')
    } finally {
      setOneriYukleniyor(false)
    }
  }

  async function oneriyiUygula() {
    if (!oneri) return
    await isleAl(async () => {
      let guncel = detay as TalepDetay
      if (oneri.kategori) {
        guncel = await talepKategoriGuncelle(takipNo, oneri.kategori)
      }
      if (oneri.mudurlukAdi) {
        guncel = await talebiMudurlugeAta(takipNo, oneri.mudurlukAdi)
      }
      return guncel
    })
    setOneri(null)
  }

  return (
    <aside className="flex w-96 shrink-0 flex-col overflow-y-auto border-l bg-background">
      <div className="flex items-center justify-between border-b px-4 py-3">
        <p className="text-[13px] font-semibold">{takipNo}</p>
        <Button variant="ghost" size="icon-sm" onClick={onKapat}>
          <X className="size-4" />
        </Button>
      </div>

      {yukleniyor && <p className="p-4 text-[12.5px] text-muted-foreground">Yükleniyor...</p>}
      {!yukleniyor && !detay && <p className="p-4 text-[12.5px] text-muted-foreground">Talep bulunamadı.</p>}

      {detay && (
        <div className="flex flex-col gap-4 p-4 text-[12.5px]">
          <div className="flex items-center gap-2">
            <DurumRozeti durum={detay.durum} />
            <OncelikMetni oncelik={detay.oncelik} />
          </div>

          <div className="space-y-1.5">
            <p className="leading-relaxed text-foreground">{detay.konuMetni}</p>
            <p className="text-muted-foreground">
              {detay.vatandasAd ?? '-'}
              {detay.iletisim ? ` · ${detay.iletisim}` : ''}
            </p>
            <p className="text-muted-foreground">
              {detay.mahalle ?? '-'} · {detay.kategori ?? 'sınıflandırılmamış'}
            </p>
            <p className="text-muted-foreground">
              Oluşturma: {tarihGoster(detay.olusturmaTarihi)}
              {detay.guncellemeTarihi ? ` · Güncelleme: ${tarihGoster(detay.guncellemeTarihi)}` : ''}
            </p>
            <p className="text-muted-foreground">Müdürlük: {detay.mudurlukAdi ?? '(atanmamış)'}</p>
          </div>

          {hata && <p className="rounded-lg bg-destructive/10 px-2.5 py-1.5 text-destructive">{hata}</p>}

          <div className="border-t pt-3">
            {!oneri && (
              <Button size="sm" variant="outline" onClick={aiOnerisiIste} disabled={oneriYukleniyor}>
                <Sparkles className="size-3.5" /> {oneriYukleniyor ? 'AI düşünüyor...' : 'AI ile Öner'}
              </Button>
            )}

            {oneriHatasi && <p className="text-destructive">{oneriHatasi}</p>}

            {oneri && (
              <div className="space-y-2 rounded-lg border border-primary/30 bg-primary/5 p-2.5">
                <p className="flex items-center gap-1.5 font-medium text-foreground">
                  <Sparkles className="size-3.5 text-primary" /> AI Önerisi
                </p>
                <p className="text-muted-foreground">
                  Müdürlük: <span className="text-foreground">{oneri.mudurlukAdi ?? 'belirlenemedi'}</span>
                </p>
                {oneri.kategori && (
                  <p className="text-muted-foreground">
                    Kategori: <span className="text-foreground">{oneri.kategori}</span>
                  </p>
                )}
                {oneri.gerekce && <p className="text-muted-foreground italic">"{oneri.gerekce}"</p>}
                <div className="flex gap-1.5 pt-1">
                  <Button size="sm" disabled={!oneri.mudurlukAdi || islemDevamEdiyor} onClick={oneriyiUygula}>
                    Uygula
                  </Button>
                  <Button size="sm" variant="outline" onClick={() => setOneri(null)}>
                    Reddet
                  </Button>
                </div>
              </div>
            )}
          </div>

          <div className="space-y-2 border-t pt-3">
            <p className="text-[11px] font-medium tracking-wide text-muted-foreground uppercase">Müdürlüğe Ata</p>
            <div className="flex gap-1.5">
              <select
                value={secilenMudurluk}
                onChange={(e) => setSecilenMudurluk(e.target.value)}
                className="h-8 flex-1 rounded-lg border border-input bg-transparent px-2 text-[12.5px]"
              >
                <option value="">Müdürlük seçin</option>
                {mudurlukler.map((m) => (
                  <option key={m.ad} value={m.ad}>
                    {m.ad}
                  </option>
                ))}
              </select>
              <Button
                size="sm"
                disabled={!secilenMudurluk || islemDevamEdiyor}
                onClick={() => isleAl(() => talebiMudurlugeAta(takipNo, secilenMudurluk))}
              >
                Ata
              </Button>
            </div>
          </div>

          <div className="space-y-2 border-t pt-3">
            <p className="text-[11px] font-medium tracking-wide text-muted-foreground uppercase">Durum Güncelle</p>
            <div className="flex gap-1.5">
              <select
                value={secilenDurum}
                onChange={(e) => setSecilenDurum(e.target.value as TalepDurumu)}
                className="h-8 flex-1 rounded-lg border border-input bg-transparent px-2 text-[12.5px]"
              >
                {TALEP_DURUMLARI.map((d) => (
                  <option key={d} value={d}>
                    {d}
                  </option>
                ))}
              </select>
              <Button
                size="sm"
                disabled={islemDevamEdiyor || secilenDurum === detay.durum}
                onClick={() => isleAl(() => talepDurumGuncelle(takipNo, secilenDurum as TalepDurumu))}
              >
                Güncelle
              </Button>
            </div>
          </div>

          <div className="space-y-2 border-t pt-3">
            <p className="text-[11px] font-medium tracking-wide text-muted-foreground uppercase">Öncelik Güncelle</p>
            <div className="flex gap-1.5">
              <select
                value={secilenOncelik}
                onChange={(e) => setSecilenOncelik(e.target.value as TalepOnceligi)}
                className="h-8 flex-1 rounded-lg border border-input bg-transparent px-2 text-[12.5px]"
              >
                {TALEP_ONCELIKLERI.map((o) => (
                  <option key={o} value={o}>
                    {o}
                  </option>
                ))}
              </select>
              <Button
                size="sm"
                disabled={islemDevamEdiyor || secilenOncelik === detay.oncelik}
                onClick={() => isleAl(() => talepOncelikGuncelle(takipNo, secilenOncelik as TalepOnceligi))}
              >
                Güncelle
              </Button>
            </div>
          </div>

          <div className="space-y-2 border-t pt-3">
            <p className="text-[11px] font-medium tracking-wide text-muted-foreground uppercase">Not Ekle</p>
            <Input placeholder="Personel adı" value={personel} onChange={(e) => setPersonel(e.target.value)} />
            <textarea
              value={notMetni}
              onChange={(e) => setNotMetni(e.target.value)}
              placeholder="Not metni"
              rows={2}
              className="w-full rounded-lg border border-input bg-transparent px-2.5 py-1.5 text-[12.5px] outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50"
            />
            <Button
              size="sm"
              disabled={!notMetni.trim() || islemDevamEdiyor}
              onClick={() =>
                isleAl(async () => {
                  const sonuc = await talebeNotEkle(takipNo, notMetni, personel)
                  setNotMetni('')
                  return sonuc
                })
              }
            >
              Not Ekle
            </Button>
          </div>

          {detay.notlar.length > 0 && (
            <div className="space-y-2 border-t pt-3">
              <p className="text-[11px] font-medium tracking-wide text-muted-foreground uppercase">Geçmiş</p>
              <div className="flex flex-col gap-2">
                {detay.notlar.map((n, i) => (
                  <div key={i} className="rounded-lg bg-muted/40 px-2.5 py-1.5">
                    <p className="text-[11px] text-muted-foreground">
                      {tarihGoster(n.tarih)} · {n.personel}
                    </p>
                    <p className="text-foreground">{n.notu}</p>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      )}
    </aside>
  )
}
