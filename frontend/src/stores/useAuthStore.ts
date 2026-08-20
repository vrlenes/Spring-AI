import { create } from 'zustand'
import { cikisYap as cikisYapApi, girisYap as girisYapApi, mevcutPersoneliGetir } from '@/lib/auth'
import type { Personel } from '@/types/auth'

interface AuthStore {
  personel: Personel | null
  kontrolEdiliyor: boolean
  girisHatasi: string | null
  girisYapiliyor: boolean
  oturumKontrolEt: () => Promise<void>
  girisYap: (kullaniciAdi: string, sifre: string) => Promise<void>
  cikisYap: () => Promise<void>
}

export const useAuthStore = create<AuthStore>((set) => ({
  personel: null,
  kontrolEdiliyor: true,
  girisHatasi: null,
  girisYapiliyor: false,

  async oturumKontrolEt() {
    try {
      const personel = await mevcutPersoneliGetir()
      set({ personel, kontrolEdiliyor: false })
    } catch {
      set({ personel: null, kontrolEdiliyor: false })
    }
  },

  async girisYap(kullaniciAdi, sifre) {
    set({ girisYapiliyor: true, girisHatasi: null })
    try {
      const personel = await girisYapApi(kullaniciAdi, sifre)
      set({ personel, girisYapiliyor: false })
    } catch (e) {
      set({ girisHatasi: e instanceof Error ? e.message : 'Giriş başarısız oldu.', girisYapiliyor: false })
      throw e
    }
  },

  async cikisYap() {
    await cikisYapApi()
    set({ personel: null })
  },
}))
