package dev.dvh.raster.modules;

import dev.dvh.raster.lua.LuaJit;
import dev.dvh.raster.lua.LuaValue;

public final class MouseModule {

  private boolean visible = true;
  private boolean grabbed;
  private boolean relativeMode;

  public void install(LuaJit lua, WindowModule window) {
    lua.registerFunction(
        "rs.mouse.getPosition",
        args -> {
          double[] position = window.mousePosition();
          return LuaValue.array(LuaValue.number(position[0]), LuaValue.number(position[1]));
        });
    lua.registerFunction(
        "rs.mouse.getX", args -> LuaValue.array(LuaValue.number(window.mousePosition()[0])));
    lua.registerFunction(
        "rs.mouse.getY", args -> LuaValue.array(LuaValue.number(window.mousePosition()[1])));
    lua.registerFunction(
        "rs.mouse.isDown",
        args ->
            LuaValue.array(
                LuaValue.bool(args.length > 0 && window.isMouseDown(args[0].asInt(1) - 1))));
    lua.registerFunction("rs.mouse.isVisible", args -> LuaValue.array(LuaValue.bool(visible)));
    lua.registerFunction(
        "rs.mouse.setVisible",
        args -> {
          visible = args.length == 0 || args[0].asBoolean();
          return LuaValue.array();
        });
    lua.registerFunction("rs.mouse.isGrabbed", args -> LuaValue.array(LuaValue.bool(grabbed)));
    lua.registerFunction(
        "rs.mouse.setGrabbed",
        args -> {
          grabbed = args.length > 0 && args[0].asBoolean();
          return LuaValue.array();
        });
    lua.registerFunction(
        "rs.mouse.getRelativeMode", args -> LuaValue.array(LuaValue.bool(relativeMode)));
    lua.registerFunction(
        "rs.mouse.setRelativeMode",
        args -> {
          relativeMode = args.length > 0 && args[0].asBoolean();
          return LuaValue.array();
        });
  }
}
