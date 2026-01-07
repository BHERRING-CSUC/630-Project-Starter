package com.sarasitha.budgettracker.repository;

import com.sarasitha.budgettracker.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    User findByUsername(String username);
}
