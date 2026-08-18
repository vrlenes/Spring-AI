import karatayMark from '@/assets/karatay-mark.png'

export function TypingDots() {
  return (
    <div className="mr-auto flex items-center gap-2 animate-in fade-in-0 duration-300">
      <span className="flex size-6.5 shrink-0 items-center justify-center rounded-full bg-primary p-1">
        <img src={karatayMark} alt="" className="size-full object-contain brightness-0 invert" />
      </span>
      <div className="flex items-center gap-1 py-2">
        <span className="size-1.5 animate-bounce rounded-full bg-muted-foreground [animation-delay:-0.3s]" />
        <span className="size-1.5 animate-bounce rounded-full bg-muted-foreground [animation-delay:-0.15s]" />
        <span className="size-1.5 animate-bounce rounded-full bg-muted-foreground" />
      </div>
    </div>
  )
}
