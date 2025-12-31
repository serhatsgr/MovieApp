package com.serhatsgr.service;

import com.serhatsgr.dto.CommentResponse;
import com.serhatsgr.dto.CreateCommentRequest;
import com.serhatsgr.entity.Comment;
import com.serhatsgr.entity.Film;
import com.serhatsgr.entity.User;
import com.serhatsgr.exception.BaseException;
import com.serhatsgr.exception.MessageType;
import com.serhatsgr.repository.CommentRepository;
import com.serhatsgr.repository.FilmRepository;
import com.serhatsgr.repository.UserRepository;
import com.serhatsgr.service.Impl.CommentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock private CommentRepository commentRepository;
    @Mock private UserRepository userRepository;
    @Mock private FilmRepository filmRepository;

    @InjectMocks private CommentServiceImpl commentService;

    @BeforeEach
    void setupSecurity() {
        Authentication auth = mock(Authentication.class);
        SecurityContext sec = mock(SecurityContext.class);

        lenient().when(sec.getAuthentication()).thenReturn(auth);
        lenient().when(auth.getName()).thenReturn("user");

        SecurityContextHolder.setContext(sec);
    }

    // --------------------------------------------------------------------
    // CREATE COMMENT TESTLERİ
    // --------------------------------------------------------------------

    @Test
    @DisplayName("createComment -> Başarılı")
    void createComment_Success() {
        // Parametre Sırası: content, filmId, parentId
        CreateCommentRequest req = new CreateCommentRequest("Yorum", 1L, null);

        User user = new User(); user.setUsername("user"); user.setEnabled(true);
        Film film = new Film(); film.setId(1L);

        Comment comment = new Comment();
        comment.setId(10L);
        comment.setContent("Yorum");
        comment.setUser(user);
        comment.setFilm(film);
        comment.setCreatedAt(LocalDateTime.now());

        given(userRepository.findByUsername("user")).willReturn(Optional.of(user));
        given(filmRepository.findById(1L)).willReturn(Optional.of(film));
        given(commentRepository.save(any(Comment.class))).willReturn(comment);

        CommentResponse res = commentService.createComment(req);

        assertThat(res.content()).isEqualTo("Yorum");
    }

    @Test
    @DisplayName("createComment -> Kullanıcı bulunamazsa NOT_FOUND")
    void createComment_UserNotFound() {
        CreateCommentRequest req = new CreateCommentRequest("test", 1L, null);

        given(userRepository.findByUsername("user")).willReturn(Optional.empty());

        Throwable thrown = catchThrowable(() -> commentService.createComment(req));

        assertThat(thrown)
                .isInstanceOf(BaseException.class)
                .extracting("errorMessage.messageType")
                .isEqualTo(MessageType.RESOURCE_NOT_FOUND);
    }

    @Test
    @DisplayName("createComment -> Film bulunamazsa NOT_FOUND")
    void createComment_FilmNotFound() {
        CreateCommentRequest req = new CreateCommentRequest("test", 1L, null);
        User user = new User(); user.setEnabled(true);

        given(userRepository.findByUsername("user")).willReturn(Optional.of(user));
        given(filmRepository.findById(1L)).willReturn(Optional.empty());

        Throwable thrown = catchThrowable(() -> commentService.createComment(req));

        assertThat(thrown)
                .isInstanceOf(BaseException.class)
                .extracting("errorMessage.messageType")
                .isEqualTo(MessageType.RESOURCE_NOT_FOUND);
    }

    // --------------------------------------------------------------------
    // DELETE COMMENT TESTLERİ
    // --------------------------------------------------------------------

    @Test
    @DisplayName("deleteComment -> Yorum bulunamazsa NOT_FOUND")
    void deleteComment_NotFound() {
        given(commentRepository.findById(1L)).willReturn(Optional.empty());

        Throwable thrown = catchThrowable(() -> commentService.deleteComment(1L, "user"));

        assertThat(thrown)
                .isInstanceOf(BaseException.class)
                .extracting("errorMessage.messageType")
                .isEqualTo(MessageType.RESOURCE_NOT_FOUND);
    }

    @Test
    @DisplayName("deleteComment -> Başkasının yorumunu silmeye çalışırsa FORBIDDEN")
    void deleteComment_Forbidden() {
        User owner = new User(); owner.setUsername("owner");
        Comment comment = new Comment(); comment.setUser(owner);

        // Yorum var, sahibi başkası -> isAdmin kontrolü için SecurityContext'e bakacak
        given(commentRepository.findById(1L)).willReturn(Optional.of(comment));

        Throwable thrown = catchThrowable(() -> commentService.deleteComment(1L, "user"));

        assertThat(thrown)
                .isInstanceOf(BaseException.class)
                .extracting("errorMessage.messageType")
                .isEqualTo(MessageType.FORBIDDEN);
    }

    @Test
    @DisplayName("deleteComment -> Sahibiyse başarıyla silinir")
    void deleteComment_Success() {
        User owner = new User(); owner.setUsername("user");
        Comment comment = new Comment(); comment.setUser(owner);

        // Yorum var, sahibi "user" -> isOwner=true -> isAdmin kontrolü yapılmaz (SecurityContext kullanılmaz)
        given(commentRepository.findById(1L)).willReturn(Optional.of(comment));

        commentService.deleteComment(1L, "user");

        verify(commentRepository).delete(comment);
    }
}