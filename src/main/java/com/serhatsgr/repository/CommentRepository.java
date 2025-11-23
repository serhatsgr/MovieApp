package com.serhatsgr.repository;

import com.serhatsgr.entity.Comment;
import com.serhatsgr.entity.Film;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findAllByFilm(Film film);
}
