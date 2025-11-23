package com.serhatsgr.service.Impl;

import com.serhatsgr.dto.CreateUserRequest;
import com.serhatsgr.dto.CreateUserResponse;
import com.serhatsgr.entity.User;
import com.serhatsgr.exception.BaseException;
import com.serhatsgr.exception.ErrorMessage;
import com.serhatsgr.exception.MessageType;
import com.serhatsgr.mapper.UserMapper;
import com.serhatsgr.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService implements UserDetailsService {
    //burada security'nin veritabanı ile ilişkisini halledecez
    //Daha sonra UserDetailsServer geliştirecez

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;

    private final UserMapper userMapper;

    private final BCryptPasswordEncoder passwordEncoder;


    public UserService(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
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
}
