import { CircleAlert, CircleCheck, FileText, Globe, TriangleAlert, ThumbsDown, ThumbsUp, User, Wand2, Wrench } from 'lucide-react'
import ReactMarkdown from 'react-markdown'
import karatayMark from '@/assets/karatay-mark.png'
import { BekleyenIslemKarti } from '@/components/chat/BekleyenIslemKarti'
import { EkOnizleme } from '@/components/chat/EkOnizleme'
import { YapisalVeriKarti } from '@/components/chat/YapisalVeriKarti'
import { useSohbetStore } from '@/stores/useSohbetStore'
import { cn } from '@/lib/utils'
import type { ChatMessage, Kaynak, KaynakDogrulamaSonucu, SohbetModu } from '@/types/chat'

const ALGILANAN_MOD_ETIKETLERI: Record<SohbetModu, string> = {
  GENEL: 'Genel',
  TALEP: 'Talep',
  IMAR: 'İmar',
  RUHSAT: 'Ruhsat',
  OTOMATIK: 'Otomatik',
}

// OTOMATIK modda ModYonlendirmeService'in bu mesaji hangi gercek moda
// yonlendirdigini gosterir - kullanici yanlis yonlendirmeyi fark edip tek
// tikla elle moda gecebilsin diye seffaflik amacli (bkz. ChatService).
function AlgilananModRozeti({ mod }: { mod: SohbetModu }) {
  return (
    <p className="mb-1.5 flex items-center gap-1 text-[11px] text-muted-foreground">
      <Wand2 className="size-3" /> Otomatik: {ALGILANAN_MOD_ETIKETLERI[mod]} modu algılandı
    </p>
  )
}

// Akis basladiktan (HTTP 200 sonrasi) bir hata olursa gosterilir (bkz.
// backend ChatService.akisliYanitla'daki "hata" SSE olayi) - onceden bu
// durum sessizce yarim kalmis bir cevap gibi gorunuyordu, kullanici bir
// seyin yanlis gittigini hic anlamiyordu.
function HataBanner({ mesaj }: { mesaj: string }) {
  return (
    <p className="mt-2 flex items-center gap-1.5 rounded-lg bg-destructive/10 px-2.5 py-1.5 text-[12px] text-destructive">
      <TriangleAlert className="size-3.5 shrink-0" /> {mesaj}
    </p>
  )
}

function AracRozetleri({ araclar }: { araclar: string[] }) {
  return (
    <div className="mt-2 flex flex-wrap gap-1.5">
      {araclar.map((arac) => (
        <span
          key={arac}
          className="inline-flex items-center gap-1 rounded-full bg-muted/60 px-2 py-0.5 text-[11px] text-muted-foreground"
        >
          <Wrench className="size-3" /> {arac}
        </span>
      ))}
    </div>
  )
}

// Model belgeye/talep sistemine dayanmadan (kaynaklar VE araclar bos) cevap
// verdiginde gosterilir. Bu isaret modelin kendi ifadesine degil, KODDAN
// (bu iki listenin bos olup olmadigina) dayanir - modele guvenmek yerine
// (bkz. CLAUDE.md "kaynak gosterimi koddan uretilir" ilkesi).
function GenelBilgiRozeti() {
  return (
    <p className="mt-2 flex items-center gap-1 text-[11px] text-muted-foreground italic">
      <Globe className="size-3" /> Genel bilgi — yüklenmiş belgelerde veya talep sisteminde bulunamadı, doğrulayın
    </p>
  )
}

interface KaynakGrubu {
  baslik: string
  etiketler: { metin: string; benzerlik: number | null }[]
}

// Ayni belgeden gelen birden fazla parca/madde (top-k=5 oldugu icin sik
// rastlanan bir durum) eskiden her biri ayri, tam genislikte bir satirdi -
// ayni basligin 5 kez tekrarlanmasi dikey olarak cok yer kapliyordu.
// Simdi belge basina TEK satir, madde/parca numaralari o satirin icinde
// yan yana kucuk etiketler olarak goruluyor.
function kaynaklariGrupla(kaynaklar: Kaynak[]): KaynakGrubu[] {
  const gruplar: KaynakGrubu[] = []
  for (const kaynak of kaynaklar) {
    const etiket = kaynak.maddeNo != null ? `Madde ${kaynak.maddeNo}` : `Parça ${kaynak.parcaNo}`
    const grup = gruplar.find((g) => g.baslik === kaynak.baslik)
    if (grup) {
      if (!grup.etiketler.some((e) => e.metin === etiket)) {
        grup.etiketler.push({ metin: etiket, benzerlik: kaynak.benzerlik })
      }
    } else {
      gruplar.push({ baslik: kaynak.baslik, etiketler: [{ metin: etiket, benzerlik: kaynak.benzerlik }] })
    }
  }
  return gruplar
}

function KaynakKartlari({ kaynaklar }: { kaynaklar: Kaynak[] }) {
  return (
    <div className="mt-2.5 flex flex-col gap-1">
      <p className="flex items-center gap-1 text-[11px] font-medium text-muted-foreground">
        <FileText className="size-3" /> Kaynaklar
      </p>
      {kaynaklariGrupla(kaynaklar).map((grup) => (
        <div key={grup.baslik} className="rounded-lg border bg-muted/40 px-2.5 py-1.5 text-[12px]">
          <p className="truncate text-foreground">{grup.baslik}</p>
          <div className="mt-1 flex flex-wrap gap-1">
            {grup.etiketler.map((etiket) => (
              <span
                key={etiket.metin}
                title={etiket.benzerlik != null ? `%${Math.round(etiket.benzerlik * 100)} benzerlik` : undefined}
                className="shrink-0 rounded-full bg-primary/10 px-1.5 py-0.5 text-[10.5px] text-primary"
              >
                {etiket.metin}
              </span>
            ))}
          </div>
        </div>
      ))}
    </div>
  )
}

// KaynakDogrulamaService'in bagimsiz ikinci-model kontrolunun sonucu -
// modelin kendi ifadesine degil, ayri bir "gerceklik kontrolu" cagrisina
// dayanir (bkz. backend). dogrulandi=false oldugunda "not" alani hangi
// kismin desteksiz oldugunu aciklar.
function DogrulamaRozeti({ dogrulama }: { dogrulama: KaynakDogrulamaSonucu }) {
  if (dogrulama.dogrulandi) {
    return (
      <p className="mt-2 flex items-center gap-1 text-[11px] text-emerald-600 dark:text-emerald-400">
        <CircleCheck className="size-3" /> Kaynakla doğrulandı
      </p>
    )
  }
  return (
    <p
      className="mt-2 flex items-center gap-1 text-[11px] text-destructive"
      title={dogrulama.not ?? undefined}
    >
      <CircleAlert className="size-3" /> Kaynakla tam örtüşmüyor{dogrulama.not ? ` — ${dogrulama.not}` : ''}
    </p>
  )
}

// Sadece kalici bir mesajId varsa gosterilir (bkz. ChatMessage.mesajId) -
// akis daha yeni bitmisse gecmis henuz yeniden cekilmemis olabilir, bu kisa
// aninda buton gecici olarak gorunmez. Tekrar tiklamak mevcut degeri geri
// alir (toggle) - "olumlu" iken tekrar basmak notru degere doner.
function GeriBildirimButonlari({ mesajId, deger }: { mesajId: number; deger?: 'OLUMLU' | 'OLUMSUZ' }) {
  const geriBildirimVer = useSohbetStore((s) => s.geriBildirimVer)

  return (
    <div className="mt-2 flex items-center gap-1">
      <button
        type="button"
        aria-label="Yararlı"
        onClick={() => geriBildirimVer(mesajId, deger === 'OLUMLU' ? null : 'OLUMLU')}
        className={cn(
          'flex size-6 items-center justify-center rounded-md transition-colors',
          deger === 'OLUMLU'
            ? 'bg-primary/10 text-primary'
            : 'text-muted-foreground hover:bg-muted hover:text-foreground',
        )}
      >
        <ThumbsUp className="size-3.5" />
      </button>
      <button
        type="button"
        aria-label="Yararlı değil"
        onClick={() => geriBildirimVer(mesajId, deger === 'OLUMSUZ' ? null : 'OLUMSUZ')}
        className={cn(
          'flex size-6 items-center justify-center rounded-md transition-colors',
          deger === 'OLUMSUZ'
            ? 'bg-destructive/10 text-destructive'
            : 'text-muted-foreground hover:bg-muted hover:text-foreground',
        )}
      >
        <ThumbsDown className="size-3.5" />
      </button>
    </div>
  )
}

interface MessageBubbleProps {
  message: ChatMessage
  streaming?: boolean
}

function RoleAvatar({ kullaniciMi }: { kullaniciMi: boolean }) {
  if (kullaniciMi) {
    return (
      <span className="flex size-6.5 shrink-0 items-center justify-center rounded-full bg-muted text-foreground">
        <User className="size-3.5" />
      </span>
    )
  }
  return (
    <span className="flex size-6.5 shrink-0 items-center justify-center rounded-full bg-primary p-1">
      <img src={karatayMark} alt="" className="size-full object-contain brightness-0 invert" />
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
          {message.ek && <EkOnizleme ek={message.ek} />}
          {message.content}
        </div>
      ) : (
        <div className="min-w-0 pt-1 text-[13px] leading-relaxed text-foreground">
          {message.algilananMod && <AlgilananModRozeti mod={message.algilananMod} />}
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
          {message.hata && <HataBanner mesaj={message.hata} />}
          {message.araclar && message.araclar.length > 0 && <AracRozetleri araclar={message.araclar} />}
          {message.yapisalVeri && <YapisalVeriKarti paket={message.yapisalVeri} />}
          {message.kaynaklar && message.kaynaklar.length > 0 && (
            <KaynakKartlari kaynaklar={message.kaynaklar} />
          )}
          {!streaming && message.dogrulama && <DogrulamaRozeti dogrulama={message.dogrulama} />}
          {message.bekleyenIslem && <BekleyenIslemKarti bekleyenIslem={message.bekleyenIslem} />}
          {!streaming &&
            message.content &&
            !(message.araclar && message.araclar.length > 0) &&
            !(message.kaynaklar && message.kaynaklar.length > 0) && <GenelBilgiRozeti />}
          {!streaming && message.mesajId != null && (
            <GeriBildirimButonlari mesajId={message.mesajId} deger={message.geriBildirim} />
          )}
        </div>
      )}
    </div>
  )
}
