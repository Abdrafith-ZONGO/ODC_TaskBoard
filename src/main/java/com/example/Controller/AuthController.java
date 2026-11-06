package com.example.Controller;

import com.example.DTO.JwtResponseDTO;
import com.example.DTO.LoginRequestDTO;
import com.example.Security.JwtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Contrôleur gérant l'authentification des utilisateurs.
 * Il expose l'endpoint permettant de générer un token JWT.
 */
@RestController
@RequiredArgsConstructor
public class AuthController {

    // Gestionnaire de Spring Security pour vérifier les identifiants
    private final AuthenticationManager authenticationManager;
    
    // Notre service pour générer le token JWT
    private final JwtService jwtService;

    /**
     * Endpoint public pour se connecter.
     * Attend un JSON contenant l'email et le mot de passe.
     */
    @PostMapping("/login")
    public ResponseEntity<JwtResponseDTO> login(@Valid @RequestBody LoginRequestDTO loginRequestDTO) {
        
        // 1. On demande à Spring Security d'authentifier l'utilisateur avec l'email et le mot de passe fournis.
        // S'ils sont incorrects, une exception sera levée ici.
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequestDTO.getEmail(),
                        loginRequestDTO.getPassword()
                )
        );

        // 2. Si l'authentification réussit, on récupère l'utilisateur connecté (UserDetails)
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        
        // 3. On génère le token JWT pour cet utilisateur
        String jwtToken = jwtService.generateToken(userDetails);

        // 4. On retourne le token dans la réponse
        return ResponseEntity.ok(new JwtResponseDTO(jwtToken));
    }
}