package tr.gov.karatay.asistan.common.enums;

// Yeni bir mod eklemek: bu enum'a yeni bir deger + ChatClientConfig'e
// karsilik gelen sistem promptu + (RAG'e ihtiyaci varsa) ChatService'te
// dokuman filtre eslemesi eklemekten ibaret olmali.
public enum SohbetModu {
    GENEL,
    TALEP,
    IMAR,
    RUHSAT
}
