import { useState, type FormEvent } from 'react'
import { Input } from '@/components/ui/input'
import { Button } from '@/components/ui/button'

interface ChatComposerProps {
  disabled: boolean
  onSend: (mesaj: string) => void
}

export function ChatComposer({ disabled, onSend }: ChatComposerProps) {
  const [deger, setDeger] = useState('')

  function gonder(e: FormEvent) {
    e.preventDefault()
    const mesaj = deger.trim()
    if (!mesaj) return
    setDeger('')
    onSend(mesaj)
  }

  return (
    <form onSubmit={gonder} className="flex gap-2 border-t bg-background p-3">
      <Input
        value={deger}
        onChange={(e) => setDeger(e.target.value)}
        placeholder="Bir soru yazın..."
        autoComplete="off"
        disabled={disabled}
      />
      <Button type="submit" disabled={disabled || !deger.trim()}>
        Gönder
      </Button>
    </form>
  )
}
