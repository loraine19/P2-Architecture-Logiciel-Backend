# Plan de Test - Etudiant Backend

## 1. Back-end (Java 21 / Spring Boot 3 / JUnit 5 / Mockito)

**Objectif de couverture JaCoCo : > 80%**

### Phase 1 : Tests Unitaires Purs (Isolation stricte)

Objectif : Valider la logique métier et le routage de manière isolée, rapide, sans charger le contexte Spring ni la sécurité.

- **Domaine User (Priorité Absolue)**
    - `UserServiceTest` : Mock du Repository. Vérification de la logique d'inscription et de connexion (retours attendus et levée des `IllegalArgumentException`).
    - `UserControllerTest` : Utilisation de `StandaloneSetup`. Validation de la sérialisation JSON, des codes HTTP (200, 400) et des annotations de validation (`@Valid`).
- **Domaine Student**
    - `StudentServiceTest` : Mock du Repository. Vérification des opérations CRUD et levées d'exceptions (`EntityNotFoundException`).
    - `StudentControllerTest` : Utilisation de `StandaloneSetup`. Validation des routes et DTOs indépendamment des droits d'accès.

### Phase 2 : Tests d'Intégration (Sécurité & Base de Données)

Objectif : Utiliser `@SpringBootTest` et `MockMvc` pour valider l'interconnexion des couches, particulièrement les filtres de sécurité.

- **Flux Sécurité (`UserIntegrationTest.java`)**
    - Inscription complète (Base de données H2/Test).
    - Login avec succès -> Génération et validation du format JWT.
    - Accès à une route protégée sans token -> Statut HTTP 401 Unauthorized.
- **Flux Étudiant (`StudentIntegrationTest.java`)**
    - Création et récupération d'un étudiant en base via les requêtes HTTP avec un token JWT valide.

---

## 2. Scénarios de Test Bout en Bout (End-to-End)

Vérification des flux métiers complets (via Postman ou tests E2E automatisés).

- **Flux 1 : Cycle de vie d'un compte**
    - (Entrées : Inscription -> Connexion -> Déconnexion) / (Sorties : Accès accordé puis révoqué, token blacklisté si applicable).
- **Flux 2 : Gestion Complète Étudiant**
    - (Entrées : Login -> Création étudiant X -> Modification étudiant X -> Suppression étudiant X) / (Sorties : La base de données reflète les états successifs sans erreur d'intégrité).
