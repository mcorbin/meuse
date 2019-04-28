CREATE TABLE roles(
id UUID PRIMARY KEY,
name TEXT UNIQUE
);

INSERT INTO roles(id, name)
VALUES ('867428a0-69ba-11e9-a674-9f6c32022150', 'admin');

INSERT INTO roles(id, name)
VALUES ('a5435b66-69ba-11e9-8385-8b7c3810e186', 'tech');

CREATE TABLE users(
id UUID PRIMARY KEY,
name TEXT UNIQUE,
password TEXT,
description TEXT,
role_id UUID,
FOREIGN KEY (role_id) REFERENCES roles(id)
);

CREATE TABLE tokens(
id UUID PRIMARY KEY,
token TEXT,
expired_at timestamp,
user_id uuid,
FOREIGN KEY (user_id) REFERENCES users(id)
);

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

CREATE TABLE crate_users(
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

CREATE TABLE crate_categories (
crate_id UUID,
category_id UUID,
PRIMARY KEY(category_id, crate_id),
FOREIGN KEY (crate_id) REFERENCES crates(id),
FOREIGN KEY (category_id) REFERENCES categories(id)
);
