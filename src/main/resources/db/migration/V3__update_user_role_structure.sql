-- V2__update_user_role_structure.sql
-- Author: Serhat Sağır
-- Description: Refactor User-Role relationship to use Enum-based @ElementCollection and add UserDetails fields

---------------------------------------------------------
-- 1. Eski ilişkili tabloları kaldır
---------------------------------------------------------
DROP TABLE IF EXISTS user_roles CASCADE;
DROP TABLE IF EXISTS roles CASCADE;

---------------------------------------------------------
-- 2. Yeni "authorities" tablosunu oluştur
---------------------------------------------------------
CREATE TABLE authorities (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(50) NOT NULL,
    PRIMARY KEY (user_id, role)
);

---------------------------------------------------------
-- 3. users tablosuna yeni alanları ekle (UserDetails için)
---------------------------------------------------------
ALTER TABLE users
    ADD COLUMN account_non_expired BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN account_non_locked BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN credentials_non_expired BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN is_enabled BOOLEAN NOT NULL DEFAULT TRUE;

---------------------------------------------------------
-- 4. Eski "enabled" kolonu artık gereksizse kaldırılabilir
---------------------------------------------------------
ALTER TABLE users DROP COLUMN IF EXISTS enabled;

---------------------------------------------------------
-- 5. (Opsiyonel) Mevcut kullanıcılar için varsayılan yetki ekle
---------------------------------------------------------
-- Bu kısım sadece test için örnektir. Gerçek uygulamada Java tarafında UserService ekleyecek.
-- Burada tüm mevcut kullanıcılara ROLE_USER atanır.
INSERT INTO authorities (user_id, role)
SELECT id, 'ROLE_USER' FROM users;
