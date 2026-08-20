import { useEffect, useMemo, useRef, useState, type ChangeEvent, type FormEvent, type KeyboardEvent } from 'react'
import { ArrowUp, FileText, Paperclip, X } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { talepleriGetir } from '@/lib/talepler'
import { useSohbetStore } from '@/stores/useSohbetStore'
import { cn } from '@/lib/utils'
import type { TalepOzeti } from '@/types/talep'

const IZIN_VERILEN_EK_TURLERI = 'image/jpeg,image/png,image/webp,application/pdf'
// Sunucudan bundan daha fazla oneri cekilir (TAKIP_NO_ONERI_TOPLAM), ama
// ayni anda sadece bir "pencere" (TAKIP_NO_ONERI_GORUNUR kadar) gosterilir -
// asagi tusuyla gezinirken pencere kaydirilir (ustteki kaybolur, alttan yeni
// gelir), 6'da donup basa sarmak yerine.
const TAKIP_NO_ONERI_GORUNUR = 6
const TAKIP_NO_ONERI_TOPLAM = 24

interface ChatComposerProps {
  disabled: boolean
  onSend: (mesaj: string, dosya?: File) => void
}

interface MentionDurumu {
  sorgu: string
  baslangic: number
  bitis: number
}

// Metinde imlecin hemen oncesinde "@" ile baslayan, bosluksuz bir kelime var
// mi diye bakar - varsa takip no onerisi tetiklenir (orn. "...@TLP-20" ->
// sorgu="TLP-20"). "@" bir bosluktan hemen sonra veya metnin basinda olmali,
// aksi halde e-posta gibi metinlerde de tetiklenirdi.
function mentionAra(metin: string, imlecKonumu: number): MentionDurumu | null {
  const oncesi = metin.slice(0, imlecKonumu)
  const eslesme = oncesi.match(/(?:^|\s)@([^\s@]*)$/)
  if (!eslesme) return null
  const baslangic = eslesme.index! + eslesme[0].indexOf('@')
  return { sorgu: eslesme[1], baslangic, bitis: imlecKonumu }
}

export function ChatComposer({ disabled, onSend }: ChatComposerProps) {
  const aktifMod = useSohbetStore((s) => s.aktifMod)
  const [deger, setDeger] = useState('')
  const [dosya, setDosya] = useState<File | null>(null)
  const [mention, setMention] = useState<MentionDurumu | null>(null)
  const [oneriler, setOneriler] = useState<TalepOzeti[]>([])
  const [seciliIndeks, setSeciliIndeks] = useState(0)
  const dosyaInputRef = useRef<HTMLInputElement>(null)
  const inputRef = useRef<HTMLInputElement>(null)
  const canSend = (deger.trim().length > 0 || dosya !== null) && !disabled
  const oneriAcik = mention !== null && aktifMod === 'TALEP'

  // "@" sorgusu degistikce, kisa bir bekleme sonrasi (debounce) takip
  // numarasi/konu metniyle eslesen acik talepleri getirir - her tus vurusunda
  // istek atmamak icin. anahtarKelime artik takipNo'yu da kapsiyor (bkz.
  // TalepService.anahtarKelimeSpec), boylece hem "@yol" hem "@TLP-2026" ile
  // arama yapilabiliyor.
  useEffect(() => {
    if (!oneriAcik) {
      setOneriler([])
      return
    }
    let iptalEdildi = false
    const zamanlayici = setTimeout(async () => {
      try {
        const sonuc = await talepleriGetir({ anahtarKelime: mention!.sorgu || undefined, limit: TAKIP_NO_ONERI_TOPLAM })
        if (!iptalEdildi) {
          setOneriler(sonuc)
          setSeciliIndeks(0)
        }
      } catch {
        if (!iptalEdildi) setOneriler([])
      }
    }, 200)
    return () => {
      iptalEdildi = true
      clearTimeout(zamanlayici)
    }
  }, [oneriAcik, mention])

  // Secili oge her zaman gorunur pencerede kalacak sekilde pencereyi kaydirir:
  // asagi inip pencerenin alt sinirini gecince pencere de asagi kayar (ustteki
  // oge gorunumden cikar), yukari cikip ust sinira gelince tersi olur.
  const pencereBaslangic = useMemo(() => {
    let baslangic = 0
    if (seciliIndeks >= baslangic + TAKIP_NO_ONERI_GORUNUR) {
      baslangic = seciliIndeks - TAKIP_NO_ONERI_GORUNUR + 1
    }
    if (seciliIndeks < baslangic) {
      baslangic = seciliIndeks
    }
    return baslangic
  }, [seciliIndeks])
  const gorunenOneriler = oneriler.slice(pencereBaslangic, pencereBaslangic + TAKIP_NO_ONERI_GORUNUR)

  function dosyaSecildi(e: ChangeEvent<HTMLInputElement>) {
    const secilen = e.target.files?.[0]
    setDosya(secilen ?? null)
    e.target.value = ''
  }

  function metinDegisti(e: ChangeEvent<HTMLInputElement>) {
    setDeger(e.target.value)
    setMention(mentionAra(e.target.value, e.target.selectionStart ?? e.target.value.length))
  }

  function oneriSec(talep: TalepOzeti) {
    if (!mention) return
    const yeniMetin = `${deger.slice(0, mention.baslangic)}@${talep.takipNo} ${deger.slice(mention.bitis)}`
    setDeger(yeniMetin)
    setMention(null)
    requestAnimationFrame(() => {
      const yeniImlec = mention.baslangic + talep.takipNo.length + 2
      inputRef.current?.focus()
      inputRef.current?.setSelectionRange(yeniImlec, yeniImlec)
    })
  }

  function gonder(e: FormEvent) {
    e.preventDefault()
    const mesaj = deger.trim()
    if (!mesaj && !dosya) return
    setDeger('')
    setMention(null)
    const gonderilecekDosya = dosya ?? undefined
    setDosya(null)
    onSend(mesaj, gonderilecekDosya)
  }

  function tusaBasildi(e: KeyboardEvent<HTMLInputElement>) {
    if (oneriAcik && oneriler.length > 0) {
      if (e.key === 'ArrowDown') {
        e.preventDefault()
        setSeciliIndeks((i) => (i + 1) % oneriler.length)
        return
      }
      if (e.key === 'ArrowUp') {
        e.preventDefault()
        setSeciliIndeks((i) => (i - 1 + oneriler.length) % oneriler.length)
        return
      }
      if (e.key === 'Enter' || e.key === 'Tab') {
        e.preventDefault()
        oneriSec(oneriler[seciliIndeks])
        return
      }
      if (e.key === 'Escape') {
        e.preventDefault()
        setMention(null)
        return
      }
    }
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      gonder(e)
    }
  }

  return (
    <form onSubmit={gonder} className="relative shrink-0 border-t p-3">
      {oneriAcik && gorunenOneriler.length > 0 && (
        <div className="absolute right-3 bottom-full left-3 mb-1.5 rounded-lg border bg-popover p-1 shadow-lg">
          {gorunenOneriler.map((t, sliceI) => {
            const gercekIndeks = pencereBaslangic + sliceI
            return (
              <button
                key={t.takipNo}
                type="button"
                onMouseDown={(e) => e.preventDefault()}
                onClick={() => oneriSec(t)}
                onMouseEnter={() => setSeciliIndeks(gercekIndeks)}
                className={cn(
                  'flex w-full items-center gap-2 rounded-md px-2.5 py-1.5 text-left text-[12.5px]',
                  gercekIndeks === seciliIndeks ? 'bg-muted' : 'hover:bg-muted/60',
                )}
              >
                <span className="shrink-0 font-mono text-[11px] text-primary">{t.takipNo}</span>
                <span className="min-w-0 flex-1 truncate text-muted-foreground">{t.konuMetni}</span>
              </button>
            )
          })}
        </div>
      )}
      {dosya && (
        <div className="mb-1.5 flex w-fit items-center gap-1.5 rounded-full border bg-muted/40 py-1 pr-1 pl-2.5 text-[12px]">
          <FileText className="size-3.5 shrink-0 text-muted-foreground" />
          <span className="max-w-40 truncate">{dosya.name}</span>
          <button
            type="button"
            onClick={() => setDosya(null)}
            aria-label="Eki kaldır"
            className="rounded-full p-0.5 text-muted-foreground hover:bg-muted hover:text-foreground"
          >
            <X className="size-3" />
          </button>
        </div>
      )}
      <div
        className={cn(
          'flex items-center gap-1.5 rounded-full border bg-muted/40 py-1.5 pr-1.5 pl-1.5 transition-colors',
          'focus-within:border-foreground/15 focus-within:bg-background',
        )}
      >
        <label
          className={cn(
            'flex size-8 shrink-0 cursor-pointer items-center justify-center rounded-full text-muted-foreground hover:bg-muted hover:text-foreground',
            disabled && 'pointer-events-none opacity-60',
          )}
          aria-label="Dosya ekle"
        >
          <Paperclip className="size-4" />
          <input
            ref={dosyaInputRef}
            type="file"
            accept={IZIN_VERILEN_EK_TURLERI}
            disabled={disabled}
            onChange={dosyaSecildi}
            className="hidden"
          />
        </label>
        <input
          ref={inputRef}
          value={deger}
          onChange={metinDegisti}
          onKeyDown={tusaBasildi}
          placeholder={aktifMod === 'TALEP' ? 'Bir soru yazın... ("@" ile takip no arayın)' : 'Bir soru yazın...'}
          autoComplete="off"
          disabled={disabled}
          className="min-w-0 flex-1 bg-transparent text-[13px] leading-relaxed text-foreground outline-none placeholder:text-muted-foreground disabled:opacity-60"
        />
        <Button
          type="submit"
          size="icon"
          disabled={!canSend}
          aria-label="Gönder"
          className="size-8 shrink-0 rounded-full"
        >
          <ArrowUp className="size-4" />
        </Button>
      </div>
    </form>
  )
}
