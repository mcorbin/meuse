CREATE TABLE IF NOT EXISTS roles(
id UUID PRIMARY KEY,
name TEXT NOT NULL UNIQUE
);

INSERT INTO roles(id, name)
VALUES ('867428a0-69ba-11e9-a674-9f6c32022150', 'admin') ON CONFLICT DO NOTHING;

INSERT INTO roles(id, name)
VALUES ('a5435b66-69ba-11e9-8385-8b7c3810e186', 'tech') ON CONFLICT DO NOTHING;

CREATE TABLE IF NOT EXISTS users(
id UUID PRIMARY KEY,
cargo_id SERIAL,
name TEXT NOT NULL UNIQUE,
password TEXT NOT NULL,
description TEXT NOT NULL,
active BOOLEAN NOT NULL,
role_id UUID,
FOREIGN KEY (role_id) REFERENCES roles(id)
);

CREATE TABLE IF NOT EXISTS tokens(
id UUID PRIMARY KEY,
name TEXT NOT NULL,
identifier TEXT NOT NULL UNIQUE,
token TEXT NOT NULL UNIQUE,
created_at timestamp NOT NULL,
expired_at timestamp NOT NULL,
user_id uuid,
FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS crates (
id UUID PRIMARY KEY,
name TEXT NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS crates_versions(
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

CREATE INDEX IF NOT EXISTS idx_crates_versions_document_vectors ON crates_versions USING gin(document_vectors);

CREATE TABLE IF NOT EXISTS crates_users(
crate_id UUID,
user_id UUID,
PRIMARY KEY(crate_id, user_id),
FOREIGN KEY (crate_id) REFERENCES crates(id),
FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS categories (
id UUID PRIMARY KEY,
description TEXT,
name TEXT NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS crates_categories (
crate_id UUID,
category_id UUID,
PRIMARY KEY(category_id, crate_id),
FOREIGN KEY (crate_id) REFERENCES crates(id),
FOREIGN KEY (category_id) REFERENCES categories(id)
);
