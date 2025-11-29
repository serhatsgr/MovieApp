package com.serhatsgr.dto;

import java.util.List;

public class DtoCategory {

    private Long id;
    private String name;
    private String description;
    private List<DtoFilmSummary> filmSummaries;

    public DtoCategory() {
    }

    public DtoCategory(Long id, String name, String description, List<DtoFilmSummary> filmSummaries) {
        this.name = name;
        this.description = description;
        this.filmSummaries = filmSummaries;
        this.id=id;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<DtoFilmSummary> getFilmSummaries() {
        return filmSummaries;
    }

    public void setFilmSummaries(List<DtoFilmSummary> filmSummaries) {
        this.filmSummaries = filmSummaries;
    }
}
