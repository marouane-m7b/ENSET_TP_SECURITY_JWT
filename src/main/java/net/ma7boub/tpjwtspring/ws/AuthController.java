package net.ma7boub.tpjwtspring.ws;
import net.ma7boub.tpjwtspring.dto.AuthResponse;
import net.ma7boub.tpjwtspring.dto.LoginRequest;
import net.ma7boub.tpjwtspring.dto.RegisterRequest;
import net.ma7boub.tpjwtspring.entity.AppUser;
import net.ma7boub.tpjwtspring.repo.UserRepository;
import net.ma7boub.tpjwtspring.service.CustomUserDetailsService;
import net.ma7boub.tpjwtspring.service.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final CustomUserDetailsService customUserDetailsService;

    public AuthController(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            CustomUserDetailsService customUserDetailsService) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.customUserDetailsService = customUserDetailsService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(
            @RequestBody RegisterRequest registerRequest) {

        if (invalidCredentials(registerRequest.email(), registerRequest.pass())) {
            return ResponseEntity.badRequest().body("Email and password are required");
        }

        if (userRepository.existsByEmail(registerRequest.email())) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body("Email already exists");
        }

        AppUser appUser = new AppUser();
        appUser.setEmail(registerRequest.email());
        appUser.setPassword(
                passwordEncoder.encode(registerRequest.pass()));
        appUser.setRole("USER");

        userRepository.save(appUser);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body("Registration successful");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody LoginRequest loginRequest) {

        if (invalidCredentials(loginRequest.email(), loginRequest.pass())) {
            return ResponseEntity.badRequest().body("Email and password are required");
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.email(),
                            loginRequest.pass()
                    )
            );
        } catch (AuthenticationException exception) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid email or password");
        }

        UserDetails userDetails =
                customUserDetailsService.loadUserByUsername(
                        loginRequest.email());

        String token = jwtService.generateToken(userDetails);

        return ResponseEntity.ok(new AuthResponse(token));
    }

    private boolean invalidCredentials(String email, String password) {
        return email == null || email.isBlank() || password == null || password.isBlank();
    }
}
