# Meuse

A Rust registry written in Clojure.

Work in progress, cf https://mcorbin.fr/posts/2019-05-01-meuse-rust-registry/

## Run the project

### PostgreSQL

In order to run Meuse, you need PostgreSQL. You should create a database and apply the sql script in the `dev/resources/sql/schema.sql` file.

You can also run the `./postgres.sh` script. This script will launch a Docker instance and automatically create a database named `meuse` (the database user and password are also `meuse`) and the schema. You will then be able to connect to the database with `psql -h localhost -p 5432 -U meuse`.

### Github repository

Crates metadata are stored in a Git repository. You need a git repository on the machine running Meuse, and Meuse should be able to run git commands (add, commit, push...) on this repository. You should then cache git credentials or if you use SSH authentication, add your SSH passphrase in an ssh agent.

### Meuse configuration

Meuse needs a `yaml` configuration file. An example file exists in `dev/resources/config.yaml` (this is the one I use when I work on the project). The configuration sections are:

* database:
  - user: the database user.
  - password: the database password.
  - subname: the url to the database.
* http:
  - address: the IP address on which Meuse will listen.
  - port: the Meuse HTTP port.
* logging: logging configuration, cf the [https://github.com/pyr/unilog/](unilog) library doc.
* metadata:
  - path: the directory path to your Git repository. The Meuse process should be able to access and write in it.
  - target: the target branch, the format should be `<remote>/<branch>`
  - url: url of the Git repository. The URL will be used by Meuse to allow dependencies from Meuse if the `allowed-registries` option is not set in the repository config (cf the [https://github.com/rust-lang/rfcs/blob/master/text/2141-alternative-registries.md#registry-index-format-specification](Registry index format specification)).
* crate:
  - path: The directory where Meuse will save crates files. Meuse will also expose this directory for Cargo.

### Run Meuse

You currently need [https://leiningen.org/](Leiningen), a Clojure build tool, to run the project. In the future, I will release `Meuse` jars for Meuse versions.

The `MEUSE_CONFIGURATION` variable should be the path of your yaml configuration file. You can then run the project with `lein run` in the project directory:

```
MEUSE_CONFIGURATION="$(pwd)/dev/resources/config.yaml" lein run
```

### Cargo configuration

Add an entry for your custom registry in your `.cargo/config` file:

```
[registries.custom]
index = "https://github.com/mcorbin/testregistry.git"
```

Your Git repository should contain a `config.json` file which will contain the URL of the Meuse API (as described in the Cargo (https://doc.rust-lang.org/nightly/cargo/reference/registries.html)[registries] documentation), for example:

```
{
    "dl": "http://localhost:8855/api/v1/crates",
    "api": "http://localhost:8855"
}
```

Next, Add a random token for this registry in `.cargo/credentials` (currently, token management is not implemented in Meuse ^^):

```
[registries.custom]
token = "aaaaaa"
```

You can now configure your Rust project to publish crates in the Meuse registry:

```
[package]
name = "testpublish"
description = "This is a description of my project"
version = "0.1.13"
authors = ["mcorbin <corbin.math@gmail.com>"]
edition = "2018"

[lib]
publish = ["custom"]

[dependencies]
```

You can now publish your crate:

```
$ cargo publish --allow-dirty --registry custom
    Updating `https://github.com/mcorbin/testregistry.git` index
warning: manifest has no license, license-file, documentation, homepage or repository.
See <http://doc.crates.io/manifest.html#package-metadata> for more info.
   Packaging testpublish v0.1.13 (/home/mathieu/prog/rust/testpublish)
   Verifying testpublish v0.1.13 (/home/mathieu/prog/rust/testpublish)
   Compiling testpublish v0.1.13 (/home/mathieu/prog/rust/testpublish/target/package/testpublish-0.1.13)
warning: field is never used: `foo`
 --> src/bar/baz.rs:2:5
  |
2 |     foo: u32
  |     ^^^^^^^^
  |
  = note: #[warn(dead_code)] on by default

    Finished dev [unoptimized + debuginfo] target(s) in 2.83s
   Uploading testpublish v0.1.13 (/home/mathieu/prog/rust/testpublish)
```
