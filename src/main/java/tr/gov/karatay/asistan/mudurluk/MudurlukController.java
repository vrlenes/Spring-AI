package tr.gov.karatay.asistan.mudurluk;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import tr.gov.karatay.asistan.mudurluk.dto.MudurlukOzeti;

@RestController
@RequestMapping("/api/mudurlukler")
public class MudurlukController {

    private final MudurlukService mudurlukService;

    public MudurlukController(MudurlukService mudurlukService) {
        this.mudurlukService = mudurlukService;
    }

    @GetMapping
    public List<MudurlukOzeti> listele() {
        return mudurlukService.mudurlukleriListele();
    }
}
