package tr.gov.karatay.asistan.chat.dto;

import java.util.List;

import tr.gov.karatay.asistan.common.enums.SohbetModu;
import tr.gov.karatay.asistan.talep.dto.PendingActionOzeti;

// algilananMod: SADECE istek OTOMATIK modundaysa dolu - ModYonlendirmeService'in
// bu mesaj icin sectigi gercek mod (GENEL/TALEP/IMAR/RUHSAT). Diger tum
// modlarda null, frontend'de bir rozet olarak gosterilir.
// mesajId: bu cevabin kalici sohbet_mesaji kaydinin id'si - frontend'in
// begen/begenme geri bildirimini dogru mesaja gonderebilmesi icin.
// dogrulama: SADECE RAG kaynagi kullanildiysa dolu - KaynakDogrulamaService'in
// cevabin kaynaga sadik olup olmadigina dair bagimsiz kontrolu.
public record ChatResponse(
        String conversationId,
        String cevap,
        List<Kaynak> kaynaklar,
        PendingActionOzeti bekleyenIslem,
        List<String> araclar,
        YapisalVeriPaketi yapisalVeri,
        SohbetModu algilananMod,
        Long mesajId,
        KaynakDogrulamaSonucu dogrulama) {
}
