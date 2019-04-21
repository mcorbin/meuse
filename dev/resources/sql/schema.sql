CREATE TABLE users(
id UUID,
name TEXT,
password TEXT,
informations TEXT,
);

CREATE TABLE tokens(
token TEXT,
expired_at timestamp,
user_id uuid
);

CREATE TABLE crates (
id UUID,
name TEXT

);

CREATE TABLE crate_versions(
id UUID,
version TEXT,
description TEXT,
yanked BOOLEAN,
created_at TIMESTAMP,
updated_at TIMESTAMP,
document_vectors TSVECTOR,
crate_name TEXT
);

CREATE INDEX idx_crate_versions_document_vectors ON crate_versions USING gin(document_vectors);
