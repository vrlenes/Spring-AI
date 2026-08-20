import { DurumRozeti, OncelikMetni } from '@/components/talep/rozetler'
import type { YapisalVeriPaketi } from '@/types/chat'
import type { TalepOzeti } from '@/types/talep'

// Modelin sohbette yazdigi metne degil, tool'un dondurdugu ham veriye
// dayanir (bkz. TalepTools.kaydetYapisalVeri, ChatService) - ayni
// kaynaklar/araclar ilkesi. "tip" alanina gore hangi kart render edilecegi
// burada secilir; yeni bir tip icin buraya yeni bir dal eklemek yeterli.
function TalepListesiTablosu({ talepler }: { talepler: TalepOzeti[] }) {
  if (talepler.length === 0) return null

  return (
    <div className="mt-2.5 overflow-x-auto rounded-lg border">
      <table className="w-full text-left text-[12px]">
        <thead className="bg-muted/50 text-[10.5px] tracking-wide text-muted-foreground uppercase">
          <tr>
            <th className="px-2.5 py-1.5 font-medium">Takip No</th>
            <th className="px-2.5 py-1.5 font-medium">Konu</th>
            <th className="px-2.5 py-1.5 font-medium">Müdürlük</th>
            <th className="px-2.5 py-1.5 font-medium">Durum</th>
            <th className="px-2.5 py-1.5 font-medium">Öncelik</th>
          </tr>
        </thead>
        <tbody>
          {talepler.map((t) => (
            <tr key={t.takipNo} className="border-t">
              <td className="px-2.5 py-1.5 font-mono text-[11px] whitespace-nowrap">{t.takipNo}</td>
              <td className="max-w-[220px] truncate px-2.5 py-1.5" title={t.konuMetni}>
                {t.konuMetni}
              </td>
              <td className="px-2.5 py-1.5 whitespace-nowrap text-muted-foreground">
                {t.mudurlukAdi ?? '(atanmamış)'}
              </td>
              <td className="px-2.5 py-1.5">
                <DurumRozeti durum={t.durum} />
              </td>
              <td className="px-2.5 py-1.5">
                <OncelikMetni oncelik={t.oncelik} />
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

export function YapisalVeriKarti({ paket }: { paket: YapisalVeriPaketi }) {
  if (paket.tip === 'TALEP_LISTESI') {
    return <TalepListesiTablosu talepler={paket.veri as TalepOzeti[]} />
  }
  return null
}
