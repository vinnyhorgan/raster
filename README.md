# raster

A fantasy runtime

## Requirements

- Java 26
- Maven 3.9+
- GCC, for the local LuaJIT JNI bridge
- `clang-format` 22.1.3, for Spotless C formatting

## Usage

Run a Raster project from a directory containing `main.lua`:

```sh
mvn process-classes exec:exec -Dexec.args="path/to/game"
```

After packaging, run a project with the executable JAR:

```sh
__GL_THREADED_OPTIMIZATIONS=0 java --enable-native-access=ALL-UNNAMED -jar target/raster-0.1.0-SNAPSHOT.jar path/to/game
```

Raster also accepts a single Lua file as the entry point, and game arguments can
be separated with `--`:

```sh
raster path/to/game -- player-name hard-mode
```

Useful CLI options:

```sh
raster --help
raster --version
```

Build the project:

```sh
mvn package
```

This creates a self-contained, executable distribution JAR at
`target/raster-0.1.0-SNAPSHOT.jar`. It includes runtime dependencies and LWJGL
Linux natives, plus the LuaJIT JNI runtime and StyLua binary for Linux x64, so
it can be run without assembling a separate classpath.

## Lua Projects

Raster exposes a Love-like global namespace named `rs`:

```lua
function rs.load(args, rawargs)
  local major, minor, revision, codename = rs.getVersion()
  print("Raster", major, minor, revision, codename)
end

function rs.update(dt)
  if rs.keyboard.isDown("escape") then
    rs.window.close()
  end
end
```

Optional `conf.lua` files can configure the first window before `main.lua` is
loaded:

```lua
function rs.conf(t)
  t.identity = "mygame"
  t.window.title = "My Game"
  t.window.width = 1280
  t.window.height = 720
end
```

Initial modules include `rs.window`, `rs.timer`, `rs.system`, `rs.mouse`,
`rs.keyboard`, and `rs.filesystem`.

Raster enforces default StyLua formatting for all `.lua` files in the project
source directory before startup. The bundled StyLua binary runs in check mode
with config and EditorConfig discovery disabled, so `stylua.toml` and
`.editorconfig` files cannot change the required style.

## LuaJIT

Use `LuaJit` as an owned Lua state. It is synchronized, `AutoCloseable`, and
translates Lua load/runtime errors to `LuaJitException`.

```java
import dev.dvh.raster.lua.LuaJit;

try (LuaJit lua = LuaJit.create()) {
  lua.setGlobal("x", 41.0);
  lua.execute("x = x + 1; message = 'hello from luajit'", "=bootstrap");

  double x = lua.getGlobalNumber("x");
  String message = lua.getGlobalString("message");
}
```

The Maven build runs `scripts/build-luajit-jni.sh` during `process-classes`. It
compiles `src/main/c/raster_luajit.c` and packages the JNI bridge together with
the LuaJIT shared library from `lj2/lib`.

`lj2` is intentionally reduced to the files required to build and package the
binding:

- `lj2/include/luajit-2.1/*.h`, excluding the C++ `lua.hpp` header
- `lj2/lib/libluajit-5.1.so*`

Check formatting:

```sh
mvn validate
```

Apply formatting:

```sh
mvn spotless:apply
```

Formatting is enforced by Spotless during Maven's `validate` phase. Java sources
are formatted with `google-java-format`, C sources are formatted with
`clang-format` using the Google style, Markdown files are formatted with Flexmark,
and `pom.xml` is formatted with SortPom. Raster project Lua files are checked at
runtime with the embedded StyLua binary using StyLua's built-in default style.

Run the packaged jar:

```sh
__GL_THREADED_OPTIMIZATIONS=0 java --enable-native-access=ALL-UNNAMED -jar target/raster-0.1.0-SNAPSHOT.jar
```

## Stack

- Java 26
- Maven
- LWJGL 3
- JOML
- LuaJIT 2.1
- JNI

