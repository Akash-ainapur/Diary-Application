package com.diaryapp.diaryapp.controller;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.diaryapp.diaryapp.model.DiaryEntry;
import com.diaryapp.diaryapp.model.User;
import com.diaryapp.diaryapp.repository.DiaryRepository;
import com.diaryapp.diaryapp.repository.UserRepository;
import com.diaryapp.diaryapp.service.DiaryService;

@Controller
public class WebController {

    @Autowired
    DiaryRepository diaryRepo;

    @Autowired
    UserRepository userRepo;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    DiaryService diaryService;

    // ── Auth ────────────────────────────────────────────────────────────────────

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    @PostMapping("/register")
    public String register(@RequestParam String username, @RequestParam String password) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        userRepo.save(user);
        return "redirect:/login";
    }

    // ── Dashboard ───────────────────────────────────────────────────────────────

    @GetMapping("/dashboard")
    public String dashboard(Model model, Principal principal) {
        List<DiaryEntry> diaries = diaryRepo.findByUsername(principal.getName());
        model.addAttribute("diaries", diaries);
        return "dashboard";
    }

    // ── Create diary ────────────────────────────────────────────────────────────

    @GetMapping("/new")
    public String newDiary() {
        return "new-diary";
    }

    @PostMapping("/save")
    public String saveDiary(@RequestParam String title, @RequestParam String content, Principal principal) {
        DiaryEntry entry = new DiaryEntry();
        entry.setTitle(title);
        entry.setContent(content);
        entry.setUsername(principal.getName());
        diaryRepo.save(entry);
        return "redirect:/dashboard";
    }

    // ── View / Edit / Update / Delete ───────────────────────────────────────────

    @GetMapping("/view/{id}")
    public String viewDiary(@PathVariable String id, Model model) {
        Optional<DiaryEntry> entry = diaryRepo.findById(id);
        entry.ifPresent(e -> model.addAttribute("diary", e));
        return "view-diary";
    }

    @GetMapping("/edit/{id}")
    public String editDiary(@PathVariable String id, Model model) {
        Optional<DiaryEntry> entry = diaryRepo.findById(id);
        entry.ifPresent(e -> model.addAttribute("diary", e));
        return "edit-diary";
    }

    @PostMapping("/update/{id}")
    public String updateDiary(@PathVariable String id, @RequestParam String title, @RequestParam String content) {
        Optional<DiaryEntry> optional = diaryRepo.findById(id);
        if (optional.isPresent()) {
            DiaryEntry entry = optional.get();
            entry.setTitle(title);
            entry.setContent(content);
            entry.setSentiment(null); // Reset sentiment if content changed
            entry.setSummary(null);
            diaryRepo.save(entry);
        }
        return "redirect:/dashboard";
    }

    @PostMapping("/delete/{id}")
    public String deleteDiary(@PathVariable String id) {
        diaryRepo.deleteById(id);
        return "redirect:/dashboard";
    }

    // ── Analyse endpoint (on-demand per entry) ──────────────────────────────────

    @PostMapping("/analyze/{id}")
    public String analyzeDiary(@PathVariable String id) {
        Optional<DiaryEntry> optional = diaryRepo.findById(id);
        if (optional.isPresent()) {
            DiaryEntry entry = optional.get();
            String detectedSentiment = fetchSentiment(entry.getContent());
            entry.setSentiment(detectedSentiment);
            
            if ("Positive".equalsIgnoreCase(detectedSentiment)) {
                entry.setSummary("It's wonderful that you're having a positive experience! Keep embracing the good moments and let them fuel your day.");
            } else if ("Negative".equalsIgnoreCase(detectedSentiment)) {
                entry.setSummary("It sounds like things are a bit tough right now. Take a deep breath, be kind to yourself, and remember it's okay to have bad days.");
            } else {
                entry.setSummary("Thank you for sharing your thoughts today.");
            }
            
            diaryRepo.save(entry);
        }
        return "redirect:/dashboard";
    }

    // ── Helpers ─────────────────────────────────────────────────────────────────

    /**
     * Calls the Python sentiment micro-service and returns a capitalized label
     * (e.g. "Positive" / "Negative"). Falls back to "Unknown" if the service is
     * unavailable.
     */
    private String fetchSentiment(String text) {
        try {
            String raw = diaryService.analyzeSentiment(text);
            if (raw == null || raw.isEmpty()) return "Unknown";
            return Character.toUpperCase(raw.charAt(0)) + raw.substring(1).toLowerCase();
        } catch (Exception e) {
            return "Unknown";
        }
    }
}
