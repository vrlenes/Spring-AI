package tr.gov.karatay.asistan.rag;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// RagTools.belgeAra'nin HER cagrisinin kalici bir kaydi - hangi sorgularin
// zayif/sifir sonuc aldigini sonradan sorgulanabilir kilmak icin (bkz.
// RagAramaLoguService). Kullanici/sohbet bilgisi kasitli olarak TUTULMUYOR -
// bu log sistemin arama kalitesini izler, kimin sordugunu degil.
@Entity
@Table(name = "rag_arama_logu")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RagAramaLogu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 20)
    private String mod;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String sorgu;

    @Column(name = "sonuc_sayisi", nullable = false)
    private int sonucSayisi;

    @Column(name = "en_iyi_benzerlik")
    private Double enIyiBenzerlik;

    @Column(name = "olusturma_tarihi", nullable = false)
    private LocalDateTime olusturmaTarihi;
}
