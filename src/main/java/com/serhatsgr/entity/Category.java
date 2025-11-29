package com.serhatsgr.entity;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "categories")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @ManyToMany(mappedBy = "categories")
    private Set<Film> films = new HashSet<>();

    // === Constructors ===
    public Category() {
    }

    public Category(Long id, String name, String description, Set<Film> films) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.films = films;
    }



    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Set<Film> getFilms() {
        return films;
    }

    // === Setters ===
    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setFilms(Set<Film> films) {
        this.films = films;
    }

    // === Helper Methods ===
    public void addFilm(Film film) {
        if (film == null) return;
        if (this.films.contains(film)) return;
        this.films.add(film);
        film.getCategories().add(this);
    }

    public void removeFilm(Film film) {
        if (film == null) return;
        if (!this.films.contains(film)) return;
        this.films.remove(film);
        film.getCategories().remove(this);
    }

    // === toString ===
    @Override
    public String toString() {
        return "Category{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}
