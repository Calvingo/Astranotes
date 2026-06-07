package com.astraNotes.web.service;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AuthService {
    public static final String SESSION_USER_ID = "astraNotesUserId";

    private final List<DemoUser> users = List.of(
            new DemoUser("alex", "Alex Chen", "alex123"),
            new DemoUser("morgan", "Morgan Lee", "morgan123"),
            new DemoUser("taylor", "Taylor Kim", "taylor123")
    );

    public List<DemoUser> users() {
        return users;
    }

    public Optional<DemoUser> findById(String id) {
        return users.stream().filter(user -> user.id().equals(id)).findFirst();
    }

    public Optional<DemoUser> authenticate(String userId, String password) {
        return findById(userId)
                .filter(user -> user.password().equals(password));
    }
}
