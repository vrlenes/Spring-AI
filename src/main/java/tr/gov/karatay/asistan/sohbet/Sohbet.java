package tr.gov.karatay.asistan.sohbet;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import tr.gov.karatay.asistan.common.enums.SohbetModu;
import tr.gov.karatay.asistan.personel.Personel;

// Id, DB'de otomatik uretilmiyor: Spring AI ChatMemory'nin conversationId'siyle
// AYNI deger olarak (UUID string) uygulama tarafinda uretiliyor (bkz.
// SohbetService.sohbetBaslat) - boylece LLM baglam penceresi ile kalici
// mesaj gecmisi tek bir id uzerinden eslesiyor, iki ayri id yonetilmiyor.
@Entity
@Table(name = "sohbet")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Sohbet {

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "personel_id", nullable = false)
    private Personel personel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SohbetModu mod;

    @Column(length = 200)
    private String baslik;

    @Column(name = "olusturma_tarihi", nullable = false)
    private LocalDateTime olusturmaTarihi;

    @Column(name = "guncelleme_tarihi", nullable = false)
    private LocalDateTime guncellemeTarihi;
}
