package dev.dvh.raster.lua;

@FunctionalInterface
public interface LuaFunction {

  LuaValue[] call(LuaValue[] arguments);
}
