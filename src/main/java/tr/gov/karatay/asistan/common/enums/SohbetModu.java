package tr.gov.karatay.asistan.common.enums;

// Yeni bir mod eklemek: bu enum'a yeni bir deger + ChatClientConfig'e
// karsilik gelen sistem promptu + (RAG'e ihtiyaci varsa) ChatService'te
// dokuman filtre eslemesi eklemekten ibaret olmali.
//
// OTOMATIK farkli: gercek bir sistem promptu/arac setine sahip bir mod
// DEGIL, bir sohbetin nasil ETIKETLENDIGI (bkz. Sohbet.mod) - kullanici bu
// modu secince ChatService her mesajda ModYonlendirmeService ile GENEL/
// TALEP/IMAR/RUHSAT'tan birini secip ONA gore calisir. OTOMATIK degeri
// asla RagTools.modFiltresi veya ChatService.sistemPromptuOverride'a
// ulasmamali - ikisi de bu durumu savunmaci sekilde (IllegalStateException)
// isaretler.
public enum SohbetModu {
    GENEL,
    TALEP,
    IMAR,
    RUHSAT,
    OTOMATIK
}
