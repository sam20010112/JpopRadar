package com.jpopradar.repository;

import com.jpopradar.model.Concert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface ConcertRepository extends JpaRepository<Concert, Long> {
    List<Concert> findByCityIgnoreCase(String city);
    List<Concert> findByArtistIgnoreCase(String artist);
    List<Concert> findByConcertDateAfterOrderByConcertDateAsc(LocalDate concertDate);
}
