package com.serhatsgr.security;

import com.serhatsgr.service.Impl.UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(UserService userService, PasswordEncoder passwordEncoder, JwtAuthFilter jwtAuthFilter) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(x -> x
                        // Film Listeleme ve Arama
                        .requestMatchers(HttpMethod.GET, "/rest/api/film/list", "/rest/api/film/list/**", "/rest/api/film/search").hasAnyRole("ADMIN", "USER")

                        // Kategori Listeleme
                        .requestMatchers(HttpMethod.GET, "/rest/api/category/list", "/rest/api/category/list/**").hasAnyRole("ADMIN", "USER")

                        // Yorumları Okuma
                        .requestMatchers(HttpMethod.GET,"/rest/api/comments/film","/rest/api/comments/film/**").hasAnyRole("ADMIN","USER")

                        // Admin İşlemleri (Film/Kategori CRUD)
                        .requestMatchers("/rest/api/film/save").hasRole("ADMIN")
                        .requestMatchers("/rest/api/film/update/**").hasRole("ADMIN")
                        .requestMatchers("/rest/api/film/delete/**").hasRole("ADMIN")
                        .requestMatchers("/rest/api/category/create").hasRole("ADMIN")
                        .requestMatchers("/rest/api/category/update/**").hasRole("ADMIN")
                        .requestMatchers("/rest/api/category/delete/**").hasRole("ADMIN")
                        .requestMatchers("/rest/api/admin/**").hasRole("ADMIN")

                        // Yorum Yazma/Silme (Herkes)
                        .requestMatchers(HttpMethod.POST,"/rest/api/comments/save").hasAnyRole("ADMIN","USER")
                        .requestMatchers(HttpMethod.PUT,"/rest/api/comments/update/**").hasAnyRole("ADMIN","USER")
                        .requestMatchers(HttpMethod.DELETE,"/rest/api/comments/delete/**").hasAnyRole("ADMIN","USER")

                        // Kullanıcı İşlemleri
                        .requestMatchers("/rest/api/user/**").hasAnyRole("ADMIN", "USER")
                        .requestMatchers("/rest/api/movies/**").hasAnyRole("ADMIN", "USER")
                        .requestMatchers("/rest/api/interactions/**").hasAnyRole("ADMIN", "USER")

                        // Auth (Herkese Açık)
                        .requestMatchers("/auth/**").permitAll()
                )
                .sessionManagement(x-> x.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();

    }

    @Bean
    public AuthenticationProvider authenticationProvider(){
        DaoAuthenticationProvider provider=new DaoAuthenticationProvider();
        provider.setUserDetailsService(userService);
        provider.setPasswordEncoder(passwordEncoder);
        provider.setHideUserNotFoundExceptions(false);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws  Exception{
        return  configuration.getAuthenticationManager();
    }
}