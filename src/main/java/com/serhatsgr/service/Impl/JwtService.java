package com.serhatsgr.service.Impl;

import com.serhatsgr.dto.RefreshTokenResponse;
import com.serhatsgr.dto.TokenPairDto;
import com.serhatsgr.entity.RefreshToken;
import com.serhatsgr.entity.User; // EKLENDİ
import com.serhatsgr.exception.BaseException;
import com.serhatsgr.exception.ErrorMessage;
import com.serhatsgr.exception.MessageType;
import com.serhatsgr.repository.RefreshTokenRepository;
import com.serhatsgr.repository.UserRepository; // EKLENDİ
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority; // EKLENDİ
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors; // EKLENDİ

@Service
public class JwtService {

    @Value("${jwt.key}")
    private String SECRET;

    @Value("${jwt.access-token-expiry}")
    private Long ACCESS_TOKEN_EXPIRY;

    @Value("${jwt.refresh-token-expiry}")
    private Long REFRESH_TOKEN_EXPIRY;

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository; // EKLENDİ

    // Constructor Güncellendi
    public JwtService(RefreshTokenRepository refreshTokenRepository, UserRepository userRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
    }

    private Key getSignKey(){
        try {
            byte[] keyBytes = Decoders.BASE64.decode(SECRET);
            return Keys.hmacShaKeyFor(keyBytes);
        } catch (Exception e) {
            throw new BaseException(new ErrorMessage(MessageType.INTERNAL_ERROR, "JWT key oluşturulamadı"));
        }
    }

    public String generateAccessToken(String username){
        Map<String, Object> claims = new HashMap<>();
        try {
            // Kullanıcıyı bul ve rollerini al
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new BaseException(new ErrorMessage(MessageType.NOT_FOUND, "Kullanıcı bulunamadı")));

            // Rolleri "authorities" key'i ile listeye çevirip ekle
            claims.put("authorities", user.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList()));

            return createToken(claims, username);
        } catch (BaseException e) {
            throw e;
        } catch (Exception e) {
            throw new BaseException(new ErrorMessage(MessageType.INTERNAL_ERROR, "Access token oluşturulamadı"));
        }
    }

    private String createToken(Map<String, Object> claims, String username){
        try {
            return Jwts.builder()
                    .setClaims(claims)
                    .setSubject(username)
                    .setIssuedAt(new Date(System.currentTimeMillis()))
                    .setExpiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_EXPIRY*1000))
                    .signWith(getSignKey(), SignatureAlgorithm.HS256)
                    .compact();
        } catch (Exception e) {
            throw new BaseException(new ErrorMessage(MessageType.INTERNAL_ERROR, "Token oluşturulamadı"));
        }
    }

    private Claims extractAllClaim(String token){
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSignKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            throw new BaseException(new ErrorMessage(MessageType.TOKEN_EXPIRED, "Token geçersiz veya süresi dolmuş"));
        }
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsTFunction){
        return claimsTFunction.apply(extractAllClaim(token));
    }

    public String extractUsername(String token){
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token){
        return extractClaim(token, Claims::getExpiration);
    }

    public Boolean isTokenExpired(String token){
        try {
            return extractExpiration(token).before(new Date());
        } catch (Exception e) {
            throw new BaseException(new ErrorMessage(MessageType.INTERNAL_ERROR, "Token süresi kontrol edilemedi"));
        }
    }

    public Boolean validateToken(String token , UserDetails userDetails){
        try {
            final String username = extractUsername(token);
            return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (BaseException e) {
            throw e;
        } catch (Exception e) {
            throw new BaseException(new ErrorMessage(MessageType.UNAUTHORIZED, "Token doğrulama başarısız"));
        }
    }

    public RefreshTokenResponse generateRefreshToken(String username){
        try {
            refreshTokenRepository.markAllAsUsedByUsername(username);

            String refreshTokenValue = UUID.randomUUID().toString() + "-" + System.currentTimeMillis();
            LocalDateTime expiryDate = LocalDateTime.now().plusMinutes(REFRESH_TOKEN_EXPIRY);

            RefreshToken refreshToken = new RefreshToken(refreshTokenValue, username, expiryDate);
            RefreshToken refreshTokenSave = refreshTokenRepository.save(refreshToken);

            return RefreshTokenResponse.success(refreshTokenSave.getToken(), "Refresh token başarıyla oluşturuldu");
        } catch (Exception e) {
            throw new BaseException(new ErrorMessage(MessageType.INTERNAL_ERROR, "Refresh token oluşturulamadı"));
        }
    }

    public TokenPairDto generateTokenPair(String userName){
        try {
            String accesToken = generateAccessToken(userName);
            RefreshTokenResponse refreshToken = generateRefreshToken(userName);
            return new TokenPairDto(accesToken, refreshToken.refreshToken());
        } catch (Exception e) {
            throw new BaseException(new ErrorMessage(MessageType.INTERNAL_ERROR, "Token pair oluşturulamadı"));
        }
    }

    public String refreshAccessToken(String refreshTokenValue){
        try {
            RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenValue)
                    .orElseThrow(() -> new BaseException(new ErrorMessage(MessageType.NOT_FOUND, "Refresh token bulunamadı")));

            if(validateRefreshToken(refreshTokenValue)){
                refreshToken.setUsed(true);
                refreshTokenRepository.save(refreshToken);
                return refreshToken.getUsername();
            }

            refreshTokenRepository.delete(refreshToken);
            throw new BaseException(new ErrorMessage(MessageType.UNAUTHORIZED, "Refresh token geçersiz"));

        } catch (BaseException e) {
            throw e;
        } catch (Exception e) {
            throw new BaseException(new ErrorMessage(MessageType.INTERNAL_ERROR, "Refresh token işlenemedi"));
        }
    }

    public boolean validateRefreshToken(String refreshTokenValue){
        try {
            return refreshTokenRepository.findByToken(refreshTokenValue)
                    .map(refreshToken -> !refreshToken.isExpired() && !refreshToken.isUsed())
                    .orElse(false);
        } catch (Exception e) {
            throw new BaseException(new ErrorMessage(MessageType.INTERNAL_ERROR, "Refresh token doğrulaması yapılamadı"));
        }
    }

    public void revokeAllRefreshTokens(String username){
        try {
            refreshTokenRepository.deleteByUsername(username);
        } catch (Exception e) {
            throw new BaseException(new ErrorMessage(MessageType.INTERNAL_ERROR, "Tüm tokenler iptal edilemedi"));
        }
    }

    public void cleanupExpiredTokens(){
        try {
            refreshTokenRepository.deleteExpiredTokens(LocalDateTime.now());
        } catch (Exception e) {
            throw new BaseException(new ErrorMessage(MessageType.INTERNAL_ERROR, "Süresi dolmuş tokenlar temizlenemedi"));
        }
    }
}