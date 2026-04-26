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

public final class Selene {

  private static final String BINARY_RESOURCE = "/raster/tools/linux-x64/selene";
  private static final String STD_RESOURCE = "/raster/tools/raster.yml";
  private static final String CONFIG_CONTENT = "std = \"raster\"";
  private static Path executable;
  private static Path configDirectory;

  private Selene() {}

  public static void check(Path sourceDirectory) {
    List<Path> luaFiles = luaFiles(sourceDirectory);
    if (luaFiles.isEmpty()) {
      return;
    }
    Path selene = executable();
    Path config = config();
    for (int offset = 0; offset < luaFiles.size(); offset += 100) {
      int end = Math.min(offset + 100, luaFiles.size());
      run(selene, config, luaFiles.subList(offset, end));
    }
  }

  private static List<Path> luaFiles(Path sourceDirectory) {
    try (Stream<Path> paths = Files.walk(sourceDirectory)) {
      return paths
          .filter(Files::isRegularFile)
          .filter(path -> path.getFileName().toString().endsWith(".lua"))
          .map(path -> path.toAbsolutePath().normalize())
          .sorted()
          .toList();
    } catch (IOException e) {
      throw new IllegalStateException("Unable to scan Lua files for linting: " + e.getMessage(), e);
    }
  }

  private static void run(Path selene, Path config, List<Path> luaFiles) {
    List<String> command = new ArrayList<>();
    command.add(selene.toAbsolutePath().toString());
    command.add("--allow-warnings");
    command.add("--color");
    command.add("Never");
    command.add("--config");
    command.add(config.toAbsolutePath().toString());
    for (Path luaFile : luaFiles) {
      command.add(luaFile.toString());
    }

    try {
      Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
      String output = new String(process.getInputStream().readAllBytes(), UTF_8).trim();
      int exit = process.waitFor();
      if (exit != 0) {
        String message = output.isEmpty() ? "Lua lint check failed" : output;
        throw new IllegalStateException("Lua files must pass selene lint:\n" + message);
      }
    } catch (IOException e) {
      throw new IllegalStateException("Unable to run embedded selene: " + e.getMessage(), e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while running embedded selene", e);
    }
  }

  private static synchronized Path executable() {
    if (executable != null) {
      return executable;
    }
    requireLinuxX64();
    try {
      Path directory = Files.createTempDirectory("raster-selene-");
      directory.toFile().deleteOnExit();
      Path target = directory.resolve("selene");
      try (InputStream input = Selene.class.getResourceAsStream(BINARY_RESOURCE)) {
        if (input == null) {
          throw new IllegalStateException("Missing embedded selene resource: " + BINARY_RESOURCE);
        }
        Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
      }
      target.toFile().setExecutable(true, true);
      target.toFile().deleteOnExit();
      executable = target;
      return executable;
    } catch (IOException e) {
      throw new IllegalStateException("Unable to extract embedded selene: " + e.getMessage(), e);
    }
  }

  private static synchronized Path config() {
    if (configDirectory != null) {
      return configDirectory.resolve("selene.toml");
    }
    try {
      Path directory = Files.createTempDirectory("raster-selene-config-");
      directory.toFile().deleteOnExit();
      Path configFile = directory.resolve("selene.toml");
      Files.writeString(configFile, CONFIG_CONTENT);
      configFile.toFile().deleteOnExit();
      Path stdFile = directory.resolve("raster.yml");
      try (InputStream input = Selene.class.getResourceAsStream(STD_RESOURCE)) {
        if (input == null) {
          throw new IllegalStateException("Missing embedded selene std resource: " + STD_RESOURCE);
        }
        Files.copy(input, stdFile, StandardCopyOption.REPLACE_EXISTING);
      }
      stdFile.toFile().deleteOnExit();
      configDirectory = directory;
      return configFile;
    } catch (IOException e) {
      throw new IllegalStateException("Unable to create selene config: " + e.getMessage(), e);
    }
  }

  private static void requireLinuxX64() {
    String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
    String arch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
    boolean linux = os.contains("linux");
    boolean x64 = arch.equals("amd64") || arch.equals("x86_64");
    if (!linux || !x64) {
      throw new IllegalStateException("Bundled selene supports Linux x64 only");
    }
  }
}
