package com.serhatsgr.service.Impl;

import com.serhatsgr.dto.*;
import com.serhatsgr.entity.Comment;
import com.serhatsgr.entity.Film;
import com.serhatsgr.entity.User;
import com.serhatsgr.exception.BaseException;
import com.serhatsgr.exception.ErrorMessage;
import com.serhatsgr.exception.MessageType;
import com.serhatsgr.repository.CommentRepository;
import com.serhatsgr.repository.FilmRepository;
import com.serhatsgr.repository.UserRepository;
import com.serhatsgr.service.ICommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements ICommentService {

    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final FilmRepository filmRepository;
    private final UserService service;

    @Override
    public CommentResponse createComment(CreateCommentRequest request) {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new BaseException(new ErrorMessage(
                            MessageType.RESOURCE_NOT_FOUND, "Kullanıcı bulunamadı: " + username
                    )));

            Film film = filmRepository.findById(request.filmId())
                    .orElseThrow(() -> new BaseException(new ErrorMessage(
                            MessageType.RESOURCE_NOT_FOUND, "Film bulunamadı: " + request.filmId()
                    )));

            Comment comment = Comment.builder()
                    .content(request.content())
                    .user(user)
                    .film(film)
                    .build();

            Comment savedComment = commentRepository.save(comment);

            return mapToResponse(savedComment);

        } catch (BaseException e) {
            throw e;
        } catch (Exception e) {
            throw new BaseException(new ErrorMessage(
                    MessageType.INTERNAL_ERROR, "Yorum oluşturulurken hata oluştu: " + e.getMessage()
            ));
        }
    }

    @Override
    public CommentResponse updateComment(Long commentId, UpdateCommentRequest request, String username) {
        try {
            Comment comment = commentRepository.findById(commentId)
                    .orElseThrow(() -> new BaseException(new ErrorMessage(
                            MessageType.RESOURCE_NOT_FOUND, "Yorum bulunamadı: " + commentId
                    )));

            if (!comment.getUser().getUsername().equals(username)) {
                throw new BaseException(new ErrorMessage(
                        MessageType.UNAUTHORIZED, "Bu yorumu güncelleme yetkiniz yok"
                ));
            }

            comment.setContent(request.content());
            Comment updatedComment = commentRepository.save(comment);

            return mapToResponse(updatedComment);

        } catch (BaseException e) {
            throw e;
        } catch (Exception e) {
            throw new BaseException(new ErrorMessage(
                    MessageType.INTERNAL_ERROR, "Yorum güncellenirken hata oluştu: " + e.getMessage()
            ));
        }
    }

    @Override
    public void deleteComment(Long commentId, String username) {
        try {
            Comment comment = commentRepository.findById(commentId)
                    .orElseThrow(() -> new BaseException(new ErrorMessage(
                            MessageType.RESOURCE_NOT_FOUND, "Yorum bulunamadı: " + commentId
                    )));

            if (!comment.getUser().getUsername().equals(username)) {
                throw new BaseException(new ErrorMessage(
                        MessageType.UNAUTHORIZED, "Bu yorumu silme yetkiniz yok"
                ));
            }

            commentRepository.delete(comment);

        } catch (BaseException e) {
            throw e;
        } catch (Exception e) {
            throw new BaseException(new ErrorMessage(
                    MessageType.INTERNAL_ERROR, "Yorum silinirken hata oluştu: " + e.getMessage()
            ));
        }
    }

    @Override
    public List<CommentResponse> getCommentsByFilm(Long filmId) {
        try {
            Film film = filmRepository.findById(filmId)
                    .orElseThrow(() -> new BaseException(new ErrorMessage(
                            MessageType.RESOURCE_NOT_FOUND, "Film bulunamadı: " + filmId
                    )));

            List<Comment> comments = commentRepository.findAllByFilm(film);

            return comments.stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());

        } catch (BaseException e) {
            throw e;
        } catch (Exception e) {
            throw new BaseException(new ErrorMessage(
                    MessageType.INTERNAL_ERROR, "Yorumlar getirilirken hata oluştu: " + e.getMessage()
            ));
        }
    }

    // ===== Mapper =====
    private CommentResponse mapToResponse(Comment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getContent(),
                comment.getCreatedAt(),
                comment.getUpdatedAt(),
                comment.getUser().getUsername(),
                comment.getFilm().getId()
        );
    }
}
