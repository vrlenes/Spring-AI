import { useEffect, useRef, useState, type ChangeEvent, type FormEvent } from 'react'
import { FileText, Loader2, Trash2 } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { dokumanSil, dokumanYukle, dokumanlariListele } from '@/lib/dokumanlar'
import type { Dokuman } from '@/types/dokuman'

export function DokumanPanel() {
  const [dokumanlar, setDokumanlar] = useState<Dokuman[]>([])
  const [secilenDosya, setSecilenDosya] = useState<File | null>(null)
  const [baslik, setBaslik] = useState('')
  const [kategori, setKategori] = useState('MEVZUAT')
  const [yukleniyor, setYukleniyor] = useState(false)
  const dosyaInputRef = useRef<HTMLInputElement>(null)

  useEffect(() => {
    yenile()
  }, [])

  async function yenile() {
    try {
      setDokumanlar(await dokumanlariListele())
    } catch {
      // sidebar'daki liste kritik degil, sessizce yut
    }
  }

  function dosyaSecildi(e: ChangeEvent<HTMLInputElement>) {
    const dosya = e.target.files?.[0]
    if (!dosya) return
    setSecilenDosya(dosya)
    setBaslik(dosya.name.replace(/\.[^./]+$/, ''))
  }

  function formuKapat() {
    setSecilenDosya(null)
    setBaslik('')
    if (dosyaInputRef.current) dosyaInputRef.current.value = ''
  }

  async function yukle(e: FormEvent) {
    e.preventDefault()
    if (!secilenDosya || !baslik.trim()) return

    setYukleniyor(true)
    try {
      await dokumanYukle(secilenDosya, baslik.trim(), kategori)
      formuKapat()
      await yenile()
    } catch {
      alert('Doküman yüklenirken bir hata oluştu.')
    } finally {
      setYukleniyor(false)
    }
  }

  async function sil(dokuman: Dokuman) {
    if (!window.confirm(`"${dokuman.baslik}" dokümanını silmek istediğinize emin misiniz?`)) return
    try {
      await dokumanSil(dokuman.id)
      await yenile()
    } catch {
      alert('Doküman silinirken bir hata oluştu.')
    }
  }

  return (
    <div className="flex min-h-0 flex-1 flex-col px-4 py-3">
      <div className="flex items-center justify-between">
        <p className="text-[11px] font-medium tracking-wide text-muted-foreground uppercase">Dokümanlar</p>
        <label className="cursor-pointer text-[11px] font-medium text-primary hover:underline">
          + Yükle
          <input
            ref={dosyaInputRef}
            type="file"
            accept=".pdf,.doc,.docx,.txt"
            className="hidden"
            onChange={dosyaSecildi}
          />
        </label>
      </div>

      {secilenDosya && (
        <form onSubmit={yukle} className="mt-2 flex flex-col gap-1.5 rounded-lg border bg-background p-2">
          <input
            value={baslik}
            onChange={(e) => setBaslik(e.target.value)}
            placeholder="Doküman başlığı"
            autoFocus
            className="rounded border bg-transparent px-2 py-1 text-[12px] outline-none focus:border-primary/40"
          />
          <select
            value={kategori}
            onChange={(e) => setKategori(e.target.value)}
            className="rounded border bg-transparent px-2 py-1 text-[12px] outline-none"
          >
            <option value="MEVZUAT">Mevzuat</option>
            <option value="GENELGE">Genelge</option>
            <option value="IK">İK</option>
            <option value="PROSEDUR">Prosedür</option>
          </select>
          <div className="flex gap-1.5">
            <Button type="submit" size="xs" disabled={yukleniyor || !baslik.trim()} className="flex-1">
              {yukleniyor ? <Loader2 className="size-3 animate-spin" /> : 'Yükle'}
            </Button>
            <Button type="button" variant="ghost" size="xs" onClick={formuKapat} disabled={yukleniyor}>
              Vazgeç
            </Button>
          </div>
        </form>
      )}

      <div className="mt-2 min-h-0 flex-1 overflow-y-auto">
        {dokumanlar.length === 0 && !secilenDosya && (
          <p className="text-[11.5px] text-muted-foreground">Henüz doküman yüklenmedi.</p>
        )}
        <div className="flex flex-col gap-0.5">
          {dokumanlar.map((dokuman) => (
            <div
              key={dokuman.id}
              className="group flex items-center gap-1.5 rounded px-1 py-1.5 text-[12px] hover:bg-muted/60"
            >
              <FileText className="size-3.5 shrink-0 text-muted-foreground" />
              <span className="min-w-0 flex-1 truncate" title={dokuman.baslik}>
                {dokuman.baslik}
              </span>
              <button
                type="button"
                onClick={() => sil(dokuman)}
                className="shrink-0 text-muted-foreground opacity-0 transition-opacity hover:text-destructive group-hover:opacity-100"
                aria-label={`${dokuman.baslik} dokümanını sil`}
              >
                <Trash2 className="size-3.5" />
              </button>
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}
