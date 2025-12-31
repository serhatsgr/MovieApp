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
import java.util.stream.Collectors;

@Service
@Transactional
public class UserService implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final CommentRepository commentRepository;
    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       BCryptPasswordEncoder passwordEncoder,
                       UserMapper userMapper,
                       CommentRepository commentRepository) {

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

    // CREATE USER
    public CreateUserResponse createUser(CreateUserRequest request) {
        try {
            logger.info("Creating new user: {}", request.username());

            if (userRepository.findByUsername(request.username()).isPresent()) {
                throw new BaseException(new ErrorMessage(
                        MessageType.DUPLICATE_RESOURCE,
                        "Bu kullanıcı adı zaten kullanımda"
                ));
            }

            if (userRepository.findByEmail(request.email()).isPresent()) {
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

            return userMapper.toDto(savedUser, "User created successfully");

        } catch (BaseException e) {
            throw e;

        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            logger.error("Data integrity violation: {}", request.username(), e);
            throw new BaseException(new ErrorMessage(
                    MessageType.DATA_INTEGRITY_VIOLATION,
                    "Veritabanı kısıtlaması ihlali"
            ));
        } catch (Exception e) {
            logger.error("Error creating user {}", request.username(), e);
            throw new BaseException(new ErrorMessage(
                    MessageType.INTERNAL_ERROR,
                    "Kullanıcı oluşturulamadı"
            ));
        }
    }


    // =============== UPDATE USER ===============
    public User updateUser(String currentUsername, UpdateUserRequest request) {

        User user = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new BaseException(new ErrorMessage(
                        MessageType.RESOURCE_NOT_FOUND, "Kullanıcı bulunamadı"
                )));

        if (!user.getUsername().equals(request.username()) &&
                userRepository.findByUsername(request.username()).isPresent()) {
            throw new BaseException(new ErrorMessage(
                    MessageType.DUPLICATE_RESOURCE, "Bu kullanıcı adı zaten kullanımda"
            ));
        }

        if (!user.getEmail().equals(request.email()) &&
                userRepository.findByEmail(request.email()).isPresent()) {
            throw new BaseException(new ErrorMessage(
                    MessageType.DUPLICATE_RESOURCE, "Bu email adresi zaten kullanımda"
            ));
        }

        user.setUsername(request.username());
        user.setEmail(request.email());

        return userRepository.save(user);
    }


    // =============== GET ALL USERS ===============
    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(user -> new UserDto(
                        user.getId(),
                        user.getUsername(),
                        user.getEmail(),
                        user.isEnabled(),
                        user.getAuthorities()
                                .stream()
                                .map(a -> a.getAuthority())
                                .collect(Collectors.toSet()),
                        null
                ))
                .collect(Collectors.toList());
    }


    // =============== GET USER BY ID ===============
    public UserDto getUserById(Long id) {

        User user = userRepository.findById(id)
                .orElseThrow(() ->
                        new BaseException(new ErrorMessage(MessageType.RESOURCE_NOT_FOUND, "Kullanıcı bulunamadı"))
                );

        return new UserDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.isEnabled(),
                user.getAuthorities()
                        .stream()
                        .map(a -> a.getAuthority())
                        .collect(Collectors.toSet()),
                null
        );
    }

    // =============== GET USER BY USERNAME ===============
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BaseException(new ErrorMessage(
                        MessageType.RESOURCE_NOT_FOUND,
                        "Kullanıcı bulunamadı: " + username
                )));
    }
    // user ban
    public UserDto toggleUserBan(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() ->
                        new BaseException(new ErrorMessage(MessageType.RESOURCE_NOT_FOUND, "Kullanıcı bulunamadı"))
                );

        user.setEnabled(!user.isEnabled());

        return getUserById(userRepository.save(user).getId());
    }

    // role change
    @Transactional
    public UserDto toggleUserRole(Long id) {

        User user = userRepository.findById(id)
                .orElseThrow(() ->
                        new BaseException(new ErrorMessage(MessageType.RESOURCE_NOT_FOUND, "Kullanıcı bulunamadı"))
                );

        boolean isAdmin =
                user.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        user.getAuthorities().clear();

        if (isAdmin) {
            user.getAuthorities().add(Role.ROLE_USER);
        } else {
            user.getAuthorities().add(Role.ROLE_ADMIN);
        }

        return getUserById(userRepository.save(user).getId());
    }


    // user delete
    public void deleteUser(Long id) {

        if (!userRepository.existsById(id)) {
            throw new BaseException(new ErrorMessage(MessageType.RESOURCE_NOT_FOUND, "Kullanıcı bulunamadı"));
        }

        commentRepository.deleteByUserId(id);
        userRepository.deleteById(id);
    }


    // self delete
    public void deleteUserByUsername(String username) {

        User user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new BaseException(new ErrorMessage(MessageType.RESOURCE_NOT_FOUND, "Kullanıcı bulunamadı"))
                );

        deleteUser(user.getId());
    }
}
