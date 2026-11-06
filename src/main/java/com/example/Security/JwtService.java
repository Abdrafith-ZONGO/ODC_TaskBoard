package com.example.Security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Service gérant toute la logique liée aux JSON Web Tokens (JWT).
 * C'est ici que les tokens sont créés, décodés et validés.
 */
@Service
public class JwtService {

    // La clé secrète utilisée pour signer le JWT (chargée depuis application.properties)
    @Value("${jwt.secret}")
    private String secretKey;

    // La durée de validité du token en millisecondes (chargée depuis application.properties)
    @Value("${jwt.expiration}")
    private long jwtExpiration;

    /**
     * Extrait le nom d'utilisateur (email) contenu dans le token.
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Méthode générique pour extraire n'importe quelle donnée (Claim) du token.
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Génère un nouveau token JWT pour un utilisateur donné, sans données additionnelles.
     */
    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    /**
     * Génère un nouveau token JWT en y incluant des données supplémentaires (extraClaims).
     */
    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername()) // On utilise l'email comme sujet
                .setIssuedAt(new Date(System.currentTimeMillis())) // Date de création
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration)) // Date d'expiration
                .signWith(getSignInKey()) // Signature du token avec la clé secrète
                .compact(); // Génération de la chaîne de caractères finale (le token JWT)
    }

    /**
     * Vérifie si le token est toujours valide pour l'utilisateur fourni.
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        // Le token est valide si l'email correspond et qu'il n'est pas expiré
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    /**
     * Vérifie si la date d'expiration du token est dépassée.
     */
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Extrait la date d'expiration du token.
     */
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Parse (déchiffre) le token JWT complet pour lire tout ce qu'il contient (le Body/Claims).
     * Si le token a été altéré ou est mal signé, cette méthode lancera une exception.
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Décode la clé secrète (qui est en Base64) pour générer un objet Key utilisable par JJWT.
     */
    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}