package com.sarasitha.budgettracker.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private double amount;
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    private Category category;

    private String description;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    // Getters & setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
