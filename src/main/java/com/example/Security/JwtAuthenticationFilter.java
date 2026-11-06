package com.example.Security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtre qui intercepte chaque requête entrante pour vérifier la présence 
 * et la validité d'un JSON Web Token (JWT).
 */
@Component
@RequiredArgsConstructor
@SuppressWarnings("deprecation")
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        
        // 1. On extrait l'en-tête "Authorization" de la requête
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;

        // 2. Si l'en-tête est absent ou ne commence pas par "Bearer ", on ignore et on passe au filtre suivant
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 3. On récupère le token pur (on enlève les 7 premiers caractères : "Bearer ")
        jwt = authHeader.substring(7);
        // 4. On extrait l'email (le subject) depuis le token via notre JwtService
        userEmail = jwtService.extractUsername(jwt);

        // 5. Si on a un email et que l'utilisateur n'est pas déjà authentifié dans ce contexte
        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            
            // On charge les détails de l'utilisateur depuis la base de données
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);
            
            // 6. On vérifie si le token est valide (non expiré et correspond au bon utilisateur)
            if (jwtService.isTokenValid(jwt, userDetails)) {
                
                // Si c'est valide, on crée un objet d'authentification pour Spring Security
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );
                
                // On y ajoute les détails de la requête (comme l'adresse IP, etc.)
                authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );
                
                // 7. On place l'utilisateur authentifié dans le contexte de sécurité de Spring
                SecurityContext context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(authToken);
                SecurityContextHolder.setContext(context);
            }
        }
        
        // 8. On passe la main au filtre suivant (la requête peut continuer son chemin)
        filterChain.doFilter(request, response);
    }
}