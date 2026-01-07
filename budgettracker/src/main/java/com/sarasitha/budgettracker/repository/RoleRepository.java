package com.sarasitha.budgettracker.repository;

import com.sarasitha.budgettracker.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {
}
