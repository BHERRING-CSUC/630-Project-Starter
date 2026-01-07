package com.sarasitha.budgettracker.repository;

import com.sarasitha.budgettracker.model.Income;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IncomeRepository extends JpaRepository<Income, Long> {
    List<Income> findByUserId(Long userId);
} 