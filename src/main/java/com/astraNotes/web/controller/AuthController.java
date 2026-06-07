package com.astraNotes.web.controller;

import com.astraNotes.web.service.AuthService;
import com.astraNotes.web.service.DemoUser;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/login")
    public String login(Model model) {
        model.addAttribute("users", authService.users());
        return "login";
    }

    @PostMapping("/login")
    public String authenticate(@RequestParam String userId, @RequestParam String password, HttpSession session, Model model) {
        return authService.authenticate(userId, password)
                .map(user -> {
                    session.setAttribute(AuthService.SESSION_USER_ID, user.id());
                    return "redirect:/notes";
                })
                .orElseGet(() -> {
                    model.addAttribute("users", authService.users());
                    model.addAttribute("error", "Invalid demo user or password.");
                    return "login";
                });
    }

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }
}
