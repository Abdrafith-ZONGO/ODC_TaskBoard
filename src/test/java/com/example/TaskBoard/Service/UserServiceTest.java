package com.example.TaskBoard.Service;

import com.example.DTO.UserRequestDTO;
import com.example.DTO.UserResponseDTO;
import com.example.Exception.DuplicateResourceException;
import com.example.Exception.ResourceNotFoundException;
import com.example.Mapper.UserMapper;
import com.example.Model.UserModel;
import com.example.Repository.UserRepository;
import com.example.Service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * =============================================================================
 * TESTS UNITAIRES : UserServiceTest
 * =============================================================================
 * 
 * OBJECTIF  :
 * - Tester la logique métier du service `UserService` de manière ISOLÉE.
 * - Aucune base de données réelle ni contexte Spring n'est démarré ici (très rapide).
 * - On utilise Mockito pour simuler (mocker) les dépendances externes :
 *     * `UserRepository` : simule les interactions avec la base de données.
 *     * `UserMapper`     : simule la conversion entre DTOs et Entités.
 *     * `PasswordEncoder`: simule le hachage sécurisé du mot de passe.
 *
 * STRUCTURE DE CHAQUE TEST :
 * - Pattern AAA (Arrange, Act, Assert) / Given - When - Then :
 *     1. ARRANGE : Préparation des données et programmation des mocks (when...thenReturn).
 *     2. ACT     : Appel de la méthode métier testée.
 *     3. ASSERT  : Vérification des résultats et des interactions avec les mocks.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    // Dépendances simulées (mocks)
    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    // Instance réelle du service dans laquelle Mockito injecte automatiquement les mocks ci-dessus
    @InjectMocks
    private UserService userService;

    // Données réutilisables dans les tests
    private UserModel user;
    private UserRequestDTO userRequestDTO;
    private UserResponseDTO userResponseDTO;

    /**
     * Exécuté avant chaque méthode de test (@Test) pour repartir d'un état propre.
     */
    @BeforeEach
    void setUp() {
        // Entité simulée en base
        user = new UserModel();
        user.setId(1L);
        user.setName("Alice Dupont");
        user.setEmail("alice@example.com");
        user.setPassword("encoded_secret");

        // DTO entrant (données envoyées par un client)
        userRequestDTO = new UserRequestDTO("Alice Dupont", "alice@example.com", "secret123");

        // DTO sortant (réponse renvoyée au client, sans le mot de passe)
        userResponseDTO = new UserResponseDTO(1L, "Alice Dupont", "alice@example.com");
    }

    // =========================================================================
    // TESTS : CREATE USER
    // =========================================================================

    @Test
    @DisplayName("createUser - Cas nominal : création réussie avec mot de passe haché")
    void createUser_Success() {
        // 1. ARRANGE (Préparation des comportements simulés)
        // L'email n'existe pas encore en base
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        // Le mapper convertit le DTO en entité
        when(userMapper.toEntity(userRequestDTO)).thenReturn(user);
        // Le passwordEncoder hache le mot de passe en clair
        when(passwordEncoder.encode("secret123")).thenReturn("encoded_secret");
        // Le repository sauvegarde et renvoie l'entité avec son ID
        when(userRepository.save(user)).thenReturn(user);
        // Le mapper convertit l'entité sauvegardée en DTO de réponse
        when(userMapper.toResponseDTO(user)).thenReturn(userResponseDTO);

        // 2. ACT (Exécution de la méthode à tester)
        UserResponseDTO result = userService.createUser(userRequestDTO);

        // 3. ASSERT (Vérifications)
        assertNotNull(result, "Le DTO de retour ne doit pas être null");
        assertEquals(1L, result.getId());
        assertEquals("alice@example.com", result.getEmail());
        
        // On vérifie que la méthode save() du repository a bien été appelée exactement une fois
        verify(userRepository, times(1)).save(user);
        // On vérifie que le mot de passe a bien été encodé
        verify(passwordEncoder, times(1)).encode("secret123");
    }

    @Test
    @DisplayName("createUser - Échec : lève DuplicateResourceException si l'email existe déjà")
    void createUser_EmailAlreadyExists_ThrowsDuplicateResourceException() {
        // 1. ARRANGE : On simule que l'email est déjà présent en base
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

        // 2. ACT & 3. ASSERT : On vérifie que l'exception métier est bien déclenchée
        assertThrows(DuplicateResourceException.class, () -> userService.createUser(userRequestDTO),
                "Une DuplicateResourceException doit être levée si l'email est déjà pris");

        // Sécurité : on s'assure que save() n'a JAMAIS été appelé
        verify(userRepository, never()).save(any());
    }

    // =========================================================================
    // TESTS : READ USERS
    // =========================================================================

    @Test
    @DisplayName("getAllUsers - Retourne la liste complète des utilisateurs")
    void getAllUsers_ReturnsList() {
        // 1. ARRANGE : Simule une liste contenant 1 utilisateur
        when(userRepository.findAll()).thenReturn(List.of(user));
        when(userMapper.toResponseDTO(user)).thenReturn(userResponseDTO);

        // 2. ACT
        List<UserResponseDTO> users = userService.getAllUsers();

        // 3. ASSERT
        assertEquals(1, users.size());
        assertEquals("Alice Dupont", users.get(0).getName());
        verify(userRepository).findAll();
    }

    @Test
    @DisplayName("getUserById - Cas nominal : utilisateur trouvé")
    void getUserById_Success() {
        // 1. ARRANGE : L'utilisateur avec ID=1 est trouvé
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userMapper.toResponseDTO(user)).thenReturn(userResponseDTO);

        // 2. ACT
        UserResponseDTO result = userService.getUserById(1L);

        // 3. ASSERT
        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    @DisplayName("getUserById - Échec : utilisateur introuvable lève ResourceNotFoundException")
    void getUserById_NotFound_ThrowsResourceNotFoundException() {
        // 1. ARRANGE : L'ID 99 n'existe pas dans la base (Optional.empty)
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        // 2. ACT & 3. ASSERT
        assertThrows(ResourceNotFoundException.class, () -> userService.getUserById(99L),
                "Doit lever ResourceNotFoundException quand l'utilisateur n'existe pas");
    }

    // =========================================================================
    // TESTS : UPDATE USER
    // =========================================================================

    @Test
    @DisplayName("updateUser - Cas nominal : mise à jour du nom, email et mot de passe")
    void updateUser_Success() {
        // 1. ARRANGE
        UserRequestDTO updateDTO = new UserRequestDTO("Alice Modifiée", "alice.new@example.com", "newsecret");
        UserResponseDTO updatedResponse = new UserResponseDTO(1L, "Alice Modifiée", "alice.new@example.com");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        // Le nouvel email n'est pas encore pris
        when(userRepository.existsByEmail("alice.new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("newsecret")).thenReturn("encoded_newsecret");
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toResponseDTO(user)).thenReturn(updatedResponse);

        // 2. ACT
        UserResponseDTO result = userService.updateUser(1L, updateDTO);

        // 3. ASSERT
        assertNotNull(result);
        assertEquals("Alice Modifiée", result.getName());
        assertEquals("alice.new@example.com", result.getEmail());
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("updateUser - Échec : nouvel email déjà utilisé par un autre compte")
    void updateUser_EmailChangedAndTaken_ThrowsDuplicateResourceException() {
        // 1. ARRANGE : L'utilisateur change d'email mais cet email appartient déjà à quelqu'un d'autre
        UserRequestDTO updateDTO = new UserRequestDTO("Alice Modifiée", "taken@example.com", "newsecret");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

        // 2. ACT & 3. ASSERT
        assertThrows(DuplicateResourceException.class, () -> userService.updateUser(1L, updateDTO));
        // On s'assure qu'aucune sauvegarde n'est effectuée
        verify(userRepository, never()).save(any());
    }

    // =========================================================================
    // TESTS : DELETE USER
    // =========================================================================

    @Test
    @DisplayName("deleteUser - Cas nominal : suppression d'un utilisateur existant")
    void deleteUser_Success() {
        // 1. ARRANGE : L'utilisateur existe en base
        when(userRepository.existsById(1L)).thenReturn(true);

        // 2. ACT
        userService.deleteUser(1L);

        // 3. ASSERT : deleteById a bien été appelé
        verify(userRepository, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("deleteUser - Échec : tentative de suppression d'un ID inexistant")
    void deleteUser_NotFound_ThrowsResourceNotFoundException() {
        // 1. ARRANGE : L'utilisateur n'existe pas
        when(userRepository.existsById(99L)).thenReturn(false);

        // 2. ACT & 3. ASSERT
        assertThrows(ResourceNotFoundException.class, () -> userService.deleteUser(99L));
        // Aucune suppression ne doit être tentée
        verify(userRepository, never()).deleteById(any());
    }
}