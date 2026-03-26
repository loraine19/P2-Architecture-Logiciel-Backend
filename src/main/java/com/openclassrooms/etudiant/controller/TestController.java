package com.openclassrooms.etudiant.controller;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/api")
public class TestController {

    /* TEST ROUTE */
    @GetMapping("/test")
    public String testRoute() {

        // get authentication call context
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        // get username
        String username = auth.getName();
        System.out.println("JWT Filter: Extracted username: " + username);

        // return
        return "Hello, " + username + "! This is a protected route.";
    }
}