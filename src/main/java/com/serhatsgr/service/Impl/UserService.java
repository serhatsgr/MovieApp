package com.serhatsgr.service.Impl;

import com.serhatsgr.dto.CreateUserRequest;
import com.serhatsgr.dto.CreateUserResponse;
import com.serhatsgr.dto.UpdateUserRequest;
import com.serhatsgr.dto.UserDto;
import com.serhatsgr.entity.Role;
import com.serhatsgr.entity.User;
import com.serhatsgr.exception.BaseException;
import com.serhatsgr.exception.ErrorMessage;
import com.serhatsgr.exception.MessageType;
import com.serhatsgr.mapper.UserMapper;
import com.serhatsgr.repository.CommentRepository;
import com.serhatsgr.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserService implements UserDetailsService {
    //burada security'nin veritabanı ile ilişkisini halledecez
    //Daha sonra UserDetailsServer geliştirecez

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;

    private final CommentRepository commentRepository;

    private final UserMapper userMapper;

    private final BCryptPasswordEncoder passwordEncoder;


    public UserService(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder, UserMapper userMapper, CommentRepository commentRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
        this.commentRepository = commentRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<User> user = userRepository.findByUsername(username);
        return user.orElseThrow(() -> new UsernameNotFoundException("Kullanıcı bulunamadı: " + username));
    }

    public CreateUserResponse createUser(CreateUserRequest request) {
        try {
            logger.info("Creating new user: {}", request.username());

            // Kullanıcı adı ve email kontrolü
            if (userRepository.findByUsername(request.username()).isPresent()) {
                logger.warn("Username already exists: {}", request.username());
                throw new BaseException(new ErrorMessage(
                        MessageType.DUPLICATE_RESOURCE,
                        "Bu kullanıcı adı zaten kullanımda"
                ));
            }

            if (userRepository.findByEmail(request.email()).isPresent()) {
                logger.warn("Email already exists: {}", request.email());
                throw new BaseException(new ErrorMessage(
                        MessageType.DUPLICATE_RESOURCE,
                        "Bu email adresi zaten kullanımda"
                ));
            }

            User newUser = new User();
            newUser.setUsername(request.username());
            newUser.setEmail(request.email());
            newUser.setPassword(passwordEncoder.encode(request.password()));
            newUser.setAuthorities(request.authorities());
            newUser.setAccountNonExpired(true);
            newUser.setAccountNonLocked(true);
            newUser.setEnabled(true);
            newUser.setCredentialsNonExpired(true);

            User savedUser = userRepository.save(newUser);
            logger.info("User created successfully: {}", savedUser.getUsername());
            return userMapper.toDto(savedUser, "User created successfully");

        } catch (BaseException e) {
            // BaseException zaten doğru formatta, aynen yukarı taşı
            throw e;
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            logger.error("Data integrity violation while creating user: {}", request.username(), e);
            throw new BaseException(new ErrorMessage(
                    MessageType.DATA_INTEGRITY_VIOLATION,
                    "Veritabanı kısıtlaması ihlali"
            ));
        } catch (Exception e) {
            logger.error("Error creating user {}: {}", request.username(), e.getMessage(), e);
            throw new BaseException(new ErrorMessage(
                    MessageType.INTERNAL_ERROR,
                    "Kullanıcı oluşturulamadı"
            ));
        }
    }

    //hesap güncelleme
    public User updateUser(String currentUsername, UpdateUserRequest request) {
        User user = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new BaseException(new ErrorMessage(
                        MessageType.RESOURCE_NOT_FOUND, "Kullanıcı bulunamadı"
                )));

        // 1. Kullanıcı adı değişikliği varsa ve yeni isim doluysa kontrol et
        if (!user.getUsername().equals(request.username()) && userRepository.findByUsername(request.username()).isPresent()) {
            throw new BaseException(new ErrorMessage(
                    MessageType.DUPLICATE_RESOURCE, "Bu kullanıcı adı zaten kullanımda"
            ));
        }

        // 2. Email değişikliği varsa ve yeni email doluysa kontrol et
        if (!user.getEmail().equals(request.email()) && userRepository.findByEmail(request.email()).isPresent()) {
            throw new BaseException(new ErrorMessage(
                    MessageType.DUPLICATE_RESOURCE, "Bu email adresi zaten kullanımda"
            ));
        }

        user.setUsername(request.username());
        user.setEmail(request.email());

        return userRepository.save(user);
    }

    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(user -> new UserDto(
                        user.getId(),
                        user.getUsername(),
                        user.getEmail(),
                        user.isEnabled(),
                        user.getAuthorities().stream().map(role -> role.name()).collect(Collectors.toSet()),
                        null // CreatedAt entity'de varsa eklenir, yoksa null
                ))
                .collect(Collectors.toList());
    }

    // 2. Kullanıcı Detayı
    public UserDto getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BaseException(new ErrorMessage(MessageType.RESOURCE_NOT_FOUND, "Kullanıcı bulunamadı")));
        return new UserDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.isEnabled(),
                user.getAuthorities().stream().map(role -> role.name()).collect(Collectors.toSet()),
                null
        );
    }

    // 3. Ban / Unban (Soft Delete mantığı - Girişi engeller)
    public UserDto toggleUserBan(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BaseException(new ErrorMessage(MessageType.RESOURCE_NOT_FOUND, "Kullanıcı bulunamadı")));

        // Admin kendini banlayamasın
        if (user.getAuthorities().stream().anyMatch(r -> r.name().equals("ROLE_ADMIN"))) {
            // Opsiyonel: Admin banlanamaz kuralı
        }

        user.setEnabled(!user.isEnabled()); // Durumu tersine çevir
        User saved = userRepository.save(user);
        return getUserById(saved.getId());
    }

    // 4. Rol Değiştirme (DÜZELTİLMİŞ HALİ)
    @Transactional // Transactional anotasyonunu unutma
    public UserDto toggleUserRole(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BaseException(new ErrorMessage(MessageType.RESOURCE_NOT_FOUND, "Kullanıcı bulunamadı")));

        // Mevcut rolleri kontrol et
        boolean isAdmin = user.getAuthorities().contains(Role.ROLE_ADMIN);

        // Listeyi temizle ve yeni rolü ekle (Hibernate dostu yöntem)
        user.getAuthorities().clear();
        if (isAdmin) {
            user.getAuthorities().add(Role.ROLE_USER);
        } else {
            user.getAuthorities().add(Role.ROLE_ADMIN);
        }

        // Kaydet ve DTO dön
        User savedUser = userRepository.save(user);
        return getUserById(savedUser.getId());
    }

    //Kullanıcı silme
    public void deleteUser(Long id) {
        // 1. Kullanıcı var mı kontrol et
        if (!userRepository.existsById(id)) {
            throw new BaseException(new ErrorMessage(MessageType.RESOURCE_NOT_FOUND, "Kullanıcı bulunamadı"));
        }

        // 2. Önce kullanıcıya ait yorumları sil (Manual Cascade)
        commentRepository.deleteByUserId(id);

        // 3. Sonra kullanıcıyı sil
        userRepository.deleteById(id);
    }

    // --- Kullanıcı Adı ile Silme (Kendini Silme İçin) ---
    public void deleteUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BaseException(new ErrorMessage(MessageType.RESOURCE_NOT_FOUND, "Kullanıcı bulunamadı")));

        deleteUser(user.getId());
    }
}
