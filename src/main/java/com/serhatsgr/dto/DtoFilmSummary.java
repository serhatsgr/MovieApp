package com.serhatsgr.dto;

public class DtoFilmSummary {
    private Long id;
    private String title;
    private String posterUrl;

    public DtoFilmSummary(Long id, String title, String posterUrl) {
        this.title = title;
        this.posterUrl = posterUrl;
        this.id=id;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getPosterUrl() {
        return posterUrl;
    }

    public void setPosterUrl(String posterUrl) {
        this.posterUrl = posterUrl;
    }
}
