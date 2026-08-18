package tr.gov.karatay.asistan.chat;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ChatViewController {

    @GetMapping("/")
    public String anaSayfa() {
        return "chat";
    }
}
