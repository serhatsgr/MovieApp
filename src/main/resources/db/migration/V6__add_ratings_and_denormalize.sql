-- 1. Films tablosuna özet alanları ekle
ALTER TABLE films
    ADD COLUMN average_rating DOUBLE PRECISION DEFAULT 0.0,
    ADD COLUMN rating_count INTEGER DEFAULT 0;

-- 2. Ratings tablosunu oluştur
CREATE TABLE ratings (
    id BIGSERIAL PRIMARY KEY,
    score INTEGER NOT NULL CHECK (score >= 1 AND score <= 5),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    film_id BIGINT NOT NULL REFERENCES films(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uk_film_user UNIQUE (film_id, user_id)
);