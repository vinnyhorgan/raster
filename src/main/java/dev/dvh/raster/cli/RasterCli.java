package dev.dvh.raster.cli;

import dev.dvh.raster.runtime.RasterRuntime;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class RasterCli {

  public static final String VERSION = "0.1.0-bootstrap";

  private RasterCli() {}

  public static int run(String[] args) {
    CliOptions options;
    try {
      options = parse(args);
    } catch (IllegalArgumentException e) {
      System.err.println("raster: " + e.getMessage());
      System.err.println("Try 'raster --help'.");
      return 2;
    }
    if (options.help()) {
      printHelp();
      return 0;
    }
    if (options.version()) {
      System.out.println("Raster " + VERSION);
      return 0;
    }
    try {
      new RasterRuntime(options).run();
      return 0;
    } catch (RuntimeException e) {
      System.err.println("raster: " + e.getMessage());
      return 1;
    }
  }

  private static CliOptions parse(String[] args) {
    List<String> all = Arrays.asList(args);
    if (all.contains("--help") || all.contains("-h")) {
      return CliOptions.forHelp();
    }
    if (all.contains("--version") || all.contains("-v")) {
      return CliOptions.forVersion();
    }

    List<String> engine = new ArrayList<>();
    List<String> game = new ArrayList<>();
    boolean gameArgs = false;
    for (String arg : args) {
      if (gameArgs) {
        game.add(arg);
      } else if ("--".equals(arg)) {
        gameArgs = true;
      } else {
        engine.add(arg);
      }
    }
    if (engine.size() > 1) {
      throw new IllegalArgumentException("expected at most one project path");
    }

    if (engine.isEmpty()) {
      return new CliOptions(
          false, false, true, null, "main.lua", List.copyOf(game), List.copyOf(all));
    }

    Path input = Path.of(engine.getFirst());
    Path absolute = input.toAbsolutePath().normalize();
    Path source;
    String main;
    if (Files.isDirectory(absolute)) {
      source = absolute;
      main = "main.lua";
    } else if (absolute.getFileName() != null
        && absolute.getFileName().toString().endsWith(".lua")) {
      source = absolute.getParent() == null ? Path.of("").toAbsolutePath() : absolute.getParent();
      main = absolute.getFileName().toString();
    } else {
      throw new IllegalArgumentException("project path must be a directory or .lua file: " + input);
    }
    if (!Files.exists(source.resolve(main))) {
      throw new IllegalArgumentException("missing " + source.resolve(main));
    }
    return new CliOptions(false, false, false, source, main, List.copyOf(game), List.copyOf(all));
  }

  private static void printHelp() {
    System.out.println(
        "Usage: raster [path] [-- game args...]\n"
            + "\n"
            + "Runs a Raster project. If path is omitted, Raster starts a built-in demo.\n"
            + "A project path may be a directory containing main.lua or a single .lua file.\n"
            + "\n"
            + "Options:\n"
            + "  -h, --help      Show this help\n"
            + "  -v, --version   Show Raster version");
  }
}
