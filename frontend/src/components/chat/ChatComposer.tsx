import { useState, type FormEvent, type KeyboardEvent } from 'react'
import { ArrowUp } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { cn } from '@/lib/utils'

interface ChatComposerProps {
  disabled: boolean
  onSend: (mesaj: string) => void
}

export function ChatComposer({ disabled, onSend }: ChatComposerProps) {
  const [deger, setDeger] = useState('')
  const canSend = deger.trim().length > 0 && !disabled

  function gonder(e: FormEvent) {
    e.preventDefault()
    const mesaj = deger.trim()
    if (!mesaj) return
    setDeger('')
    onSend(mesaj)
  }

  function tusaBasildi(e: KeyboardEvent<HTMLInputElement>) {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      gonder(e)
    }
  }

  return (
    <form onSubmit={gonder} className="shrink-0 border-t p-3">
      <div
        className={cn(
          'flex items-center gap-1.5 rounded-full border bg-muted/40 py-1.5 pr-1.5 pl-4 transition-colors',
          'focus-within:border-foreground/15 focus-within:bg-background',
        )}
      >
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
