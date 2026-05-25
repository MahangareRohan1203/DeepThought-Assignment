package com.example.demo.site;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sites")
@RequiredArgsConstructor
public class SiteController {

    private final SiteRepository siteRepository;

    @PostMapping
    public ResponseEntity<Site> createSite(@Valid @RequestBody Site site) {
        return ResponseEntity.ok(siteRepository.save(site));
    }

    @GetMapping
    public ResponseEntity<java.util.List<Site>> getAllSites() {
        return ResponseEntity.ok(siteRepository.findAll());
    }
}
