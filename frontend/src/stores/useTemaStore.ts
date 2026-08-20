import { create } from 'zustand'

type Tema = 'light' | 'dark'

function baslangicTemasi(): Tema {
  const kayitli = localStorage.getItem('tema')
  if (kayitli === 'light' || kayitli === 'dark') return kayitli
  return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
}

function uygula(tema: Tema) {
  document.documentElement.classList.toggle('dark', tema === 'dark')
  localStorage.setItem('tema', tema)
}

interface TemaStore {
  tema: Tema
  temaDegistir: () => void
}

// index.html'deki inline script sayfa ilk cizilirken class'i zaten dogru
// ayarliyor (flash'i onlemek icin) - burada uygula() tekrar cagrilmasi
// zararsiz, sadece store ile DOM'u senkron tutuyor.
export const useTemaStore = create<TemaStore>((set, get) => ({
  tema: baslangicTemasi(),

  temaDegistir() {
    const yeni: Tema = get().tema === 'dark' ? 'light' : 'dark'
    uygula(yeni)
    set({ tema: yeni })
  },
}))
