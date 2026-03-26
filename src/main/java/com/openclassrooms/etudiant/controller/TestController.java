package com.openclassrooms.etudiant.controller;

import com.openclassrooms.etudiant.dto.TestReturnDTO;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class TestController {

    /* TEST ROUTE */
    @GetMapping("/test")
    public TestReturnDTO testRoute() {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        // get username
        String username = auth.getName();
        System.out.println("JWT Filter: Extracted username: " + username);

        // return
        return new TestReturnDTO("Hello, " + username + "! This is a protected route.");
    }
}