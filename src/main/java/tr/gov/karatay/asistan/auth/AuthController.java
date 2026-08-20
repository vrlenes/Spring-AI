package tr.gov.karatay.asistan.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import tr.gov.karatay.asistan.auth.dto.LoginIstegi;
import tr.gov.karatay.asistan.auth.dto.PersonelOzeti;
import tr.gov.karatay.asistan.personel.PersonelDetails;

// Ozel (form-login degil) JSON login: kimlik dogrulama basarili olduktan
// sonra SecurityContext'i ACIKCA HttpSessionSecurityContextRepository ile
// kaydediyoruz - Spring Security 6'da bu artik otomatik olmuyor, aksi halde
// sonraki istekler oturumu tanimaz (bkz. Spring Security guncel dokumantasyonu,
// "Authentication Persistence and Session Management").
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;

    public AuthController(AuthenticationManager authenticationManager, SecurityContextRepository securityContextRepository) {
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
    }

    @PostMapping("/login")
    public PersonelOzeti login(@Valid @RequestBody LoginIstegi istek, HttpServletRequest request, HttpServletResponse response) {
        Authentication token = UsernamePasswordAuthenticationToken.unauthenticated(istek.kullaniciAdi(), istek.sifre());
        Authentication sonuc = authenticationManager.authenticate(token);

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(sonuc);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);

        return ozetleVer((PersonelDetails) sonuc.getPrincipal());
    }

    @PostMapping("/logout")
    public void logout(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        var oturum = request.getSession(false);
        if (oturum != null) {
            oturum.invalidate();
        }
    }

    @GetMapping("/me")
    public ResponseEntity<PersonelOzeti> mevcutPersonel(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof PersonelDetails details)) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(ozetleVer(details));
    }

    private PersonelOzeti ozetleVer(PersonelDetails details) {
        var personel = details.getPersonel();
        return new PersonelOzeti(personel.getId(), personel.getKullaniciAdi(), personel.getAdSoyad());
    }
}
