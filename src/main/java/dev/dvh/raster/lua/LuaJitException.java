package dev.dvh.raster.lua;

public final class LuaJitException extends RuntimeException {

  public LuaJitException(String message) {
    super(message);
  }

  public LuaJitException(String message, Throwable cause) {
    super(message, cause);
  }
}
