package tr.gov.karatay.asistan.sohbet;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.fasterxml.jackson.databind.ObjectMapper;

import tr.gov.karatay.asistan.chat.dto.Kaynak;
import tr.gov.karatay.asistan.common.enums.MesajRolu;
import tr.gov.karatay.asistan.common.enums.SohbetModu;
import tr.gov.karatay.asistan.personel.Personel;
import tr.gov.karatay.asistan.personel.PersonelRepository;
import tr.gov.karatay.asistan.sohbet.dto.SohbetMesajOzeti;
import tr.gov.karatay.asistan.sohbet.dto.SohbetOzeti;
import tr.gov.karatay.asistan.talep.PendingActionTuru;
import tr.gov.karatay.asistan.talep.dto.PendingActionOzeti;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// kaynaklar/araclar/bekleyenIslem alanlari DB'de JSON string olarak tutuluyor
// (bkz. SohbetMesaji, SohbetService) - buradaki testler bunun round-trip
// (yaz -> oku) dogru calistigini ve modelin kendi ifadesine degil bu alanlara
// guvenildigini (CLAUDE.md "kaynak koddan uretilir") dogruluyor. Gercek
// ObjectMapper kullanilir, mocklanmaz - serialize/deserialize mantiginin
// kendisi test edilen sey.
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SohbetServiceTest {

    @Mock
    private SohbetRepository sohbetRepository;

    @Mock
    private SohbetMesajiRepository sohbetMesajiRepository;

    @Mock
    private PersonelRepository personelRepository;

    private SohbetService sohbetService;

    @BeforeEach
    void hazirla() {
        sohbetService = new SohbetService(sohbetRepository, sohbetMesajiRepository, personelRepository, new ObjectMapper());
        when(sohbetRepository.save(any(Sohbet.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private Personel ornekPersonel(long id) {
        Personel personel = new Personel();
        personel.setId(id);
        personel.setKullaniciAdi("admin");
        personel.setAdSoyad("Yönetici");
        personel.setOlusturmaTarihi(LocalDateTime.now());
        return personel;
    }

    private Sohbet ornekSohbet(String id, String baslik) {
        Sohbet sohbet = new Sohbet();
        sohbet.setId(id);
        sohbet.setPersonel(ornekPersonel(1L));
        sohbet.setMod(SohbetModu.GENEL);
        sohbet.setBaslik(baslik);
        sohbet.setOlusturmaTarihi(LocalDateTime.now());
        sohbet.setGuncellemeTarihi(LocalDateTime.now());
        return sohbet;
    }

    @Nested
    class SohbetBaslatTestleri {

        @Test
        void gecerliUuidIdIleYeniSohbetOlusturupKaydeder() {
            when(personelRepository.getReferenceById(1L)).thenReturn(ornekPersonel(1L));

            Sohbet sonuc = sohbetService.sohbetBaslat(1L, SohbetModu.TALEP);

            assertThat(UUID.fromString(sonuc.getId())).isNotNull();
            assertThat(sonuc.getMod()).isEqualTo(SohbetModu.TALEP);
            assertThat(sonuc.getPersonel().getId()).isEqualTo(1L);
            assertThat(sonuc.getOlusturmaTarihi()).isNotNull();
            assertThat(sonuc.getGuncellemeTarihi()).isNotNull();
        }
    }

    @Nested
    class SohbetleriGetirTestleri {

        @Test
        void personelinSohbetleriniOzetOlarakDoner() {
            Sohbet s1 = ornekSohbet("id-1", "İlk sohbet");
            Sohbet s2 = ornekSohbet("id-2", null);
            when(sohbetRepository.findByPersonelIdOrderByGuncellemeTarihiDesc(1L)).thenReturn(List.of(s1, s2));

            List<SohbetOzeti> sonuc = sohbetService.sohbetleriGetir(1L);

            assertThat(sonuc).hasSize(2);
            assertThat(sonuc.get(0).id()).isEqualTo("id-1");
            assertThat(sonuc.get(0).baslik()).isEqualTo("İlk sohbet");
            assertThat(sonuc.get(1).baslik()).isNull();
        }
    }

    @Nested
    class MesajEkleTestleri {

        @Test
        void ilkKullaniciMesajindaBaslikOtomatikUretilir() {
            Sohbet sohbet = ornekSohbet("id-1", null);
            when(sohbetRepository.findById("id-1")).thenReturn(Optional.of(sohbet));

            sohbetService.mesajEkle("id-1", MesajRolu.KULLANICI, "Merhaba, bir sorum var", null, null, null);

            ArgumentCaptor<Sohbet> captor = ArgumentCaptor.forClass(Sohbet.class);
            verify(sohbetRepository).save(captor.capture());
            assertThat(captor.getValue().getBaslik()).isEqualTo("Merhaba, bir sorum var");
        }

        @Test
        void uzunMesajBaslikOlarakKisaltilir() {
            Sohbet sohbet = ornekSohbet("id-1", null);
            when(sohbetRepository.findById("id-1")).thenReturn(Optional.of(sohbet));
            String uzunMesaj = "a".repeat(100);

            sohbetService.mesajEkle("id-1", MesajRolu.KULLANICI, uzunMesaj, null, null, null);

            ArgumentCaptor<Sohbet> captor = ArgumentCaptor.forClass(Sohbet.class);
            verify(sohbetRepository).save(captor.capture());
            assertThat(captor.getValue().getBaslik()).hasSize(61).endsWith("…");
        }

        @Test
        void mevcutBaslikVarsaDegistirilmez() {
            Sohbet sohbet = ornekSohbet("id-1", "Var olan başlık");
            when(sohbetRepository.findById("id-1")).thenReturn(Optional.of(sohbet));

            sohbetService.mesajEkle("id-1", MesajRolu.KULLANICI, "Yeni mesaj", null, null, null);

            ArgumentCaptor<Sohbet> captor = ArgumentCaptor.forClass(Sohbet.class);
            verify(sohbetRepository).save(captor.capture());
            assertThat(captor.getValue().getBaslik()).isEqualTo("Var olan başlık");
        }

        @Test
        void asistanMesajindaBaslikUretilmez() {
            Sohbet sohbet = ornekSohbet("id-1", null);
            when(sohbetRepository.findById("id-1")).thenReturn(Optional.of(sohbet));

            sohbetService.mesajEkle("id-1", MesajRolu.ASISTAN, "Cevap metni", null, null, null);

            ArgumentCaptor<Sohbet> captor = ArgumentCaptor.forClass(Sohbet.class);
            verify(sohbetRepository).save(captor.capture());
            assertThat(captor.getValue().getBaslik()).isNull();
        }

        @Test
        void kaynaklarAraclarBekleyenIslemJsonOlarakSaklanir() {
            Sohbet sohbet = ornekSohbet("id-1", "Başlık");
            when(sohbetRepository.findById("id-1")).thenReturn(Optional.of(sohbet));
            List<Kaynak> kaynaklar = List.of(new Kaynak("Belge", 1, 0.9));
            List<String> araclar = List.of("Talepler sorgulandı");
            PendingActionOzeti bekleyenIslem =
                    new PendingActionOzeti("pid-1", PendingActionTuru.MUDURLUGE_ATA, "TLP-2026-00001", "açıklama");

            sohbetService.mesajEkle("id-1", MesajRolu.ASISTAN, "Cevap", kaynaklar, araclar, bekleyenIslem);

            ArgumentCaptor<SohbetMesaji> captor = ArgumentCaptor.forClass(SohbetMesaji.class);
            verify(sohbetMesajiRepository).save(captor.capture());
            SohbetMesaji kaydedilen = captor.getValue();
            assertThat(kaydedilen.getKaynaklar()).contains("Belge");
            assertThat(kaydedilen.getAraclar()).contains("Talepler sorgulandı");
            assertThat(kaydedilen.getBekleyenIslem()).contains("TLP-2026-00001");
        }

        @Test
        void bosListelerNullOlarakSaklanir() {
            Sohbet sohbet = ornekSohbet("id-1", "Başlık");
            when(sohbetRepository.findById("id-1")).thenReturn(Optional.of(sohbet));

            sohbetService.mesajEkle("id-1", MesajRolu.KULLANICI, "Mesaj", null, null, null);

            ArgumentCaptor<SohbetMesaji> captor = ArgumentCaptor.forClass(SohbetMesaji.class);
            verify(sohbetMesajiRepository).save(captor.capture());
            assertThat(captor.getValue().getKaynaklar()).isNull();
            assertThat(captor.getValue().getAraclar()).isNull();
            assertThat(captor.getValue().getBekleyenIslem()).isNull();
        }

        @Test
        void bulunmayanSohbeteMesajEklenemez() {
            when(sohbetRepository.findById("olmayan-id")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> sohbetService.mesajEkle("olmayan-id", MesajRolu.KULLANICI, "Mesaj", null, null, null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class MesajlariGetirTestleri {

        @Test
        void sahibiOlanSohbetinMesajlariJsonGeriOkunarakDoner() throws Exception {
            Sohbet sohbet = ornekSohbet("id-1", "Başlık");
            when(sohbetRepository.findByIdAndPersonelId("id-1", 1L)).thenReturn(Optional.of(sohbet));

            ObjectMapper om = new ObjectMapper();
            SohbetMesaji mesaj = new SohbetMesaji();
            mesaj.setId(1L);
            mesaj.setSohbet(sohbet);
            mesaj.setRol(MesajRolu.ASISTAN);
            mesaj.setIcerik("Cevap metni");
            mesaj.setKaynaklar(om.writeValueAsString(List.of(new Kaynak("Belge", 2, 0.8))));
            mesaj.setAraclar(om.writeValueAsString(List.of("Talepler sorgulandı")));
            mesaj.setBekleyenIslem(null);
            mesaj.setOlusturmaTarihi(LocalDateTime.now());
            when(sohbetMesajiRepository.findBySohbetIdOrderByOlusturmaTarihiAsc("id-1")).thenReturn(List.of(mesaj));

            List<SohbetMesajOzeti> sonuc = sohbetService.mesajlariGetir("id-1", 1L);

            assertThat(sonuc).hasSize(1);
            assertThat(sonuc.get(0).icerik()).isEqualTo("Cevap metni");
            assertThat(sonuc.get(0).kaynaklar()).hasSize(1);
            assertThat(sonuc.get(0).kaynaklar().get(0).baslik()).isEqualTo("Belge");
            assertThat(sonuc.get(0).araclar()).containsExactly("Talepler sorgulandı");
            assertThat(sonuc.get(0).bekleyenIslem()).isNull();
        }

        @Test
        void baskaPersonelinSohbetineErisilemez() {
            when(sohbetRepository.findByIdAndPersonelId("id-1", 2L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> sohbetService.mesajlariGetir("id-1", 2L))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
