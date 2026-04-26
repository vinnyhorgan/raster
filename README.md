# raster

A fantasy runtime

## Requirements

- Java 26
- Maven 3.9+

## Usage

Run the project:

```sh
mvn compile exec:exec
```

Build the project:

```sh
mvn package
```

Check formatting:

```sh
mvn validate
```

Apply formatting:

```sh
mvn spotless:apply
```

Formatting is enforced by Spotless during Maven's `validate` phase. Java sources are formatted with `google-java-format`, Markdown files are formatted with Flexmark, and `pom.xml` is formatted with SortPom.

Run the packaged jar:

```sh
__GL_THREADED_OPTIMIZATIONS=0 java --enable-native-access=ALL-UNNAMED -jar target/raster-0.1.0-SNAPSHOT.jar
```

## Stack

- Java 26
- Maven
- LWJGL 3
- JOML

