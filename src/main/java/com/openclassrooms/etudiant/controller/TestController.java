package com.openclassrooms.etudiant.controller;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class TestController {

    /* Définition de la route GET /api/test */
    @GetMapping("/test")
    public String testRoute() {

        /*
         * 1. On interroge la mémoire de Spring pour récupérer le badge validé par ton
         * filtre
         */
        var auth = SecurityContextHolder.getContext().getAuthentication();

        /* 2. On extrait le nom de l'utilisateur (le login) */
        String username = auth.getName();

        /* 3. Ta preuve d'exécution dans le terminal du serveur */
        System.out.println("=== CONTROLLER : Route /api/test appelée par -> " + username + " ===");

        /* 4. On renvoie une vraie réponse HTTP 200 OK à Postman */
        return "Preuve de connexion : tu as passé la sécurité en tant que '" + username + "'";
    }
}