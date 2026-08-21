import { create } from 'zustand'
import { streamChat } from '@/lib/chatStream'
import { geriBildirimVer as geriBildirimVerApi, sohbetleriGetir, sohbetMesajlariniGetir } from '@/lib/sohbetler'
import type { AracGrubu, ChatMessage, GeriBildirim, SohbetMesajOzeti, SohbetModu, SohbetOzeti } from '@/types/chat'

function mesajOzetindenChatMesaji(m: SohbetMesajOzeti, sohbetId: string): ChatMessage {
  return {
    id: crypto.randomUUID(),
    role: m.rol === 'KULLANICI' ? 'kullanici' : 'asistan',
    content: m.icerik,
    kaynaklar: m.kaynaklar ?? undefined,
    araclar: m.araclar ?? undefined,
    bekleyenIslem: m.bekleyenIslem ?? undefined,
    yapisalVeri: m.yapisalVeri ?? undefined,
    mesajId: m.id,
    geriBildirim: m.geriBildirim ?? undefined,
    ek: m.ekMimeTipi
      ? {
          url: `/api/sohbetler/${sohbetId}/mesajlar/${m.id}/ek`,
          mimeTipi: m.ekMimeTipi,
          dosyaAdi: m.ekDosyaAdi ?? 'ek',
        }
      : undefined,
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
  // "Araçlar" panelinden kapatilan tool gruplari - bkz. AracPaneli.tsx.
  // Konusma bazli degil, oturum bazli tutulur (yeni konusmada sifirlanmaz).
  kapaliAraclar: Set<AracGrubu>

  sohbetListesiniYukle: () => Promise<void>
  yeniKonusma: (mod?: SohbetModu) => void
  modDegistir: (mod: SohbetModu) => void
  sohbetSec: (id: string) => Promise<void>
  mesajGonder: (mesaj: string, dosya?: File) => Promise<void>
  geriBildirimVer: (mesajId: number, deger: GeriBildirim | null) => Promise<void>
  aracToggle: (arac: AracGrubu) => void
}

export const useSohbetStore = create<SohbetStore>((set, get) => ({
  sohbetListesi: [],
  aktifSohbetId: null,
  aktifMod: 'GENEL',
  mesajlar: [],
  gonderiliyor: false,
  streamingId: null,
  kapaliAraclar: new Set(),
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
    // Aktif konusma ortasinda da mod degistirilebilir - konusma KAPANMAZ,
    // sadece bir sonraki mesaj yeni modla gider (backend her mesajda modu
    // istekten okur, Sohbet.mod sadece gosterim/arama icin ayrica
    // senkronize edilir, bkz. ChatService.modGuncelle).
    set({ aktifMod: mod })
  },

  async sohbetSec(id) {
    set({ gecmisYukleniyor: true })
    try {
      const mesajlar = await sohbetMesajlariniGetir(id)
      const bulunanSohbet = get().sohbetListesi.find((s) => s.id === id)
      set({
        aktifSohbetId: id,
        aktifMod: bulunanSohbet?.mod ?? 'GENEL',
        mesajlar: mesajlar.map((m) => mesajOzetindenChatMesaji(m, id)),
      })
    } finally {
      set({ gecmisYukleniyor: false })
    }
  },

  async mesajGonder(mesaj, dosya) {
    const kullaniciMesaji: ChatMessage = {
      id: crypto.randomUUID(),
      role: 'kullanici',
      content: mesaj,
      ek: dosya ? { url: URL.createObjectURL(dosya), mimeTipi: dosya.type, dosyaAdi: dosya.name } : undefined,
    }
    const asistanId = crypto.randomUUID()
    const { aktifSohbetId, aktifMod, kapaliAraclar } = get()

    set((durum) => ({
      mesajlar: [...durum.mesajlar, kullaniciMesaji, { id: asistanId, role: 'asistan', content: '' }],
      gonderiliyor: true,
      streamingId: asistanId,
    }))

    try {
      await streamChat(
        { conversationId: aktifSohbetId, mesaj, mod: aktifMod, kapaliAraclar: [...kapaliAraclar], dosya },
        (olay) => {
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
        if (olay.type === 'yapisalVeri') {
          set((durum) => ({
            mesajlar: durum.mesajlar.map((m) =>
              m.id === asistanId ? { ...m, yapisalVeri: olay.yapisalVeri } : m,
            ),
          }))
          return
        }
        if (olay.type === 'algilananMod') {
          set((durum) => ({
            mesajlar: durum.mesajlar.map((m) => (m.id === asistanId ? { ...m, algilananMod: olay.mod } : m)),
          }))
          return
        }
        if (olay.type === 'dogrulama') {
          set((durum) => ({
            mesajlar: durum.mesajlar.map((m) => (m.id === asistanId ? { ...m, dogrulama: olay.dogrulama } : m)),
          }))
          return
        }
        if (olay.type === 'hata') {
          set((durum) => ({
            mesajlar: durum.mesajlar.map((m) => (m.id === asistanId ? { ...m, hata: olay.mesaj } : m)),
          }))
          return
        }
        set((durum) => ({
          mesajlar: durum.mesajlar.map((m) => (m.id === asistanId ? { ...m, content: m.content + olay.text } : m)),
        }))
      })

      // Akis SSE ile mesajin kalici id'sini dondurmuyor (bkz. backend
      // ChatService.akisliYanitla) - geri bildirim (begen/begenme)
      // butonlarinin dogru mesaji hedefleyebilmesi icin akis bitince
      // gecmisi tekrar cekip gercek id'yi eslestiriyoruz.
      const guncelSohbetId = get().aktifSohbetId
      if (guncelSohbetId) {
        try {
          const gecmis = await sohbetMesajlariniGetir(guncelSohbetId)
          const sonAsistanMesaji = [...gecmis].reverse().find((m) => m.rol === 'ASISTAN')
          if (sonAsistanMesaji) {
            set((durum) => ({
              mesajlar: durum.mesajlar.map((m) =>
                m.id === asistanId ? { ...m, mesajId: sonAsistanMesaji.id } : m,
              ),
            }))
          }
        } catch {
          // mesajId eslesmezse sadece geri bildirim butonlari calismaz,
          // sohbetin kendisi etkilenmez - sessizce yut
        }
      }
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

  async geriBildirimVer(mesajId, deger) {
    const { aktifSohbetId, mesajlar } = get()
    if (!aktifSohbetId) return
    const oncekiDeger = mesajlar.find((m) => m.mesajId === mesajId)?.geriBildirim

    // Iyimser (optimistic) guncelleme - kullanici tikladigi anda buton
    // durumu degisir, istek basarisiz olursa asagida geri alinir.
    set((durum) => ({
      mesajlar: durum.mesajlar.map((m) => (m.mesajId === mesajId ? { ...m, geriBildirim: deger ?? undefined } : m)),
    }))

    try {
      await geriBildirimVerApi(aktifSohbetId, mesajId, deger)
    } catch {
      set((durum) => ({
        mesajlar: durum.mesajlar.map((m) => (m.mesajId === mesajId ? { ...m, geriBildirim: oncekiDeger } : m)),
      }))
    }
  },

  aracToggle(arac) {
    set((durum) => {
      const yeni = new Set(durum.kapaliAraclar)
      if (yeni.has(arac)) {
        yeni.delete(arac)
      } else {
        yeni.add(arac)
      }
      return { kapaliAraclar: yeni }
    })
  },
}))
