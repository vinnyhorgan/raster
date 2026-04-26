package dev.dvh.raster.modules;

import dev.dvh.raster.lua.LuaJit;
import dev.dvh.raster.lua.LuaValue;

public final class TimerModule {

  private long start = System.nanoTime();
  private long previous = start;
  private double delta;
  private double averageDelta;
  private int frames;
  private long fpsWindow = start;
  private int fps;
  private int framesThisSecond;

  public void install(LuaJit lua) {
    lua.registerFunction("rs.timer.step", args -> LuaValue.array(LuaValue.number(step())));
    lua.registerFunction("rs.timer.getDelta", args -> LuaValue.array(LuaValue.number(delta)));
    lua.registerFunction("rs.timer.getTime", args -> LuaValue.array(LuaValue.number(getTime())));
    lua.registerFunction("rs.timer.getFPS", args -> LuaValue.array(LuaValue.number(fps)));
    lua.registerFunction(
        "rs.timer.getAverageDelta", args -> LuaValue.array(LuaValue.number(averageDelta)));
    lua.registerFunction(
        "rs.timer.sleep",
        args -> {
          sleep(args.length == 0 ? 0 : args[0].asNumber());
          return LuaValue.array();
        });
  }

  public double step() {
    long now = System.nanoTime();
    delta = (now - previous) / 1_000_000_000.0;
    previous = now;
    frames++;
    averageDelta += (delta - averageDelta) / frames;
    framesThisSecond++;
    if (now - fpsWindow >= 1_000_000_000L) {
      fps = framesThisSecond;
      framesThisSecond = 0;
      fpsWindow = now;
    }
    return delta;
  }

  public double getTime() {
    return (System.nanoTime() - start) / 1_000_000_000.0;
  }

  private static void sleep(double seconds) {
    if (seconds <= 0) {
      return;
    }
    try {
      Thread.sleep((long) (seconds * 1000.0));
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
