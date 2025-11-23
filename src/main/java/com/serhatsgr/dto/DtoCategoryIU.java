package com.serhatsgr.dto;

import com.serhatsgr.entity.Film;

import java.util.List;

public class DtoCategoryIU {

    private String name;

    private String description;



    public DtoCategoryIU(String name, String description) {
        this.name = name;
        this.description = description;
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


}
