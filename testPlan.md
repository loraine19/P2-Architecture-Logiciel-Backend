---

## 1. Back-end (Java 21 / Spring Boot 3 / JUnit 5 / Mockito)
**Objectif de couverture : > 80%**

### Priorité 1 : Tests Unitaires des Services (Simple)
L'objectif est de tester la logique métier de l'application en mockant la couche de persistance (`Repository`).

* **`src/test/java/.../service/StudentServiceTest.java`**
    * *Test 1 :* `getAllStudents()` (Entrée : Appel méthode / Sortie : Retourne une liste de `StudentDTO`, vérification que `studentRepository.findAll()` est appelé).
    * *Test 2 :* `getStudentById_Found()` (Entrée : ID existant / Sortie : Retourne le `StudentDTO` correspondant).
    * *Test 3 :* `getStudentById_NotFound()` (Entrée : ID inexistant / Sortie : Lève une `EntityNotFoundException`).
    * *Test 4 :* `createStudent()` (Entrée : Nouveau `StudentDTO` / Sortie : `studentRepository.save()` est appelé, retourne le DTO sauvegardé).
    * *Test 5 :* `deleteStudent()` (Entrée : ID existant / Sortie : `studentRepository.deleteById()` est appelé).

### Priorité 2 : Tests d'Intégration des Contrôleurs (Complexe)
Ces tests utilisent `@SpringBootTest` et `MockMvc` pour valider les routes HTTP, la sérialisation JSON et les filtres de sécurité.

* **`src/test/java/.../controller/StudentControllerIntegrationTest.java`**
    * *Test 1 :* Accès non autorisé (Entrée : Requête GET `/api/students` sans token / Sortie : Statut HTTP 401 Unauthorized).
    * *Test 2 :* Récupération de la liste (Entrée : Requête GET avec token valide / Sortie : Statut HTTP 200 OK + Flux JSON des étudiants).
    * *Test 3 :* Création invalide (Entrée : Requête POST avec JSON incomplet / Sortie : Statut HTTP 400 Bad Request, validation `@Valid` échoue).
* **`src/test/java/.../controller/AuthControllerIntegrationTest.java`**
    * *Test 1 :* Authentification réussie (Entrée : POST `/api/auth/login` avec bons identifiants / Sortie : Statut HTTP 200, présence du Cookie HttpOnly ou du Header JWT).

---

## 3. Scénarios de Test Bout en Bout (End-to-End)

Vérification des flux métiers complets (Test manuel ou via outil type Cypress/Selenium si requis par le périmètre).

- **Flux 1 : Cycle de vie d'un compte**
    - (Entrées : Inscription -> Connexion -> Déconnexion) / (Sorties : Accès accordé puis révoqué).
- **Flux 2 : Gestion Complète Étudiant**
    - (Entrées : Login admin -> Création étudiant X -> Modification étudiant X -> Suppression étudiant X) / (Sorties : La base de données reflète les états successifs sans erreur d'intégrité).
