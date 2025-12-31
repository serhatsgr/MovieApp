package com.serhatsgr.service.Impl;

import com.serhatsgr.dto.*;
import com.serhatsgr.entity.Comment;
import com.serhatsgr.entity.Film;
import com.serhatsgr.entity.User;
import com.serhatsgr.exception.*;
import com.serhatsgr.repository.CommentRepository;
import com.serhatsgr.repository.FilmRepository;
import com.serhatsgr.repository.UserRepository;
import com.serhatsgr.service.ICommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements ICommentService {

    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final FilmRepository filmRepository;

    // --- Helper: Admin Kontrolü ---
    private boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals("ROLE_ADMIN"));
    }

    // --- Yorum Oluşturma (Yanıt Verme Dahil) ---
    @Override
    @Transactional
    public CommentResponse createComment(CreateCommentRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BaseException(new ErrorMessage(MessageType.RESOURCE_NOT_FOUND, "Kullanıcı bulunamadı")));

        Film film = filmRepository.findById(request.filmId())
                .orElseThrow(() -> new BaseException(new ErrorMessage(MessageType.RESOURCE_NOT_FOUND, "Film bulunamadı")));

        // Eğer bir yoruma yanıt veriliyorsa (parentCommentId varsa)
        Comment parent = null;
        if (request.parentCommentId() != null) {
            parent = commentRepository.findById(request.parentCommentId())
                    .orElseThrow(() -> new BaseException(new ErrorMessage(MessageType.RESOURCE_NOT_FOUND, "Yanıtlanan yorum bulunamadı")));
        }

        Comment comment = Comment.builder()
                .content(request.content())
                .user(user)
                .film(film)
                .parentComment(parent) // Parent ilişki
                .isDeleted(false)
                .build();

        Comment savedComment = commentRepository.save(comment);
        return mapToResponse(savedComment);
    }

    // --- Yorum Güncelleme ---
    @Override
    @Transactional
    public CommentResponse updateComment(Long commentId, UpdateCommentRequest request, String username) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new BaseException(new ErrorMessage(
                        MessageType.RESOURCE_NOT_FOUND, "Yorum bulunamadı: " + commentId
                )));

        // Silinmiş ("Soft Deleted") bir yorum güncellenemez
        if (comment.isDeleted()) {
            throw new BaseException(new ErrorMessage(
                    MessageType.BAD_REQUEST, "Silinmiş bir yorumu düzenleyemezsiniz."
            ));
        }

        // Sadece yorum sahibi güncelleyebilir
        if (!comment.getUser().getUsername().equals(username)) {
            throw new BaseException(new ErrorMessage(
                    MessageType.FORBIDDEN, "Bu yorumu güncelleme yetkiniz yok"
            ));
        }

        comment.setContent(request.content());
        Comment updatedComment = commentRepository.save(comment);

        return mapToResponse(updatedComment);
    }

    // Yorum Silme (Soft vs Hard Delete) ---
    @Override
    @Transactional
    public void deleteComment(Long commentId, String username) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new BaseException(new ErrorMessage(MessageType.RESOURCE_NOT_FOUND, "Yorum bulunamadı")));

        boolean isOwner = comment.getUser().getUsername().equals(username);
        boolean isAdmin = isAdmin();

        if (!isOwner && !isAdmin) {
            throw new BaseException(new ErrorMessage(MessageType.FORBIDDEN, "Bu yorumu silme yetkiniz yok"));
        }

        //eğer yorumun yanıtı varsa o yorumu tamamen silmek agacı bozar
        // bu sebeple yanıtı olan yorumları Soft Delete ile siliyoruz(içerik gizliyoruz.)
        //yorumun yanıtı yoksa tamamen siliyoruz yani: Hard Delete
        if (comment.getReplies() != null && !comment.getReplies().isEmpty()) {
            comment.setDeleted(true);
            comment.setContent("Bu yorum silindi."); // İçeriği temizle
            commentRepository.save(comment);
        } else {
            commentRepository.delete(comment);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentResponse> getCommentsByFilm(Long filmId) {
        Film film = filmRepository.findById(filmId)
                .orElseThrow(() -> new BaseException(new ErrorMessage(MessageType.RESOURCE_NOT_FOUND, "Film bulunamadı")));

        List<Comment> allComments = commentRepository.findAllByFilm(film);


        List<CommentResponse> allDtos = allComments.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        // ID -> DTO haritası
        Map<Long, CommentResponse> dtoMap = allDtos.stream()
                .collect(Collectors.toMap(CommentResponse::id, c -> c));

        List<CommentResponse> rootComments = new ArrayList<>();

        for (CommentResponse dto : allDtos) {
            if (dto.parentCommentId() == null) {
                rootComments.add(dto);
            } else {
                CommentResponse parent = dtoMap.get(dto.parentCommentId());
                if (parent != null) {
                    parent.replies().add(dto);
                }
            }
        }

        // SIRALAMA:
        // 1. Ana yorumlar: En yeni yorum en üstte olmalı
        rootComments.sort((c1, c2) -> c2.createdAt().compareTo(c1.createdAt()));

        // 2. Alt yorumlar: En eski en üstte olmalı
        allDtos.forEach(dto ->
                dto.replies().sort((r1, r2) -> r1.createdAt().compareTo(r2.createdAt()))
        );

        return rootComments;
    }

    private CommentResponse mapToResponse(Comment comment) {

        boolean isAuthorBanned = !comment.getUser().isEnabled();
        boolean isDeleted = comment.isDeleted();
        String displayContent = isDeleted ? "🗑️ [Silindi]" : (isAuthorBanned ? "🚫 [Banlı]" : comment.getContent());

        return new CommentResponse(
                comment.getId(),
                displayContent,
                comment.getCreatedAt(),
                comment.getUpdatedAt(),
                comment.getUser().getUsername(),
                comment.getFilm().getId(),
                isAuthorBanned,
                isDeleted,
                comment.getParentComment() != null ? comment.getParentComment().getId() : null,
                new ArrayList<>() // Boş liste başlat
        );
    }
}