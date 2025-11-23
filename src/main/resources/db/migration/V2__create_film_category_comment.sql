-- V2__create_film_category_comment.sql

-- ============================
-- CATEGORY TABLE
-- ============================
CREATE TABLE categories (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(500)
);

-- ============================
-- FILM TABLE
-- ============================
CREATE TABLE films (
    id SERIAL PRIMARY KEY,
    title VARCHAR(50) NOT NULL UNIQUE,
    description TEXT NOT NULL,
    release_date DATE NOT NULL,
    poster_url VARCHAR(500) NOT NULL UNIQUE,
    trailer_url VARCHAR(500) NOT NULL UNIQUE
);

-- ============================
-- FILM_CATEGORY (Many-to-Many)
-- ============================
CREATE TABLE film_category (
    film_id INTEGER NOT NULL REFERENCES films(id) ON DELETE CASCADE,
    category_id INTEGER NOT NULL REFERENCES categories(id) ON DELETE CASCADE,
    PRIMARY KEY (film_id, category_id)
);

-- ============================
-- COMMENT TABLE
-- ============================
CREATE TABLE comments (
    id SERIAL PRIMARY KEY,
    content VARCHAR(2000) NOT NULL,
    created_at TIMESTAMP DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMP,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    film_id INTEGER NOT NULL REFERENCES films(id) ON DELETE CASCADE
);
