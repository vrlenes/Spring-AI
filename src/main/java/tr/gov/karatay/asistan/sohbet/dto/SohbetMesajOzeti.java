package tr.gov.karatay.asistan.sohbet.dto;

import java.time.LocalDateTime;
import java.util.List;

import tr.gov.karatay.asistan.chat.dto.Kaynak;
import tr.gov.karatay.asistan.chat.dto.YapisalVeriPaketi;
import tr.gov.karatay.asistan.common.enums.MesajRolu;
import tr.gov.karatay.asistan.talep.dto.PendingActionOzeti;

public record SohbetMesajOzeti(
        MesajRolu rol,
        String icerik,
        List<Kaynak> kaynaklar,
        List<String> araclar,
        PendingActionOzeti bekleyenIslem,
        YapisalVeriPaketi yapisalVeri,
        LocalDateTime olusturmaTarihi) {
}
