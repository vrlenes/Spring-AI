package tr.gov.karatay.asistan.mudurluk;

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
@Table(name = "mudurluk")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Mudurluk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 120)
    private String ad;

    @Column(name = "sorumluluk_alani", columnDefinition = "TEXT")
    private String sorumlulukAlani;

    @Column(nullable = false)
    private Boolean aktif = true;
}
