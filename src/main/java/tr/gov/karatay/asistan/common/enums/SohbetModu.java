package tr.gov.karatay.asistan.common.enums;

// Bu turda sadece GENEL ve TALEP tam calisir durumda (sistem promptu + arac
// seti ile). Gelecekte (Imar, Mevzuat vb.) yeni bir mod eklemek, bu enum'a
// yeni bir deger + ChatClientConfig'e karsilik gelen sistem promptunu
// eklemekten ibaret olmali.
public enum SohbetModu {
    GENEL,
    TALEP
}
