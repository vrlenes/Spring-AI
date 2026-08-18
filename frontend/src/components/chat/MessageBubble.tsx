import { cn } from '@/lib/utils'
import type { ChatMessage } from '@/types/chat'

export function MessageBubble({ message }: { message: ChatMessage }) {
  const kullaniciMi = message.role === 'kullanici'

  return (
    <div
      className={cn(
        'max-w-[75%] rounded-lg px-3 py-2 text-sm whitespace-pre-wrap',
        kullaniciMi
          ? 'ml-auto bg-primary text-primary-foreground'
          : 'mr-auto border bg-card text-card-foreground',
      )}
    >
      {message.content}
    </div>
  )
}
