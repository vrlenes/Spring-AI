export function TypingDots() {
  return (
    <div className="mr-auto flex items-center gap-1 px-0.5 py-2 animate-in fade-in-0 duration-300">
      <span className="size-1.5 animate-bounce rounded-full bg-muted-foreground [animation-delay:-0.3s]" />
      <span className="size-1.5 animate-bounce rounded-full bg-muted-foreground [animation-delay:-0.15s]" />
      <span className="size-1.5 animate-bounce rounded-full bg-muted-foreground" />
    </div>
  )
}
