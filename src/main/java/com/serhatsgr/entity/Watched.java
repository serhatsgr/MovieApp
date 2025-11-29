package com.serhatsgr.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "watched_movies")
public class Watched {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "film_id", nullable = false)
    private Film film;

    private LocalDateTime createdAt;

    // --- Constructors ---

    public Watched() {
        // JPA için boş constructor
    }

    public Watched(Long id, User user, Film film, LocalDateTime createdAt) {
        this.id = id;
        this.user = user;
        this.film = film;
        this.createdAt = createdAt;
    }

    // --- Builder ---

    public static class Builder {
        private Long id;
        private User user;
        private Film film;
        private LocalDateTime createdAt;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder user(User user) {
            this.user = user;
            return this;
        }

        public Builder film(Film film) {
            this.film = film;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Watched build() {
            return new Watched(id, user, film, createdAt);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    // --- Getters & Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Film getFilm() { return film; }
    public void setFilm(Film film) { this.film = film; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    // --- Lifecycle ---

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // --- equals & hashCode ---

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Watched)) return false;
        Watched watched = (Watched) o;
        return Objects.equals(id, watched.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    // --- toString ---

    @Override
    public String toString() {
        return "Watched{" +
                "id=" + id +
                ", createdAt=" + createdAt +
                '}';
    }
}
