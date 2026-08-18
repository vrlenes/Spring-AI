import { ClipboardList, FileText, Plus } from 'lucide-react'
import karatayLogo from '@/assets/karatay-logo.png'
import { Button } from '@/components/ui/button'
import { DokumanPanel } from '@/components/chat/DokumanPanel'

interface SidebarProps {
  onYeniKonusma: () => void
}

export function Sidebar({ onYeniKonusma }: SidebarProps) {
  return (
    <aside className="hidden w-64 shrink-0 flex-col border-r bg-muted/30 md:flex">
      <div className="flex items-center gap-2.5 border-b px-4 py-4">
        <img src={karatayLogo} alt="Karatay Belediyesi" className="h-9 w-auto" />
        <div className="leading-tight">
          <p className="text-[13px] font-semibold tracking-tight">Karatay Belediyesi</p>
          <p className="text-[11px] text-muted-foreground">AI Asistanı</p>
        </div>
      </div>

      <div className="p-3">
        <Button onClick={onYeniKonusma} className="w-full justify-start gap-2">
          <Plus className="size-4" />
          Yeni Konuşma
        </Button>
      </div>

      <div className="px-4 py-2">
        <p className="text-[11px] font-medium tracking-wide text-muted-foreground uppercase">
          Neler yapabilirim?
        </p>
        <ul className="mt-2.5 space-y-3 text-[12.5px] text-muted-foreground">
          <li className="flex gap-2">
            <FileText className="mt-0.5 size-3.5 shrink-0 text-primary" />
            <span>Mevzuat ve iç genelge sorularını, yüklenen belgelere dayanarak cevaplarım.</span>
          </li>
          <li className="flex gap-2">
            <ClipboardList className="mt-0.5 size-3.5 shrink-0 text-primary" />
            <span>Vatandaş taleplerini listeler, sınıflandırır, ilgili müdürlüğe yönlendiririm.</span>
          </li>
        </ul>
      </div>

      <div className="my-1 border-t" />

      <DokumanPanel />

      <div className="border-t px-4 py-3 text-[11px] text-muted-foreground">
        Faz 2 — RAG (mevzuat soru-cevap)
      </div>
    </aside>
  )
}
