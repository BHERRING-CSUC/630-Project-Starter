package com.sarasitha.budgettracker.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
public class Income {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    private double amount;
    private LocalDate date;
    private String source;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    public Income() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
} 