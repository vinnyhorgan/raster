package dev.dvh.raster.runtime;

import dev.dvh.raster.lua.LuaValue;

public record RasterEvent(String name, LuaValue[] arguments) {}
