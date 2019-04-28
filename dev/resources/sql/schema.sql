-- CREATE TABLE users(
-- id UUID,
-- name TEXT,
-- password TEXT,
-- informations TEXT,
-- );

-- CREATE TABLE tokens(
-- token TEXT,
-- expired_at timestamp,
-- user_id uuid
-- );

CREATE TABLE crates (
id UUID PRIMARY KEY,
name TEXT NOT NULL UNIQUE
);

CREATE TABLE crate_versions(
id UUID PRIMARY KEY,
version TEXT NOT NULL,
description TEXT,
yanked BOOLEAN NOT NULL,
created_at TIMESTAMP NOT NULL,
updated_at TIMESTAMP NOT NULL,
document_vectors TSVECTOR,
crate_id UUID,
FOREIGN KEY (crate_id) REFERENCES crates(id)
);

CREATE INDEX idx_crate_versions_document_vectors ON crate_versions USING gin(document_vectors);

CREATE TABLE categories (
id UUID PRIMARY KEY,
description TEXT,
name TEXT NOT NULL UNIQUE
);

CREATE TABLE crate_categories (
category_id UUID,
crate_id UUID,
PRIMARY KEY(category_id, crate_id),
FOREIGN KEY (crate_id) REFERENCES crates(id),
FOREIGN KEY (category_id) REFERENCES categories(id)
);
