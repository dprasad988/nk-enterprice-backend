package com.hardwarepos.repository;

import com.hardwarepos.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, Long> {
    List<UserSession> findAllByOrderByLoginTimeDesc();
    List<UserSession> findByUserIdOrderByLoginTimeDesc(Long userId);
    java.util.Optional<UserSession> findFirstByUserIdAndStatusOrderByLoginTimeDesc(Long userId, String status);
}
