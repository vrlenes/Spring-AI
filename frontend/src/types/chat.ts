export interface Kaynak {
  baslik: string
  parcaNo: number
  benzerlik: number | null
}

export interface ChatMessage {
  id: string
  role: 'kullanici' | 'asistan'
  content: string
  kaynaklar?: Kaynak[]
}

export type ChatStreamEvent =
  | { type: 'conversationId'; conversationId: string }
  | { type: 'token'; text: string }
  | { type: 'kaynaklar'; kaynaklar: Kaynak[] }
