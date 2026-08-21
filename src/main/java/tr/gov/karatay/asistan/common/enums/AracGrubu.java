package tr.gov.karatay.asistan.common.enums;

// Sohbet arayuzundeki "Araçlar" panelinden kullanicinin o konusma icin
// kapatabildigi tool gruplari (bkz. ChatService.sistemPromptuOlustur). Bu bir
// sert engelleme degil - ayni TALEP modunun belgeAra'yi yasaklamasi gibi,
// sistem promptuna eklenen bir kurala dayanir (bkz. CLAUDE.md'deki mevcut
// mod izolasyonu ilkesiyle ayni, kanitlanmis mekanizma).
public enum AracGrubu {
    RAG,
    TALEP,
    KURUM_DIZIN
}
