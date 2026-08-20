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

@Entity
@Table(name = "dokuman")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Dokuman {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dosya_adi", nullable = false)
    private String dosyaAdi;

    @Column(nullable = false)
    private String baslik;

    @Column(length = 80)
    private String kategori;

    // Hangi sohbet moduna ait oldugu (SohbetModu.name(), orn. "IMAR") - null/bos
    // ise sadece GENEL modda aranir. RAG aramasi bu alana gore filtrelenir
    // (bkz. ChatService) - her modun kendi belge havuzu olur, "derli toplu"
    // hedefi icin.
    @Column(length = 20)
    private String mod;

    @Column(name = "yuklenme_tarihi", nullable = false)
    private LocalDateTime yuklenmeTarihi;

    @Column(name = "chunk_sayisi")
    private Integer chunkSayisi;
}
