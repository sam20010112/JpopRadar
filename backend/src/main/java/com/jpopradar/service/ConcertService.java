package com.jpopradar.service;

import com.jpopradar.model.Concert;
import com.jpopradar.repository.ConcertRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class ConcertService {

    private final ConcertRepository concertRepository;

    public ConcertService(ConcertRepository concertRepository) {
        this.concertRepository = concertRepository;
    }

    public List<Concert> getAllConcerts() {
        return concertRepository.findAll();
    }

    public Optional<Concert> getConcertById(Long id) {
        return concertRepository.findById(id);
    }

    public Concert createConcert(Concert concert) {
        return concertRepository.save(concert);
    }

    public Optional<Concert> updateConcert(Long id, Concert updated) {
        return concertRepository.findById(id).map(concert -> {
            concert.setArtist(updated.getArtist());
            concert.setVenue(updated.getVenue());
            concert.setCity(updated.getCity());
            concert.setConcertDate(updated.getConcertDate());
            concert.setTicketUrl(updated.getTicketUrl());
            return concertRepository.save(concert);
        });
    }

    public boolean deleteConcert(Long id) {
        if (concertRepository.existsById(id)) {
            concertRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public List<Concert> getConcertsByCity(String city) {
        return concertRepository.findByCityIgnoreCase(city);
    }

    public List<Concert> getConcertsByArtist(String artist) {
        return concertRepository.findByArtistIgnoreCase(artist);
    }

    public List<Concert> getUpcomingConcerts() {
        return concertRepository.findByConcertDateAfterOrderByConcertDateAsc(LocalDate.now().minusDays(1));
    }
}
