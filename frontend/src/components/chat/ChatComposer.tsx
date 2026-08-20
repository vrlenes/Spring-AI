import { useRef, useState, type ChangeEvent, type FormEvent, type KeyboardEvent } from 'react'
import { ArrowUp, FileText, Paperclip, X } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { cn } from '@/lib/utils'

const IZIN_VERILEN_EK_TURLERI = 'image/jpeg,image/png,image/webp,application/pdf'

interface ChatComposerProps {
  disabled: boolean
  onSend: (mesaj: string, dosya?: File) => void
}

export function ChatComposer({ disabled, onSend }: ChatComposerProps) {
  const [deger, setDeger] = useState('')
  const [dosya, setDosya] = useState<File | null>(null)
  const dosyaInputRef = useRef<HTMLInputElement>(null)
  const canSend = (deger.trim().length > 0 || dosya !== null) && !disabled

  function dosyaSecildi(e: ChangeEvent<HTMLInputElement>) {
    const secilen = e.target.files?.[0]
    setDosya(secilen ?? null)
    e.target.value = ''
  }

  function gonder(e: FormEvent) {
    e.preventDefault()
    const mesaj = deger.trim()
    if (!mesaj && !dosya) return
    setDeger('')
    const gonderilecekDosya = dosya ?? undefined
    setDosya(null)
    onSend(mesaj, gonderilecekDosya)
  }

  function tusaBasildi(e: KeyboardEvent<HTMLInputElement>) {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      gonder(e)
    }
  }

  return (
    <form onSubmit={gonder} className="shrink-0 border-t p-3">
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
          value={deger}
          onChange={(e) => setDeger(e.target.value)}
          onKeyDown={tusaBasildi}
          placeholder="Bir soru yazın..."
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
