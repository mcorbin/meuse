# Meuse

A free crate registry for the Rust programming language.

It implements the [alternative registries](https://github.com/rust-lang/rfcs/blob/master/text/2141-alternative-registries.md) RFC and offers also various features (cf the `Features` section).

You can use Meuse to store your private crates, configure it to mirror crates.io...

Documentation is available at https://meuse.mcorbin.fr/

## Features

- [x] Complete implementation of the alternative registries RFC (including search).
- [x] crates.io mirroring.
- [x] Multiple backends for crates files: filesystem, S3.
- [x] Multiple ways of managing the Git crate Index: git command, JGit.
- [x] Manage categories.
- [x] Manage users, roles, and tokens.
- [x] Manage crates.
- [x] Security: HTTPS support, TLS support for the PostgreSQL client.
- [x] Monitoring: Meuse exposes a Prometheus endpoint with various metrics (HTTP server, database pool, JVM metrics...).
- [x] Small frontend to explore crates.

## Plan

Take a look at https://meuse.mcorbin.fr/roadmap/.
