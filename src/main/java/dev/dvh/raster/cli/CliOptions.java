package dev.dvh.raster.cli;

import java.nio.file.Path;
import java.util.List;

public record CliOptions(
    boolean help,
    boolean version,
    boolean builtinDemo,
    Path sourceDirectory,
    String mainFile,
    List<String> gameArguments,
    List<String> rawArguments) {

  public static CliOptions forHelp() {
    return new CliOptions(true, false, false, null, null, List.of(), List.of());
  }

  public static CliOptions forVersion() {
    return new CliOptions(false, true, false, null, null, List.of(), List.of());
  }
}
