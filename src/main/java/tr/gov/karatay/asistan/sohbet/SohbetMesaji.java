package tr.gov.karatay.asistan.sohbet;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import tr.gov.karatay.asistan.common.enums.MesajRolu;

// kaynaklar/araclar/bekleyenIslem JSON string olarak (TEXT kolon) tutulur -
// Hibernate'in JSON tipiyle ugrasmadan, ObjectMapper ile serialize/deserialize
// edilir (bkz. SohbetService). Bu, ChatResponse DTO'sundaki ayni alanlarin
// kalici karsiligi - sohbet gecmisi UI'da render edilirken modelin o anki
// metnine degil bu alanlara guvenilir (bkz. CLAUDE.md "kaynak koddan uretilir").
@Entity
@Table(name = "sohbet_mesaji")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SohbetMesaji {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sohbet_id", nullable = false)
    private Sohbet sohbet;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MesajRolu rol;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String icerik;

    @Column(columnDefinition = "TEXT")
    private String kaynaklar;

    @Column(columnDefinition = "TEXT")
    private String araclar;

    @Column(name = "bekleyen_islem", columnDefinition = "TEXT")
    private String bekleyenIslem;

    @Column(name = "yapisal_veri", columnDefinition = "TEXT")
    private String yapisalVeri;

    // Mesaja ozel, gecici bir ek (gorsel/PDF) - kalici RAG doküman havuzuna
    // (Dokuman/vector_store) DAHIL DEGIL, sadece bu mesajin Gemini'ye
    // multimodal icerik olarak gonderilmesi ve gecmiste tekrar goruntulenmesi
    // icin. @Lob KULLANILMADI: Hibernate 6.x'te duz byte[] alani PostgreSQL'de
    // doğrudan bytea'ya eslenir (oid/large-object degil).
    @Column(name = "ek_veri")
    private byte[] ekVeri;

    @Column(name = "ek_mime_tipi", length = 100)
    private String ekMimeTipi;

    @Column(name = "ek_dosya_adi", length = 255)
    private String ekDosyaAdi;

    @Column(name = "olusturma_tarihi", nullable = false)
    private LocalDateTime olusturmaTarihi;
}
