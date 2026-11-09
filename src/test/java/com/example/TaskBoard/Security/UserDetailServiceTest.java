package com.example.TaskBoard.Security;


import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.example.Model.UserModel;
import com.example.Repository.UserRepository;
import com.example.Security.UserDetailService;

/**
 * =============================================================================
 * TESTS UNITAIRES : UserDetailServiceTest
 * =============================================================================
 *
 * OBJECTIF PÉDAGOGIQUE :
 * - `UserDetailsService` est l'interface pivot de Spring Security pour charger
 *   les données d'authentification d'un utilisateur depuis une source de données.
 * - Ce test valide deux aspects cruciaux :
 *     1. Si l'utilisateur existe en base, il est transformé en `UserDetails` avec
 *        au minimum une autorité (`ROLE_USER`), évitant ainsi le crash de Spring Security.
 *     2. Si l'email est introuvable, l'exception standard `UsernameNotFoundException`
 *        doit être levée conformément au contrat de Spring Security.
 */
@ExtendWith(MockitoExtension.class)
class UserDetailServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserDetailService userDetailService;

    private UserModel user;

    @BeforeEach
    void setUp() {
        user = new UserModel();
        user.setId(1L);
        user.setName("Amadou");
        user.setEmail("amadou@example.com");
        user.setPassword("hashed_secret");
    }

    @Test
    @DisplayName("loadUserByUsername - Utilisateur trouvé : retourne UserDetails avec ROLE_USER")
    void loadUserByUsername_UserFound_ReturnsUserDetails() {
        // 1. ARRANGE : On simule la présence de l'utilisateur en base
        when(userRepository.findByEmail("amadou@example.com")).thenReturn(Optional.of(user));

        // 2. ACT : Chargement de l'utilisateur
        UserDetails userDetails = userDetailService.loadUserByUsername("amadou@example.com");

        // 3. ASSERT
        assertNotNull(userDetails);
        assertEquals("amadou@example.com", userDetails.getUsername());
        assertEquals("hashed_secret", userDetails.getPassword());
        
        // Point clé : vérifier la présence de l'autorité "ROLE_USER"
        boolean hasUserRole = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_USER"));
        assertTrue(hasUserRole, "L'utilisateur doit posséder l'autorité ROLE_USER");
    }

    @Test
    @DisplayName("loadUserByUsername - Utilisateur non trouvé : lance UsernameNotFoundException")
    void loadUserByUsername_UserNotFound_ThrowsException() {
        // 1. ARRANGE : Aucun utilisateur avec cet email
        when(userRepository.findByEmail("inconnu@example.com")).thenReturn(Optional.empty());

        // 2. ACT & 3. ASSERT
        assertThrows(UsernameNotFoundException.class,
                () -> userDetailService.loadUserByUsername("inconnu@example.com"),
                "Doit lever UsernameNotFoundException quand l'email n'existe pas");
    }
}

