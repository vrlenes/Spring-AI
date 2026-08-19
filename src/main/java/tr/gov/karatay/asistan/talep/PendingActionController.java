package tr.gov.karatay.asistan.talep;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import tr.gov.karatay.asistan.talep.dto.TalepDetay;

@RestController
@RequestMapping("/api/pending-actions")
public class PendingActionController {

    private final PendingActionService pendingActionService;

    public PendingActionController(PendingActionService pendingActionService) {
        this.pendingActionService = pendingActionService;
    }

    @PostMapping("/{id}/onayla")
    public ResponseEntity<TalepDetay> onayla(@PathVariable String id) {
        return ResponseEntity.ok(pendingActionService.onayla(id));
    }

    @PostMapping("/{id}/iptal")
    public ResponseEntity<Void> iptal(@PathVariable String id) {
        pendingActionService.iptalEt(id);
        return ResponseEntity.noContent().build();
    }
}
