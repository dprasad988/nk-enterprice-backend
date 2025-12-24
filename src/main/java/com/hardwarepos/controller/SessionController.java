package com.hardwarepos.controller;

import com.hardwarepos.entity.UserSession;
import com.hardwarepos.repository.UserSessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/sessions")
public class SessionController {

    @Autowired
    private UserSessionRepository userSessionRepository;

    @GetMapping
    public List<UserSession> getAllSessions() {
        return userSessionRepository.findAllByOrderByLoginTimeDesc();
    }
}
