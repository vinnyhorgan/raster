# Raster Agent Notes

Raster is a Java 26/Maven fantasy runtime that runs LuaJIT projects through a
Love-like global API named `rs`. The current implementation is a bootstrap
runtime: Java owns the process, window/event loop, filesystem sandbox, embedded
tools, LuaJIT bridge, and Lua resources that define the user-facing `rs` boot
and callback layer.

## Project Shape

- Entry point: `src/main/java/dev/dvh/raster/Main.java` delegates to
  `dev.dvh.raster.cli.RasterCli`.
- CLI parsing: `src/main/java/dev/dvh/raster/cli/RasterCli.java` accepts a
  project directory or single `.lua` file, starts a built-in demo when omitted,
  then separates game args after `--`.
- Runtime orchestration: `src/main/java/dev/dvh/raster/runtime/RasterRuntime.java`
  runs Selene and StyLua checks, creates the VFS, installs Java modules into
  Lua, loads embedded Lua resources, creates the window, runs `rs.load`, then
  drives update/draw/event dispatch.
- LuaJIT Java API: `src/main/java/dev/dvh/raster/lua/LuaJit.java`,
  `LuaValue.java`, `LuaFunction.java`, and `LuaJitException.java`.
- JNI bridge: `src/main/c/raster_luajit.c`, built by
  `scripts/build-luajit-jni.sh` during Maven `process-classes`.
- Runtime Lua layer: `src/main/resources/raster/lua/rs.lua`,
  `rs_boot.lua`, `rs_callbacks.lua`, `inspect.lua`, and bundled `compat53`.
- Java-backed modules: `src/main/java/dev/dvh/raster/modules/*Module.java`,
  including the initial OpenGL 1.1-style `GlModule` behind `rs.gl` and the
  diagnostic-only `DebugModule` behind `rs.debug`.
- VFS implementation: `src/main/java/dev/dvh/raster/vfs/VirtualFileSystem.java`.
- Embedded tools: `src/main/resources/raster/tools/linux-x64/stylua`, `selene`,
  and `raster/tools/raster.yml`.
- Vendored/reference trees such as `love/`, `lua-compat-5.3/`, and `lj2/` are
  dependency/reference material. Do not edit them unless the task explicitly
  calls for dependency work.

## What The Runtime Does Today

- A Raster project is a directory containing `main.lua`, or a single Lua file.
  Running with no project path launches the bundled hello demo in
  `src/main/resources/raster/demo`.
- Before startup, project Lua files are checked with embedded Selene and StyLua.
- `rs.boot` loads optional `conf.lua`, calls `rs.conf(config)` when present,
  sets the filesystem identity, and returns config to Java. After the OpenGL
  window exists, Java calls `rs.__loadMain(mainfile)` to load `main.lua` and
  snapshot callbacks with `rs.createhandlers`, allowing load errors to render
  in-window.
- Java creates a GLFW/OpenGL window from the returned config and then runs:
  `rs.__runLoad(args, rawargs)`, event dispatch through `rs.__dispatch`,
  `rs.__update(dt)`, `rs.__draw()`, buffer swap, and a small timer sleep.
- Rendering is fixed at a logical 640x480. `WindowModule` binds a 640x480 FBO
  before Lua drawing, then upscales it into the resizable OS window with
  aspect-preserving letterbox borders. Window size/resizability config is
  intentionally ignored; `rs.window.getDimensions()` reports 640x480. The OS
  window minimum size is 320x240, and Alt+Enter toggles fullscreen.
- Current exposed modules include `rs.window`, `rs.timer`, `rs.system`,
  `rs.mouse`, `rs.keyboard`, `rs.filesystem`, `rs.gl`, and `rs.debug`.
- `rs.gl` currently implements the immediate-mode foundation with modern
  OpenGL objects: shader program, VAO/VBO upload, color/vertex calls, matrix
  stacks, clear/viewport, and common depth/blend/cull state. It is not a full
  OpenGL 1.1 compatibility layer yet.
- `rs.debug.print`/`rs.debug.text` render diagnostic Inter text using LWJGL STB
  TrueType packed glyph atlases. Text rendering is intentionally not a core
  fantasy API.
- The VFS reads from mounted project roots first and the save directory second;
  writes always go to the save directory under XDG data or `~/.local/share`.

## Build And Verification

- Requirements from `README.md`: Java 26, Maven 3.9+, GCC, and
  `clang-format` 22.1.3.
- Main verification command: `mvn validate` for Spotless formatting checks.
- Compile/package command: `mvn package`. This also builds and packages the
  LuaJIT JNI bridge.
- Runtime/dev command: `mvn process-classes exec:exec -Dexec.args="path/to/game"`.
- Packaged run command needs native access:
  `__GL_THREADED_OPTIMIZATIONS=0 java --enable-native-access=ALL-UNNAMED -jar target/raster-0.1.0-SNAPSHOT.jar path/to/game`.
- Omit the project path in the packaged run command to launch the built-in hello
  demo.
- Linux x64 is currently assumed for bundled LWJGL natives, LuaJIT, StyLua, and
  Selene. Cross-platform behavior is not implemented yet.
- There is currently no `src/test` tree; if you add behavior, prefer adding
  focused tests when practical instead of relying only on manual runs.

## Coding Rules

- Keep changes small and local. This is a bootstrap runtime, not a mature API.
- Preserve the Love-like shape of `rs` unless the user explicitly chooses a
  different API direction.
- Java is formatted with google-java-format through Spotless; C is formatted by
  clang-format Google style; Markdown and `pom.xml` are also checked by
  Spotless.
- Use C11 for `src/main/c` code.
- Do not add compatibility shims or public API aliases unless there is a clear
  need. Ask if unsure.
- Avoid broad edits in vendored/reference directories.
- Be careful with native resources and Maven phases. `process-classes` is where
  `libraster_luajit.so` and LuaJIT are copied into `target/classes/natives`.

## Known Risk Areas

- `LuaJit.call` builds `return <expression>` and executes it. Only pass trusted
  internal expressions or change the API before exposing it to user input.
- Keyboard key naming is intentionally minimal. Only a small set of named keys
  and printable ASCII are implemented.
- `rs.gl` does not yet implement texture objects, lighting/material equations,
  display lists, evaluators, pixel-transfer operations, client arrays, or full
  OpenGL error-state behavior.

## Review Checklist For Future Agents

- Inspect `RasterRuntime` first for lifecycle changes.
- Inspect `rs.lua`, `rs_boot.lua`, and `rs_callbacks.lua` when behavior crosses
  Java/Lua boundaries.
- Inspect `LuaJit.java` and `raster_luajit.c` together for bridge changes.
- Inspect `VirtualFileSystem` and `FilesystemModule` together for file behavior.
- Inspect `GlModule` and `ogl_1_1.md` together for OpenGL compatibility work.
- Run `mvn validate` after source edits when dependencies and Java 26 are
  available.
- Run `mvn package` when JNI, Maven build config, native resources, or packaging
  behavior changes.

