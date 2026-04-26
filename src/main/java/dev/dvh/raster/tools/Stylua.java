package dev.dvh.raster.tools;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

public final class Stylua {

  private static final String RESOURCE = "/raster/tools/linux-x64/stylua";
  private static Path executable;

  private Stylua() {}

  public static void check(Path sourceDirectory) {
    List<Path> luaFiles = luaFiles(sourceDirectory);
    if (luaFiles.isEmpty()) {
      return;
    }
    Path stylua = executable();
    for (int offset = 0; offset < luaFiles.size(); offset += 100) {
      int end = Math.min(offset + 100, luaFiles.size());
      run(stylua, luaFiles.subList(offset, end));
    }
  }

  private static final List<String> EXCLUDED_DIRS =
      List.of("love", "lj2", "lua-compat-5.3", "src", "target", "scripts", "vendor");

  private static List<Path> luaFiles(Path sourceDirectory) {
    try (Stream<Path> paths = Files.walk(sourceDirectory)) {
      return paths
          .filter(Files::isRegularFile)
          .filter(path -> path.getFileName().toString().endsWith(".lua"))
          .filter(path -> !isUnderExcludedDir(sourceDirectory, path))
          .map(path -> path.toAbsolutePath().normalize())
          .sorted()
          .toList();
    } catch (IOException e) {
      throw new IllegalStateException(
          "Unable to scan Lua files for formatting: " + e.getMessage(), e);
    }
  }

  private static boolean isUnderExcludedDir(Path root, Path path) {
    Path rel = root.toAbsolutePath().relativize(path.toAbsolutePath());
    if (rel.getNameCount() < 2) {
      return false;
    }
    return EXCLUDED_DIRS.contains(rel.getName(0).toString());
  }

  private static void run(Path stylua, List<Path> luaFiles) {
    List<String> command = new ArrayList<>();
    command.add(stylua.toAbsolutePath().toString());
    command.add("--check");
    command.add("--color");
    command.add("Never");
    command.add("--no-editorconfig");
    command.add("--syntax");
    command.add("LuaJit");
    for (Path luaFile : luaFiles) {
      command.add(luaFile.toString());
    }

    try {
      Path workingDirectory = Files.createTempDirectory("raster-stylua-cwd-");
      workingDirectory.toFile().deleteOnExit();
      Process process =
          new ProcessBuilder(command)
              .directory(workingDirectory.toFile())
              .redirectErrorStream(true)
              .start();
      String output = new String(process.getInputStream().readAllBytes(), UTF_8).trim();
      int exit = process.waitFor();
      if (exit != 0) {
        String message = output.isEmpty() ? "Lua formatting check failed" : output;
        throw new IllegalStateException(
            "Lua files must use default StyLua formatting:\n" + message);
      }
    } catch (IOException e) {
      throw new IllegalStateException("Unable to run embedded StyLua: " + e.getMessage(), e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while running embedded StyLua", e);
    }
  }

  private static synchronized Path executable() {
    if (executable != null) {
      return executable;
    }
    requireLinuxX64();
    try {
      Path directory = Files.createTempDirectory("raster-stylua-");
      directory.toFile().deleteOnExit();
      Path target = directory.resolve("stylua");
      try (InputStream input = Stylua.class.getResourceAsStream(RESOURCE)) {
        if (input == null) {
          throw new IllegalStateException("Missing embedded StyLua resource: " + RESOURCE);
        }
        Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
      }
      target.toFile().setExecutable(true, true);
      target.toFile().deleteOnExit();
      executable = target;
      return executable;
    } catch (IOException e) {
      throw new IllegalStateException("Unable to extract embedded StyLua: " + e.getMessage(), e);
    }
  }

  private static void requireLinuxX64() {
    String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
    String arch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
    boolean linux = os.contains("linux");
    boolean x64 = arch.equals("amd64") || arch.equals("x86_64");
    if (!linux || !x64) {
      throw new IllegalStateException("Bundled StyLua supports Linux x64 only");
    }
  }
}
