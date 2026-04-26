package dev.dvh.raster.runtime;

import dev.dvh.raster.cli.CliOptions;
import dev.dvh.raster.lua.LuaJit;
import dev.dvh.raster.lua.LuaValue;
import dev.dvh.raster.modules.FilesystemModule;
import dev.dvh.raster.modules.KeyboardModule;
import dev.dvh.raster.modules.MouseModule;
import dev.dvh.raster.modules.SystemModule;
import dev.dvh.raster.modules.TimerModule;
import dev.dvh.raster.modules.WindowModule;
import dev.dvh.raster.tools.Selene;
import dev.dvh.raster.tools.Stylua;
import dev.dvh.raster.vfs.VirtualFileSystem;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Queue;

public final class RasterRuntime {

  private final CliOptions options;
  private final Queue<RasterEvent> events = new ArrayDeque<>();
  private final WindowModule window = new WindowModule();
  private final TimerModule timer = new TimerModule();

  public RasterRuntime(CliOptions options) {
    this.options = options;
  }

  public void run() {
    Selene.check(options.sourceDirectory());
    Stylua.check(options.sourceDirectory());
    VirtualFileSystem filesystem = new VirtualFileSystem(options.sourceDirectory());
    try (LuaJit lua = LuaJit.create()) {
      install(lua, filesystem);
      executeResource(lua, "rs.lua");
      executeResource(lua, "rs_callbacks.lua");
      executeResource(lua, "rs_boot.lua");
      LuaValue config = callBoot(lua);
      window.create(events, config.field("window"));
      lua.call(
          "rs.__runLoad",
          argumentTable(options.gameArguments()),
          argumentTable(options.rawArguments()));
      timer.step();
      loop(lua);
    } finally {
      window.close();
    }
  }

  private void loop(LuaJit lua) {
    boolean quitQueued = false;
    while (window.isOpen()) {
      window.poll();
      if (window.shouldClose() && !quitQueued) {
        events.add(new RasterEvent("quit", LuaValue.array()));
        quitQueued = true;
      }
      RasterEvent event;
      while ((event = events.poll()) != null) {
        LuaValue[] dispatchArgs = new LuaValue[event.arguments().length + 1];
        dispatchArgs[0] = LuaValue.string(event.name());
        System.arraycopy(event.arguments(), 0, dispatchArgs, 1, event.arguments().length);
        LuaValue[] result = lua.call("rs.__dispatch", dispatchArgs);
        if ("quit".equals(event.name())) {
          boolean canceled = result.length > 0 && result[0].isBoolean() && result[0].asBoolean();
          if (!canceled) {
            window.closeWindow();
          }
        }
      }
      double delta = timer.step();
      lua.call("rs.__update", LuaValue.number(delta));
      lua.call("rs.__draw");
      window.present();
      lua.call("rs.timer.sleep", LuaValue.number(0.001));
    }
  }

  private void install(LuaJit lua, VirtualFileSystem filesystem) {
    lua.registerFunction(
        "rs.getVersion",
        args ->
            LuaValue.array(
                LuaValue.number(0),
                LuaValue.number(1),
                LuaValue.number(0),
                LuaValue.string("bootstrap")));
    new FilesystemModule(filesystem).install(lua);
    preloadCompat53(lua);
    timer.install(lua);
    window.install(lua);
    new SystemModule().install(lua, window);
    new KeyboardModule().install(lua, window);
    new MouseModule().install(lua, window);
  }

  private void preloadCompat53(LuaJit lua) {
    String moduleSource = readResource("compat53/module.lua");
    String fileMtSource = readResource("compat53/file_mt.lua");
    String initSource = readResource("compat53/init.lua");
    lua.execute(
        "package.preload['compat53.module'] = load("
            + luaString(moduleSource)
            + ", '@compat53/module.lua')\n"
            + "package.preload['compat53.file_mt'] = load("
            + luaString(fileMtSource)
            + ", '@compat53/file_mt.lua')\n"
            + "package.preload['compat53'] = load("
            + luaString(initSource)
            + ", '@compat53/init.lua')\n"
            + "package.preload['compat53.init'] = package.preload['compat53']\n",
        "=(compat53 preload)");
  }

  private static String luaString(String raw) {
    return "\""
        + raw.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\0", "\\0")
        + "\"";
  }

  private static String readResource(String name) {
    String path = "/raster/lua/" + name;
    try (InputStream input = RasterRuntime.class.getResourceAsStream(path)) {
      if (input == null) {
        throw new IllegalStateException("Missing runtime Lua resource: " + path);
      }
      return new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new IllegalStateException("Unable to read runtime Lua resource: " + path, e);
    }
  }

  private LuaValue callBoot(LuaJit lua) {
    LuaValue[] result = lua.call("rs.boot", LuaValue.string(options.mainFile()));
    if (result.length == 0 || !result[0].isTable()) {
      return LuaValue.table(Map.of("window", defaultWindow()));
    }
    return result[0];
  }

  private static LuaValue argumentTable(java.util.List<String> arguments) {
    Map<String, LuaValue> table = new LinkedHashMap<>();
    for (int i = 0; i < arguments.size(); i++) {
      table.put(String.valueOf(i + 1), LuaValue.string(arguments.get(i)));
    }
    return LuaValue.table(table);
  }

  private static LuaValue defaultWindow() {
    Map<String, LuaValue> window = new LinkedHashMap<>();
    window.put("width", LuaValue.number(800));
    window.put("height", LuaValue.number(600));
    window.put("title", LuaValue.string("Raster"));
    window.put("visible", LuaValue.bool(true));
    window.put("vsync", LuaValue.bool(true));
    window.put("resizable", LuaValue.bool(true));
    return LuaValue.table(window);
  }

  private static void executeResource(LuaJit lua, String name) {
    String path = "/raster/lua/" + name;
    try (InputStream input = RasterRuntime.class.getResourceAsStream(path)) {
      if (input == null) {
        throw new IllegalStateException("Missing runtime Lua resource: " + path);
      }
      lua.execute(input.readAllBytes(), "@" + path);
    } catch (IOException e) {
      throw new IllegalStateException("Unable to read runtime Lua resource: " + path, e);
    }
  }
}
