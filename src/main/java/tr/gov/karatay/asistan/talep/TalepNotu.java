package tr.gov.karatay.asistan.talep;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

@Entity
@Table(name = "talep_notu")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TalepNotu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "talep_id", nullable = false)
    private Talep talep;

    @Column(length = 120)
    private String personel;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String notu;

    @Column(nullable = false)
    private LocalDateTime tarih;
}
