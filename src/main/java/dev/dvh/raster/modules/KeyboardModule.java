package dev.dvh.raster.modules;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_A;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_D;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_DOWN;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_S;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_UP;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_W;

import dev.dvh.raster.lua.LuaJit;
import dev.dvh.raster.lua.LuaValue;
import java.util.Map;

public final class KeyboardModule {

  private static final Map<String, Integer> KEYS =
      Map.ofEntries(
          Map.entry("escape", GLFW_KEY_ESCAPE),
          Map.entry("space", GLFW_KEY_SPACE),
          Map.entry("left", GLFW_KEY_LEFT),
          Map.entry("right", GLFW_KEY_RIGHT),
          Map.entry("up", GLFW_KEY_UP),
          Map.entry("down", GLFW_KEY_DOWN),
          Map.entry("w", GLFW_KEY_W),
          Map.entry("a", GLFW_KEY_A),
          Map.entry("s", GLFW_KEY_S),
          Map.entry("d", GLFW_KEY_D));

  private boolean keyRepeat = true;

  public void install(LuaJit lua, WindowModule window) {
    lua.registerFunction(
        "rs.keyboard.isDown",
        args ->
            LuaValue.array(LuaValue.bool(args.length > 0 && window.isKeyDown(keyCode(args[0])))));
    lua.registerFunction(
        "rs.keyboard.isScancodeDown",
        args ->
            LuaValue.array(LuaValue.bool(args.length > 0 && window.isKeyDown(args[0].asInt(-1)))));
    lua.registerFunction(
        "rs.keyboard.setKeyRepeat",
        args -> {
          keyRepeat = args.length == 0 || args[0].asBoolean();
          return LuaValue.array();
        });
    lua.registerFunction(
        "rs.keyboard.hasKeyRepeat", args -> LuaValue.array(LuaValue.bool(keyRepeat)));
  }

  private static int keyCode(LuaValue value) {
    String key = value.asString();
    if (key == null) {
      return -1;
    }
    if (key.length() == 1) {
      return Character.toUpperCase(key.charAt(0));
    }
    return KEYS.getOrDefault(key, -1);
  }
}
