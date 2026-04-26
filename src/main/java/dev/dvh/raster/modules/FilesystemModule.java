package dev.dvh.raster.modules;

import dev.dvh.raster.lua.LuaJit;
import dev.dvh.raster.lua.LuaValue;
import dev.dvh.raster.vfs.VirtualFileSystem;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class FilesystemModule {

  private final VirtualFileSystem filesystem;

  public FilesystemModule(VirtualFileSystem filesystem) {
    this.filesystem = filesystem;
  }

  public void install(LuaJit lua) {
    lua.registerFunction("rs.filesystem.getSource", args -> path(filesystem.sourceDirectory()));
    lua.registerFunction(
        "rs.filesystem.getIdentity",
        args -> LuaValue.array(LuaValue.string(filesystem.identity())));
    lua.registerFunction(
        "rs.filesystem.setIdentity",
        args -> {
          if (args.length > 0) {
            filesystem.setIdentity(args[0].asString());
          }
          return LuaValue.array();
        });
    lua.registerFunction(
        "rs.filesystem.getSaveDirectory", args -> path(filesystem.saveDirectory()));
    lua.registerFunction(
        "rs.filesystem.getWorkingDirectory", args -> path(filesystem.workingDirectory()));
    lua.registerFunction(
        "rs.filesystem.getUserDirectory", args -> path(filesystem.userDirectory()));
    lua.registerFunction(
        "rs.filesystem.getAppdataDirectory", args -> path(filesystem.appDataDirectory()));
    lua.registerFunction(
        "rs.filesystem.exists",
        args ->
            LuaValue.array(
                LuaValue.bool(args.length > 0 && filesystem.exists(args[0].asString()))));
    lua.registerFunction(
        "rs.filesystem.read",
        args ->
            LuaValue.array(
                LuaValue.string(args.length == 0 ? null : filesystem.read(args[0].asString()))));
    lua.registerFunction(
        "rs.filesystem.write",
        args -> {
          filesystem.write(args[0].asString(), args.length > 1 ? args[1].asString() : "", false);
          return LuaValue.array(LuaValue.bool(true));
        });
    lua.registerFunction(
        "rs.filesystem.append",
        args -> {
          filesystem.write(args[0].asString(), args.length > 1 ? args[1].asString() : "", true);
          return LuaValue.array(LuaValue.bool(true));
        });
    lua.registerFunction(
        "rs.filesystem.createDirectory",
        args ->
            LuaValue.array(
                LuaValue.bool(args.length > 0 && filesystem.createDirectory(args[0].asString()))));
    lua.registerFunction(
        "rs.filesystem.remove",
        args ->
            LuaValue.array(
                LuaValue.bool(args.length > 0 && filesystem.remove(args[0].asString()))));
    lua.registerFunction(
        "rs.filesystem.getDirectoryItems",
        args -> directoryItems(args.length == 0 ? "" : args[0].asString()));
    lua.registerFunction(
        "rs.filesystem.getInfo", args -> info(args.length == 0 ? "" : args[0].asString()));
    lua.registerFunction(
        "rs.filesystem.getRequirePath",
        args -> LuaValue.array(LuaValue.string(filesystem.requirePath())));
    lua.registerFunction(
        "rs.filesystem.setRequirePath",
        args -> {
          if (args.length > 0) {
            filesystem.setRequirePath(args[0].asString());
          }
          return LuaValue.array();
        });
    lua.registerFunction(
        "rs.filesystem.mount",
        args ->
            LuaValue.array(
                LuaValue.bool(args.length > 0 && filesystem.mount(Path.of(args[0].asString())))));
    lua.registerFunction(
        "rs.filesystem.unmount",
        args ->
            LuaValue.array(
                LuaValue.bool(args.length > 0 && filesystem.unmount(Path.of(args[0].asString())))));
    lua.registerFunction(
        "rs.__readFile",
        args ->
            LuaValue.array(
                LuaValue.string(args.length == 0 ? null : filesystem.read(args[0].asString()))));
    lua.registerFunction(
        "rs.__findRequire",
        args ->
            LuaValue.array(
                LuaValue.string(
                    args.length == 0 ? null : filesystem.findRequire(args[0].asString()))));
  }

  private static LuaValue[] path(Path path) {
    return LuaValue.array(LuaValue.string(path.toString()));
  }

  private LuaValue[] directoryItems(String virtualPath) {
    Map<String, LuaValue> table = new LinkedHashMap<>();
    int index = 1;
    for (String item : filesystem.directoryItems(virtualPath)) {
      table.put(String.valueOf(index++), LuaValue.string(item));
    }
    return LuaValue.array(LuaValue.table(table));
  }

  private LuaValue[] info(String virtualPath) {
    Map<String, String> info = filesystem.info(virtualPath);
    if (info.isEmpty()) {
      return LuaValue.array(LuaValue.nil());
    }
    Map<String, LuaValue> table = new LinkedHashMap<>();
    table.put("type", LuaValue.string(info.get("type")));
    table.put("size", LuaValue.number(Double.parseDouble(info.get("size"))));
    table.put("modtime", LuaValue.number(Double.parseDouble(info.get("modtime"))));
    table.put("readonly", LuaValue.bool(Boolean.parseBoolean(info.get("readonly"))));
    return LuaValue.array(LuaValue.table(table));
  }
}
