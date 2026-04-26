package dev.dvh.raster;

import dev.dvh.raster.cli.RasterCli;

public final class Main {

  private Main() {}

  public static void main(String[] args) {
    System.exit(RasterCli.run(args));
  }
}
