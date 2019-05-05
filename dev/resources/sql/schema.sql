CREATE TABLE roles(
id UUID PRIMARY KEY,
name TEXT NOT NULL UNIQUE
);

INSERT INTO roles(id, name)
VALUES ('867428a0-69ba-11e9-a674-9f6c32022150', 'admin');

INSERT INTO roles(id, name)
VALUES ('a5435b66-69ba-11e9-8385-8b7c3810e186', 'tech');

CREATE TABLE users(
id UUID PRIMARY KEY,
cargo_id SERIAL,
name TEXT NOT NULL UNIQUE,
password TEXT NOT NULL,
description TEXT NOT NULL,
role_id UUID,
FOREIGN KEY (role_id) REFERENCES roles(id)
);

INSERT INTO users(id, name, password, description, role_id)
VALUES ('fe9dd8f0-6ac6-11e9-8607-1f7cdd916f75', 'user1', '$2a$11$cI.kgSttkrag3Mdj3Zg9gupv4dZ4kFisomeWauZqSDCXa2ZSL3MwK', 'desc 1', 'a5435b66-69ba-11e9-8385-8b7c3810e186');

INSERT INTO users(id, name, password, description, role_id)
VALUES ('1dbdeb3a-6ac7-11e9-b2dc-dbb2118a325d', 'user2', '$2a$11$YEHP9DkYTPlMVZKhlHQQWulanfJHyjjYp0pyfmWngSR0rpmIcqb2i', 'desc 2', 'a5435b66-69ba-11e9-8385-8b7c3810e186');

CREATE TABLE tokens(
id UUID PRIMARY KEY,
token TEXT NOT NULL UNIQUE,
expired_at timestamp NOT NULL,
user_id uuid,
FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE crates (
id UUID PRIMARY KEY,
name TEXT NOT NULL UNIQUE
);

CREATE TABLE crates_versions(
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

CREATE INDEX idx_crates_versions_document_vectors ON crates_versions USING gin(document_vectors);

CREATE TABLE crates_users(
crate_id UUID,
user_id UUID,
PRIMARY KEY(crate_id, user_id),
FOREIGN KEY (crate_id) REFERENCES crates(id),
FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE categories (
id UUID PRIMARY KEY,
description TEXT,
name TEXT NOT NULL UNIQUE
);

CREATE TABLE crates_categories (
crate_id UUID,
category_id UUID,
PRIMARY KEY(category_id, crate_id),
FOREIGN KEY (crate_id) REFERENCES crates(id),
FOREIGN KEY (category_id) REFERENCES categories(id)
);
