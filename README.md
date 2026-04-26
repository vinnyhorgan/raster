# raster

A fantasy runtime

## Requirements

- Java 26
- Maven 3.9+

## Usage

Run the project:

```sh
mvn compile exec:java
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

Formatting is enforced by Spotless during Maven's `validate` phase. Java sources are formatted with `google-java-format`, and `pom.xml` is formatted with SortPom.

Run the packaged jar:

```sh
java -jar target/raster-0.1.0-SNAPSHOT.jar
```

## Stack

- Java 26
- Maven
- LWJGL 3
- JOML
