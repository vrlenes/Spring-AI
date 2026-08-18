export interface ChatMessage {
  id: string
  role: 'kullanici' | 'asistan'
  content: string
}

export type ChatStreamEvent =
  | { type: 'conversationId'; conversationId: string }
  | { type: 'token'; text: string }
