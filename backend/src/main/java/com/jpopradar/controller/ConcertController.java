package com.jpopradar.controller;

import com.jpopradar.model.Concert;
import com.jpopradar.service.ConcertService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.List;

@RestController
@RequestMapping("/api/concerts")
@CrossOrigin(origins = "*")
public class ConcertController {

    private final ConcertService concertService;

    @Value("${app.concerts-scan-url:}")
    private String concertsScanUrl;

    public ConcertController(ConcertService concertService) {
        this.concertService = concertService;
    }

    @GetMapping
    public List<Concert> getAllConcerts() {
        return concertService.getAllConcerts();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Concert> getConcertById(@PathVariable Long id) {
        return concertService.getConcertById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Concert> createConcert(@RequestBody Concert concert) {
        return ResponseEntity.status(HttpStatus.CREATED).body(concertService.createConcert(concert));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Concert> updateConcert(@PathVariable Long id, @RequestBody Concert concert) {
        return concertService.updateConcert(id, concert)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteConcert(@PathVariable Long id) {
        return concertService.deleteConcert(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    @GetMapping("/upcoming")
    public List<Concert> getUpcomingConcerts() {
        return concertService.getUpcomingConcerts();
    }

    @GetMapping("/by-city")
    public List<Concert> getConcertsByCity(@RequestParam String city) {
        return concertService.getConcertsByCity(city);
    }

    @GetMapping("/by-artist")
    public List<Concert> getConcertsByArtist(@RequestParam String artist) {
        return concertService.getConcertsByArtist(artist);
    }

    @GetMapping("/scan")
    public void getScan(HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        if (concertsScanUrl != null && !concertsScanUrl.isBlank()) {
            try {
                byte[] json = WebClient.create()
                    .get()
                    .uri(concertsScanUrl)
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .timeout(Duration.ofSeconds(10))
                    .block();
                if (json != null) {
                    response.getOutputStream().write(json);
                    return;
                }
            } catch (Exception e) {
                System.err.println("[ConcertController] S3 fetch failed, falling back to classpath: " + e.getMessage());
            }
        }

        new ClassPathResource("concertsScan.json").getInputStream().transferTo(response.getOutputStream());
    }
}
