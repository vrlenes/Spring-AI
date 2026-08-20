import { create } from 'zustand'
import { streamChat } from '@/lib/chatStream'
import { sohbetleriGetir, sohbetMesajlariniGetir } from '@/lib/sohbetler'
import type { ChatMessage, SohbetMesajOzeti, SohbetModu, SohbetOzeti } from '@/types/chat'

function mesajOzetindenChatMesaji(m: SohbetMesajOzeti): ChatMessage {
  return {
    id: crypto.randomUUID(),
    role: m.rol === 'KULLANICI' ? 'kullanici' : 'asistan',
    content: m.icerik,
    kaynaklar: m.kaynaklar ?? undefined,
    araclar: m.araclar ?? undefined,
    bekleyenIslem: m.bekleyenIslem ?? undefined,
  }
}

interface SohbetStore {
  sohbetListesi: SohbetOzeti[]
  aktifSohbetId: string | null
  aktifMod: SohbetModu
  mesajlar: ChatMessage[]
  gonderiliyor: boolean
  streamingId: string | null
  gecmisYukleniyor: boolean

  sohbetListesiniYukle: () => Promise<void>
  yeniKonusma: (mod?: SohbetModu) => void
  modDegistir: (mod: SohbetModu) => void
  sohbetSec: (id: string) => Promise<void>
  mesajGonder: (mesaj: string) => Promise<void>
}

export const useSohbetStore = create<SohbetStore>((set, get) => ({
  sohbetListesi: [],
  aktifSohbetId: null,
  aktifMod: 'GENEL',
  mesajlar: [],
  gonderiliyor: false,
  streamingId: null,
  gecmisYukleniyor: false,

  async sohbetListesiniYukle() {
    try {
      set({ sohbetListesi: await sohbetleriGetir() })
    } catch {
      // sol paneldeki gecmis listesi kritik degil, sessizce yut
    }
  },

  yeniKonusma(mod) {
    set((durum) => ({ aktifSohbetId: null, mesajlar: [], aktifMod: mod ?? durum.aktifMod }))
  },

  modDegistir(mod) {
    // Mod degistirmek, aktif konusmayi degistirmez - sadece bir SONRAKI yeni
    // konusma bu modda baslar. Var olan bir konusmanin ortasinda mod
    // degistirilemez (SohbetModu, konusma acilirken sabitleniyor - bkz. backend).
    if (get().aktifSohbetId === null) {
      set({ aktifMod: mod, mesajlar: [] })
    } else {
      set({ aktifMod: mod, aktifSohbetId: null, mesajlar: [] })
    }
  },

  async sohbetSec(id) {
    set({ gecmisYukleniyor: true })
    try {
      const mesajlar = await sohbetMesajlariniGetir(id)
      const bulunanSohbet = get().sohbetListesi.find((s) => s.id === id)
      set({
        aktifSohbetId: id,
        aktifMod: bulunanSohbet?.mod ?? 'GENEL',
        mesajlar: mesajlar.map(mesajOzetindenChatMesaji),
      })
    } finally {
      set({ gecmisYukleniyor: false })
    }
  },

  async mesajGonder(mesaj) {
    const kullaniciMesaji: ChatMessage = { id: crypto.randomUUID(), role: 'kullanici', content: mesaj }
    const asistanId = crypto.randomUUID()
    const { aktifSohbetId, aktifMod } = get()

    set((durum) => ({
      mesajlar: [...durum.mesajlar, kullaniciMesaji, { id: asistanId, role: 'asistan', content: '' }],
      gonderiliyor: true,
      streamingId: asistanId,
    }))

    try {
      await streamChat({ conversationId: aktifSohbetId, mesaj, mod: aktifMod }, (olay) => {
        if (olay.type === 'conversationId') {
          set({ aktifSohbetId: olay.conversationId })
          return
        }
        if (olay.type === 'kaynaklar') {
          set((durum) => ({
            mesajlar: durum.mesajlar.map((m) => (m.id === asistanId ? { ...m, kaynaklar: olay.kaynaklar } : m)),
          }))
          return
        }
        if (olay.type === 'bekleyenIslem') {
          set((durum) => ({
            mesajlar: durum.mesajlar.map((m) =>
              m.id === asistanId ? { ...m, bekleyenIslem: olay.bekleyenIslem } : m,
            ),
          }))
          return
        }
        if (olay.type === 'araclar') {
          set((durum) => ({
            mesajlar: durum.mesajlar.map((m) => (m.id === asistanId ? { ...m, araclar: olay.araclar } : m)),
          }))
          return
        }
        set((durum) => ({
          mesajlar: durum.mesajlar.map((m) => (m.id === asistanId ? { ...m, content: m.content + olay.text } : m)),
        }))
      })
    } catch {
      set((durum) => ({
        mesajlar: durum.mesajlar.map((m) =>
          m.id === asistanId ? { ...m, content: 'Bir hata oluştu, lütfen tekrar deneyin.' } : m,
        ),
      }))
    } finally {
      set({ gonderiliyor: false, streamingId: null })
      get().sohbetListesiniYukle()
    }
  },
}))
