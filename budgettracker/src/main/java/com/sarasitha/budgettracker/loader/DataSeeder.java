package com.sarasitha.budgettracker.loader;

import com.sarasitha.budgettracker.model.Category;
import com.sarasitha.budgettracker.model.Income;
import com.sarasitha.budgettracker.model.Transaction;
import com.sarasitha.budgettracker.model.User;
import com.sarasitha.budgettracker.repository.IncomeRepository;
import com.sarasitha.budgettracker.repository.TransactionRepository;
import com.sarasitha.budgettracker.service.UserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Arrays;

@Component
public class DataSeeder implements CommandLineRunner {

    private final UserService userService;
    private final TransactionRepository transactionRepository;
    private final IncomeRepository incomeRepository;

    public DataSeeder(UserService userService, TransactionRepository transactionRepository, IncomeRepository incomeRepository) {
        this.userService = userService;
        this.transactionRepository = transactionRepository;
        this.incomeRepository = incomeRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        if (userService.findByUsername("test") == null) {
            System.out.println("Seeding database with test user and sample data...");

            // Create User
            User user = new User();
            user.setUsername("test");
            user.setPassword("test");
            user.setPasswordConfirm("test"); // Required for validation/save logic
            userService.save(user);

            // Re-fetch to get ID/roles (if any transformation happens on save)
            User testUser = userService.findByUsername("test");

            // Seed Income
            seedIncome(testUser);

            // Seed Transactions
            seedTransactions(testUser);
            
            System.out.println("Database seeding completed.");
        }
    }

    private void seedIncome(User user) {
        LocalDate today = LocalDate.now();

        // Income for this month
        createIncome(user, 5000.00, today.withDayOfMonth(1), "Monthly Salary", "Tech Corp Inc.");
        createIncome(user, 800.00, today.minusDays(5), "Freelance Project", "Upwork Client");

        // Income for last month
        createIncome(user, 5000.00, today.minusMonths(1).withDayOfMonth(1), "Monthly Salary", "Tech Corp Inc.");
        
        // Income for two months ago
        createIncome(user, 5000.00, today.minusMonths(2).withDayOfMonth(1), "Monthly Salary", "Tech Corp Inc.");
        createIncome(user, 1500.00, today.minusMonths(2).withDayOfMonth(15), "Performance Bonus", "Tech Corp Inc.");
    }

    private void createIncome(User user, double amount, LocalDate date, String title, String source) {
        Income income = new Income();
        income.setAmount(amount);
        income.setDate(date);
        income.setTitle(title);
        income.setSource(source);
        income.setUser(user);
        incomeRepository.save(income);
    }

    private void seedTransactions(User user) {
        LocalDate today = LocalDate.now();
        LocalDate lastMonth = today.minusMonths(1);

        // --- Fixed Expenses (Rent, Utilities) ---
        // This month
        createTransaction(user, "Apartment Rent", 1800.00, Category.RENT, today.withDayOfMonth(1), "Monthly rent payment");
        createTransaction(user, "Electric Bill", 124.50, Category.UTILITIES, today.withDayOfMonth(5), "Winter heating cost");
        createTransaction(user, "Internet", 65.00, Category.UTILITIES, today.withDayOfMonth(8), "Fiber optic");

        // Last month
        createTransaction(user, "Apartment Rent", 1800.00, Category.RENT, lastMonth.withDayOfMonth(1), "Monthly rent payment");
        createTransaction(user, "Electric Bill", 110.20, Category.UTILITIES, lastMonth.withDayOfMonth(5), "");

        // --- Food & Dining ---
        createTransaction(user, "Whole Foods Market", 186.45, Category.FOOD, today.minusDays(2), "Weekly groceries");
        createTransaction(user, "Starbucks", 5.75, Category.FOOD, today.minusDays(1), "Morning coffee");
        createTransaction(user, "Trader Joe's", 92.30, Category.FOOD, today.minusDays(9), "Groceries and snacks");
        createTransaction(user, "Local Pizza Place", 45.00, Category.FOOD, today.minusDays(4), "Dinner with friends");
        createTransaction(user, "Sushi Lunch", 28.50, Category.FOOD, today.minusDays(0), "Treat yourself");
        createTransaction(user, "Safeway", 65.20, Category.FOOD, lastMonth.withDayOfMonth(15), "Groceries");

        // --- Transport ---
        createTransaction(user, "Shell Gas Station", 45.00, Category.TRANSPORT, today.minusDays(3), "Full tank");
        createTransaction(user, "Uber to Airport", 32.50, Category.TRANSPORT, lastMonth.withDayOfMonth(20), "Business trip");
        createTransaction(user, "Car Insurance", 110.00, Category.TRANSPORT, today.withDayOfMonth(12), "Monthly premium");

        // --- Entertainment ---
        createTransaction(user, "Netflix", 15.99, Category.ENTERTAINMENT, today.withDayOfMonth(15), "Subscription");
        createTransaction(user, "Spotify", 9.99, Category.ENTERTAINMENT, today.withDayOfMonth(14), "Music");
        createTransaction(user, "Cinema: Dune 2", 35.00, Category.ENTERTAINMENT, today.minusDays(6), "Tickets and popcorn");
        createTransaction(user, "Concert Tickets", 150.00, Category.ENTERTAINMENT, lastMonth.withDayOfMonth(25), "Rock band tour");

        // --- Other / Shopping ---
        createTransaction(user, "Amazon Purchase", 42.99, Category.OTHER, today.minusDays(10), "Home visuals decoration");
        createTransaction(user, "New Headphones", 250.00, Category.OTHER, lastMonth.withDayOfMonth(10), "Tech upgrade");
        createTransaction(user, "Gym Membership", 40.00, Category.OTHER, today.withDayOfMonth(1), "Fitness");
    }

    private void createTransaction(User user, String title, double amount, Category category, LocalDate date, String description) {
        Transaction t = new Transaction();
        t.setTitle(title);
        t.setAmount(amount);
        t.setCategory(category);
        t.setDate(date);
        t.setDescription(description);
        t.setUser(user);
        transactionRepository.save(t);
    }
}
