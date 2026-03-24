# 📋 TODO - Projet 2 : Testez et améliorez une application existante

## Étape 1 - Analysez le code existant ✅

- [x] Explorer les starters code back-end et front-end.
- [x] Vérifier l'installation de l'environnement : Java 21, Maven 3.9.3, Angular 19 et Docker.
- [x] Lancer le back-end et confirmer la connexion DB.
- [x] Lancer le front-end et confirmer le démarrage.
- [x] Tester l'enregistrement d'un agent sur `http://localhost:4200/register`.
- [x] Vérifier l'insertion physique dans la table `user` (via phpMyAdmin/Docker).

## Étape 2 - Corrigez l'API d'authentification (Back-end) 🛠️

- [x] Identifier l'endpoint d'authentification `/api/login`.
- [x] Déboguer la méthode `login` dans `UserService.java` via des points d'arrêt.
- [x] Implémenter le service `JWTService.java` pour la délivrance du token.
- [x] Garantir que l'API retourne un token JWT valide en cas de succès.
- [x] Valider le fonctionnement de la route avec **Postman**.
- [ ] Respecter le découpage en couches et l'usage exclusif de **DTO** dans les controllers.

## Étape 3 - Implémentez l'interface d'authentification (Front-end) 🖥️

- [ ] Créer le composant `login` et sa route dédiée.
- [ ] Développer le formulaire (champs login et mot de passe).
- [ ] Créer le service Angular pour consommer l'API `/api/login`.
- [ ] Assurer la réception et la gestion du token d'authentification.
- [ ] Gérer les états de l'interface : chargement, erreurs serveur et succès.

## Étape 4 - Ajoutez de nouvelles fonctionnalités (Back-end) 🏗️

- [ ] Implémenter les APIs CRUD pour la gestion des étudiants (Ajout, Liste, Détail, Modif, Suppression).
- [ ] Sécuriser les APIs : accès réservé aux utilisateurs authentifiés via **Bearer Token**.
- [ ] Maintenir l'architecture : entrées/sorties via Controller, logique via Service, données via Repository.
- [ ] Tester chaque endpoint avec Postman.

## Étape 5 - Implémentez les écrans (Front-end) 🎨

- [ ] Développer les vues Angular pour toutes les opérations CRUD des étudiants.
- [ ] Sécuriser l'accès aux écrans à l'aide de **Guards** Angular.
- [ ] Créer les services Angular pour appeler les nouvelles APIs
