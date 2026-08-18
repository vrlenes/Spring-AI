package tr.gov.karatay.asistan.talep;

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
import tr.gov.karatay.asistan.common.enums.TalepDurumu;
import tr.gov.karatay.asistan.common.enums.TalepOnceligi;
import tr.gov.karatay.asistan.mudurluk.Mudurluk;

@Entity
@Table(name = "talep")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Talep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "takip_no", nullable = false, unique = true, length = 20)
    private String takipNo;

    @Column(name = "vatandas_ad", length = 150)
    private String vatandasAd;

    @Column(length = 100)
    private String iletisim;

    @Column(length = 100)
    private String mahalle;

    @Column(name = "konu_metni", nullable = false, columnDefinition = "TEXT")
    private String konuMetni;

    @Column(length = 60)
    private String kategori;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mudurluk_id")
    private Mudurluk mudurluk;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TalepDurumu durum;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TalepOnceligi oncelik;

    @Column(name = "olusturma_tarihi", nullable = false)
    private LocalDateTime olusturmaTarihi;

    @Column(name = "guncelleme_tarihi")
    private LocalDateTime guncellemeTarihi;
}
