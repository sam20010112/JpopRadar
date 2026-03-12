package com.jpopradar.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "concerts")
public class Concert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String artist;
    private String venue;
    private String city;
    private LocalDate concertDate;
    private String ticketUrl;

    public Concert() {}

    public Concert(String artist, String venue, String city, LocalDate concertDate, String ticketUrl) {
        this.artist = artist;
        this.venue = venue;
        this.city = city;
        this.concertDate = concertDate;
        this.ticketUrl = ticketUrl;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getArtist() { return artist; }
    public void setArtist(String artist) { this.artist = artist; }

    public String getVenue() { return venue; }
    public void setVenue(String venue) { this.venue = venue; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public LocalDate getConcertDate() { return concertDate; }
    public void setConcertDate(LocalDate concertDate) { this.concertDate = concertDate; }

    public String getTicketUrl() { return ticketUrl; }
    public void setTicketUrl(String ticketUrl) { this.ticketUrl = ticketUrl; }
}
