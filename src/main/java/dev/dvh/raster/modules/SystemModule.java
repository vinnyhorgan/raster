package dev.dvh.raster.modules;

import dev.dvh.raster.lua.LuaJit;
import dev.dvh.raster.lua.LuaValue;
import java.awt.Desktop;
import java.net.URI;
import java.util.Locale;

public final class SystemModule {

  public void install(LuaJit lua, WindowModule window) {
    lua.registerFunction("rs.system.getOS", args -> LuaValue.array(LuaValue.string(osName())));
    lua.registerFunction(
        "rs.system.getProcessorCount",
        args -> LuaValue.array(LuaValue.number(Runtime.getRuntime().availableProcessors())));
    lua.registerFunction(
        "rs.system.getMemorySize",
        args -> LuaValue.array(LuaValue.number(Runtime.getRuntime().maxMemory())));
    lua.registerFunction(
        "rs.system.getClipboardText",
        args -> LuaValue.array(LuaValue.string(window.getClipboardText())));
    lua.registerFunction(
        "rs.system.setClipboardText",
        args -> {
          window.setClipboardText(args.length == 0 ? "" : args[0].asString());
          return LuaValue.array();
        });
    lua.registerFunction(
        "rs.system.openURL",
        args ->
            LuaValue.array(LuaValue.bool(openUrl(args.length == 0 ? null : args[0].asString()))));
  }

  private static String osName() {
    String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
    if (os.contains("linux")) {
      return "Linux";
    }
    if (os.contains("mac")) {
      return "OS X";
    }
    if (os.contains("win")) {
      return "Windows";
    }
    return System.getProperty("os.name", "Unknown");
  }

  private static boolean openUrl(String value) {
    if (value == null || !Desktop.isDesktopSupported()) {
      return false;
    }
    try {
      Desktop.getDesktop().browse(URI.create(value));
      return true;
    } catch (RuntimeException | java.io.IOException e) {
      return false;
    }
  }
}
