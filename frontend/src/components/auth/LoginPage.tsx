import { useState, type FormEvent } from 'react'
import { Loader2 } from 'lucide-react'
import karatayLogo from '@/assets/karatay-logo.png'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { useAuthStore } from '@/stores/useAuthStore'

export function LoginPage() {
  const { girisYap, girisYapiliyor, girisHatasi } = useAuthStore()
  const [kullaniciAdi, setKullaniciAdi] = useState('')
  const [sifre, setSifre] = useState('')

  async function gonder(e: FormEvent) {
    e.preventDefault()
    if (!kullaniciAdi.trim() || !sifre) return
    try {
      await girisYap(kullaniciAdi.trim(), sifre)
    } catch {
      // hata zaten store'da girisHatasi olarak tutuluyor
    }
  }

  return (
    <div className="flex h-screen w-full items-center justify-center bg-muted/30">
      <form onSubmit={gonder} className="w-full max-w-sm rounded-2xl border bg-background p-8 shadow-sm">
        <div className="mb-6 flex flex-col items-center gap-3 text-center">
          <img src={karatayLogo} alt="Karatay Belediyesi" className="h-12 w-auto" />
          <div>
            <p className="text-[15px] font-semibold tracking-tight">Karatay Belediyesi</p>
            <p className="text-[12.5px] text-muted-foreground">AI Asistanı — Personel Girişi</p>
          </div>
        </div>

        <div className="flex flex-col gap-3">
          <div className="flex flex-col gap-1.5">
            <label className="text-[12px] font-medium text-muted-foreground">Kullanıcı adı</label>
            <Input
              value={kullaniciAdi}
              onChange={(e) => setKullaniciAdi(e.target.value)}
              autoFocus
              autoComplete="username"
            />
          </div>
          <div className="flex flex-col gap-1.5">
            <label className="text-[12px] font-medium text-muted-foreground">Şifre</label>
            <Input
              type="password"
              value={sifre}
              onChange={(e) => setSifre(e.target.value)}
              autoComplete="current-password"
            />
          </div>

          {girisHatasi && <p className="text-[12.5px] text-destructive">{girisHatasi}</p>}

          <Button type="submit" disabled={girisYapiliyor || !kullaniciAdi.trim() || !sifre} className="mt-2 w-full">
            {girisYapiliyor ? <Loader2 className="size-4 animate-spin" /> : 'Giriş Yap'}
          </Button>
        </div>
      </form>
    </div>
  )
}
