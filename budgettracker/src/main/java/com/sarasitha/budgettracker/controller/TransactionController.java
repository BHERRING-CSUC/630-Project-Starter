package com.sarasitha.budgettracker.controller;

import com.sarasitha.budgettracker.model.User;
import com.sarasitha.budgettracker.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import com.sarasitha.budgettracker.model.Transaction;
import com.sarasitha.budgettracker.model.Category;
import com.sarasitha.budgettracker.repository.TransactionRepository;
import com.sarasitha.budgettracker.model.Income;
import com.sarasitha.budgettracker.repository.IncomeRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.HashMap;
import org.springframework.http.ResponseEntity;

@Controller
public class TransactionController {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private IncomeRepository incomeRepository;

    @Autowired
    private UserService userService;

    private User getLoggedInUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentPrincipalName = authentication.getName();
        return userService.findByUsername(currentPrincipalName);
    }

    @GetMapping("/")
    public String home(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            return "landing";
        }

        User user = getLoggedInUser();
        List<Transaction> transactions = transactionRepository.findByUserId(user.getId());
        YearMonth currentMonth = YearMonth.now();
        double monthlyTotal = transactions.stream()
                .filter(t -> t.getDate() != null && YearMonth.from(t.getDate()).equals(currentMonth))
                .mapToDouble(Transaction::getAmount)
                .sum();
        List<Income> incomes = incomeRepository.findByUserId(user.getId());
        double totalEarnings = incomes.stream()
                .filter(i -> i.getDate() != null && YearMonth.from(i.getDate()).equals(currentMonth))
                .mapToDouble(Income::getAmount)
                .sum();
        double netCashflow = totalEarnings - monthlyTotal;
        model.addAttribute("transaction", new Transaction());
        model.addAttribute("income", new Income());
        model.addAttribute("incomeList", incomes);
        model.addAttribute("transactions", transactions);
        model.addAttribute("categories", Category.values());
        model.addAttribute("monthlyTotal", monthlyTotal);
        model.addAttribute("totalEarnings", totalEarnings);
        model.addAttribute("netCashflow", netCashflow);
        // Category-wise totals
        Map<Category, Double> categoryTotals = new HashMap<>();
        for (Transaction t : transactions) {
            if (t.getCategory() != null) {
                categoryTotals.put(t.getCategory(), categoryTotals.getOrDefault(t.getCategory(), 0.0) + t.getAmount());
            }
        }
        java.util.List<String> categoryLabels = categoryTotals.keySet().stream().map(Enum::name).toList();
        java.util.List<Double> categoryValues = categoryTotals.values().stream().toList();
        model.addAttribute("categoryLabels", categoryLabels);
        model.addAttribute("categoryValues", categoryValues);
        java.util.List<String> categoryGradients = java.util.List.of(
            "#6366f1", "#10b981", "#f59e42", "#ef4444", "#eab308",
            "#a78bfa", "#f472b6", "#38bdf8", "#34d399", "#f87171"
        );
        model.addAttribute("categoryGradients", categoryGradients);
        return "home";
    }

    @PostMapping("/add")
    public String addTransaction(@ModelAttribute Transaction transaction) {
        if (transaction.getDate() == null) {
            transaction.setDate(LocalDate.now());
        }
        transaction.setUser(getLoggedInUser());
        transactionRepository.save(transaction);
        return "redirect:/";
    }

    @GetMapping("/delete/{id}")
    public String deleteTransaction(@PathVariable Long id) {
        Transaction t = transactionRepository.findById(id).orElse(null);
        if (t != null && t.getUser().getId().equals(getLoggedInUser().getId())) {
             transactionRepository.deleteById(id);
        }
        return "redirect:/";
    }

    @GetMapping("/export")
    public void exportToCSV(HttpServletResponse response) throws IOException {
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=transactions.csv");
        User user = getLoggedInUser();
        List<Transaction> transactions = transactionRepository.findByUserId(user.getId());
        try (PrintWriter writer = response.getWriter()) {
            writer.println("Title,Amount,Date,Category,Description");
            for (Transaction t : transactions) {
                writer.printf("\"%s\",%s,%s,%s,\"%s\"\n",
                        t.getTitle() != null ? t.getTitle().replace("\"", "\"\"") : "",
                        t.getAmount(),
                        t.getDate() != null ? t.getDate() : "",
                        t.getCategory() != null ? t.getCategory() : "",
                        t.getDescription() != null ? t.getDescription().replace("\"", "\"\"") : "");
            }
        }
    }

    @PostMapping("/income")
    public String addIncome(@ModelAttribute Income income) {
        if (income.getDate() == null) {
            income.setDate(LocalDate.now());
        }
        income.setUser(getLoggedInUser());
        incomeRepository.save(income);
        return "redirect:/";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        User user = getLoggedInUser();
        List<Transaction> transactions = transactionRepository.findByUserId(user.getId());
        YearMonth currentMonth = YearMonth.now();
        double monthlyTotal = transactions.stream()
                .filter(t -> t.getDate() != null && YearMonth.from(t.getDate()).equals(currentMonth))
                .mapToDouble(Transaction::getAmount)
                .sum();
        List<Income> incomes = incomeRepository.findByUserId(user.getId());
        double totalEarnings = incomes.stream()
                .filter(i -> i.getDate() != null && YearMonth.from(i.getDate()).equals(currentMonth))
                .mapToDouble(Income::getAmount)
                .sum();
        double netCashflow = totalEarnings - monthlyTotal;
        model.addAttribute("transactions", transactions);
        model.addAttribute("monthlyTotal", monthlyTotal);
        model.addAttribute("totalEarnings", totalEarnings);
        model.addAttribute("netCashflow", netCashflow);
        return "dashboard";
    }


    @GetMapping("/transaction/{id}")
    @ResponseBody
    public ResponseEntity<Transaction> getTransaction(@PathVariable Long id) {
        return transactionRepository.findById(id)
                .filter(t -> t.getUser().getId().equals(getLoggedInUser().getId()))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/transaction/edit")
    public String editTransaction(@ModelAttribute Transaction transaction) {
        Transaction existing = transactionRepository.findById(transaction.getId()).orElse(null);
        if (existing != null && existing.getUser().getId().equals(getLoggedInUser().getId())) {
            transaction.setUser(existing.getUser());
            transactionRepository.save(transaction);
        }
        return "redirect:/";
    }

    @PostMapping("/transaction/delete/{id}")
    @ResponseBody
    public ResponseEntity<?> deleteTransactionAjax(@PathVariable Long id) {
        Transaction t = transactionRepository.findById(id).orElse(null);
        if (t != null && t.getUser().getId().equals(getLoggedInUser().getId())) {
            transactionRepository.deleteById(id);
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/income/{id}")
    @ResponseBody
    public ResponseEntity<Income> getIncome(@PathVariable Long id) {
        return incomeRepository.findById(id)
                .filter(i -> i.getUser().getId().equals(getLoggedInUser().getId()))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/income/edit")
    public String editIncome(@ModelAttribute Income income) {
        Income existing = incomeRepository.findById(income.getId()).orElse(null);
        if (existing != null && existing.getUser().getId().equals(getLoggedInUser().getId())) {
            income.setUser(existing.getUser());
            incomeRepository.save(income);
        }
        return "redirect:/";
    }

    @PostMapping("/income/delete/{id}")
    @ResponseBody
    public ResponseEntity<?> deleteIncomeAjax(@PathVariable Long id) {
        Income i = incomeRepository.findById(id).orElse(null);
        if (i != null && i.getUser().getId().equals(getLoggedInUser().getId())) {
            incomeRepository.deleteById(id);
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
