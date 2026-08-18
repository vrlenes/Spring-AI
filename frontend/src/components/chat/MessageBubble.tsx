import { Building2, User } from 'lucide-react'
import ReactMarkdown from 'react-markdown'
import { cn } from '@/lib/utils'
import type { ChatMessage } from '@/types/chat'

interface MessageBubbleProps {
  message: ChatMessage
  streaming?: boolean
}

function RoleAvatar({ kullaniciMi }: { kullaniciMi: boolean }) {
  return (
    <span
      className={cn(
        'flex size-6.5 shrink-0 items-center justify-center rounded-full',
        kullaniciMi ? 'bg-muted text-foreground' : 'bg-primary text-primary-foreground',
      )}
    >
      {kullaniciMi ? <User className="size-3.5" /> : <Building2 className="size-3.5" />}
    </span>
  )
}

export function MessageBubble({ message, streaming }: MessageBubbleProps) {
  const kullaniciMi = message.role === 'kullanici'

  return (
    <div className={cn('flex max-w-[88%] animate-in fade-in-0 slide-in-from-bottom-1 items-start gap-2 duration-300', kullaniciMi ? 'ml-auto flex-row-reverse' : 'mr-auto')}>
      <RoleAvatar kullaniciMi={kullaniciMi} />

      {kullaniciMi ? (
        <div className="rounded-2xl rounded-tr-sm bg-muted px-3.5 py-2 text-[13px] leading-relaxed whitespace-pre-wrap text-foreground">
          {message.content}
        </div>
      ) : (
        <div className="min-w-0 pt-1 text-[13px] leading-relaxed text-foreground">
          <ReactMarkdown
            components={{
              p: ({ children }) => <p className="mb-2.5 last:mb-0">{children}</p>,
              ul: ({ children }) => <ul className="mb-2.5 list-disc space-y-1 pl-4 last:mb-0">{children}</ul>,
              ol: ({ children }) => <ol className="mb-2.5 list-decimal space-y-1 pl-4 last:mb-0">{children}</ol>,
              li: ({ children }) => <li>{children}</li>,
              strong: ({ children }) => <strong className="font-semibold">{children}</strong>,
              code: ({ children }) => (
                <code className="rounded bg-muted px-1 py-0.5 font-mono text-[12px]">{children}</code>
              ),
              a: ({ children, href }) => (
                <a href={href} target="_blank" rel="noreferrer" className="text-primary underline underline-offset-2">
                  {children}
                </a>
              ),
            }}
          >
            {message.content}
          </ReactMarkdown>
          {streaming && (
            <span className="ml-0.5 inline-block h-3.5 w-0.5 translate-y-0.5 animate-pulse rounded-full bg-foreground/70" />
          )}
        </div>
      )}
    </div>
  )
}
