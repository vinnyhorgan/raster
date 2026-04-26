package dev.dvh.raster.modules;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.GLFW_CURSOR;
import static org.lwjgl.glfw.GLFW.GLFW_CURSOR_DISABLED;
import static org.lwjgl.glfw.GLFW.GLFW_CURSOR_HIDDEN;
import static org.lwjgl.glfw.GLFW.GLFW_CURSOR_NORMAL;
import static org.lwjgl.glfw.GLFW.GLFW_FALSE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;
import static org.lwjgl.glfw.GLFW.GLFW_REPEAT;
import static org.lwjgl.glfw.GLFW.GLFW_RESIZABLE;
import static org.lwjgl.glfw.GLFW.GLFW_TRUE;
import static org.lwjgl.glfw.GLFW.GLFW_VISIBLE;
import static org.lwjgl.glfw.GLFW.glfwCreateWindow;
import static org.lwjgl.glfw.GLFW.glfwDefaultWindowHints;
import static org.lwjgl.glfw.GLFW.glfwDestroyWindow;
import static org.lwjgl.glfw.GLFW.glfwGetClipboardString;
import static org.lwjgl.glfw.GLFW.glfwGetCursorPos;
import static org.lwjgl.glfw.GLFW.glfwGetKey;
import static org.lwjgl.glfw.GLFW.glfwGetMouseButton;
import static org.lwjgl.glfw.GLFW.glfwHideWindow;
import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwSetCharCallback;
import static org.lwjgl.glfw.GLFW.glfwSetClipboardString;
import static org.lwjgl.glfw.GLFW.glfwSetCursorPosCallback;
import static org.lwjgl.glfw.GLFW.glfwSetErrorCallback;
import static org.lwjgl.glfw.GLFW.glfwSetFramebufferSizeCallback;
import static org.lwjgl.glfw.GLFW.glfwSetInputMode;
import static org.lwjgl.glfw.GLFW.glfwSetKeyCallback;
import static org.lwjgl.glfw.GLFW.glfwSetMouseButtonCallback;
import static org.lwjgl.glfw.GLFW.glfwSetScrollCallback;
import static org.lwjgl.glfw.GLFW.glfwSetWindowShouldClose;
import static org.lwjgl.glfw.GLFW.glfwSetWindowSize;
import static org.lwjgl.glfw.GLFW.glfwSetWindowTitle;
import static org.lwjgl.glfw.GLFW.glfwShowWindow;
import static org.lwjgl.glfw.GLFW.glfwSwapBuffers;
import static org.lwjgl.glfw.GLFW.glfwSwapInterval;
import static org.lwjgl.glfw.GLFW.glfwTerminate;
import static org.lwjgl.glfw.GLFW.glfwWindowHint;
import static org.lwjgl.glfw.GLFW.glfwWindowShouldClose;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.system.MemoryUtil.NULL;

import dev.dvh.raster.lua.LuaJit;
import dev.dvh.raster.lua.LuaValue;
import dev.dvh.raster.runtime.RasterEvent;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Queue;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;

public final class WindowModule implements AutoCloseable {

  private long window;
  private boolean glfwStarted;
  private String title = "Raster";
  private int width = 800;
  private int height = 600;
  private boolean visible = true;
  private boolean vsync = true;
  private boolean keyRepeat = true;
  private boolean cursorVisible = true;
  private boolean cursorGrabbed;
  private boolean relativeMouseMode;
  private double mouseX;
  private double mouseY;
  private Queue<RasterEvent> events;

  public void install(LuaJit lua) {
    lua.registerFunction(
        "rs.window.setMode",
        args ->
            LuaValue.array(LuaValue.bool(setMode(args.length == 0 ? LuaValue.nil() : args[0]))));
    lua.registerFunction(
        "rs.window.updateMode",
        args ->
            LuaValue.array(LuaValue.bool(setMode(args.length == 0 ? LuaValue.nil() : args[0]))));
    lua.registerFunction(
        "rs.window.close",
        args -> {
          closeWindow();
          return LuaValue.array();
        });
    lua.registerFunction("rs.window.isOpen", args -> LuaValue.array(LuaValue.bool(isOpen())));
    lua.registerFunction(
        "rs.window.setTitle",
        args -> {
          setTitle(args.length == 0 ? "Raster" : args[0].asString());
          return LuaValue.array();
        });
    lua.registerFunction("rs.window.getTitle", args -> LuaValue.array(LuaValue.string(title)));
    lua.registerFunction("rs.window.getWidth", args -> LuaValue.array(LuaValue.number(width)));
    lua.registerFunction("rs.window.getHeight", args -> LuaValue.array(LuaValue.number(height)));
    lua.registerFunction(
        "rs.window.getDimensions",
        args -> LuaValue.array(LuaValue.number(width), LuaValue.number(height)));
    lua.registerFunction("rs.window.getMode", args -> LuaValue.array(modeTable()));
    lua.registerFunction(
        "rs.window.setVSync",
        args -> {
          setVSync(args.length == 0 || args[0].asBoolean());
          return LuaValue.array();
        });
  }

  public void create(Queue<RasterEvent> events, LuaValue config) {
    this.events = events;
    if (!glfwStarted) {
      GLFWErrorCallback.createPrint(System.err).set();
      if (!glfwInit()) {
        throw new IllegalStateException("Unable to initialize GLFW");
      }
      glfwStarted = true;
    }
    width = config.fieldInt("width", width);
    height = config.fieldInt("height", height);
    title = config.fieldString("title", title);
    visible = config.fieldBoolean("visible", visible);
    vsync = config.fieldBoolean("vsync", vsync);
    glfwDefaultWindowHints();
    glfwWindowHint(GLFW_VISIBLE, visible ? GLFW_TRUE : GLFW_FALSE);
    glfwWindowHint(GLFW_RESIZABLE, config.fieldBoolean("resizable", true) ? GLFW_TRUE : GLFW_FALSE);
    window = glfwCreateWindow(width, height, title, NULL, NULL);
    if (window == NULL) {
      throw new IllegalStateException("Failed to create the GLFW window");
    }
    installCallbacks();
    glfwMakeContextCurrent(window);
    glfwSwapInterval(vsync ? 1 : 0);
    GL.createCapabilities();
    double[] position = mousePosition();
    mouseX = position[0];
    mouseY = position[1];
    applyCursorMode();
    glClearColor(0.07f, 0.07f, 0.08f, 1.0f);
    if (visible) {
      glfwShowWindow(window);
    }
  }

  public boolean setMode(LuaValue options) {
    width = options.fieldInt("width", width);
    height = options.fieldInt("height", height);
    title = options.fieldString("title", title);
    visible = options.fieldBoolean("visible", visible);
    vsync = options.fieldBoolean("vsync", vsync);
    if (window != 0) {
      glfwSetWindowSize(window, width, height);
      glfwSetWindowTitle(window, title);
      glfwSwapInterval(vsync ? 1 : 0);
      if (visible) {
        glfwShowWindow(window);
      } else {
        glfwHideWindow(window);
      }
    }
    return true;
  }

  public void poll() {
    glfwPollEvents();
  }

  public void present() {
    glfwSwapBuffers(window);
  }

  public boolean shouldClose() {
    return window == 0 || glfwWindowShouldClose(window);
  }

  public void closeWindow() {
    if (window != 0) {
      glfwSetWindowShouldClose(window, true);
    }
  }

  public boolean isOpen() {
    return window != 0 && !glfwWindowShouldClose(window);
  }

  public int getWidth() {
    return width;
  }

  public int getHeight() {
    return height;
  }

  public String getClipboardText() {
    if (window == 0) {
      return "";
    }
    String value = glfwGetClipboardString(window);
    return value == null ? "" : value;
  }

  public void setClipboardText(String value) {
    if (window != 0) {
      glfwSetClipboardString(window, value == null ? "" : value);
    }
  }

  public boolean isKeyDown(int key) {
    return window != 0 && glfwGetKey(window, key) == GLFW_PRESS;
  }

  public boolean isMouseDown(int button) {
    return window != 0 && glfwGetMouseButton(window, button) == GLFW_PRESS;
  }

  public void setKeyRepeat(boolean keyRepeat) {
    this.keyRepeat = keyRepeat;
  }

  public boolean hasKeyRepeat() {
    return keyRepeat;
  }

  public void setMouseVisible(boolean visible) {
    cursorVisible = visible;
    applyCursorMode();
  }

  public boolean isMouseVisible() {
    return cursorVisible;
  }

  public void setMouseGrabbed(boolean grabbed) {
    cursorGrabbed = grabbed;
    applyCursorMode();
  }

  public boolean isMouseGrabbed() {
    return cursorGrabbed;
  }

  public void setRelativeMouseMode(boolean enabled) {
    relativeMouseMode = enabled;
    applyCursorMode();
  }

  public boolean isRelativeMouseMode() {
    return relativeMouseMode;
  }

  public double[] mousePosition() {
    double[] x = new double[1];
    double[] y = new double[1];
    if (window != 0) {
      glfwGetCursorPos(window, x, y);
    }
    return new double[] {x[0], y[0]};
  }

  @Override
  public void close() {
    if (window != 0) {
      glfwFreeCallbacks(window);
      glfwDestroyWindow(window);
      window = 0;
    }
    if (glfwStarted) {
      glfwTerminate();
      var callback = glfwSetErrorCallback(null);
      if (callback != null) {
        callback.free();
      }
      glfwStarted = false;
    }
  }

  public void setTitle(String title) {
    this.title = title == null ? "Raster" : title;
    if (window != 0) {
      glfwSetWindowTitle(window, this.title);
    }
  }

  private void setVSync(boolean enabled) {
    vsync = enabled;
    if (window != 0) {
      glfwSwapInterval(vsync ? 1 : 0);
    }
  }

  private void applyCursorMode() {
    if (window == 0) {
      return;
    }
    int mode = GLFW_CURSOR_NORMAL;
    if (relativeMouseMode || cursorGrabbed) {
      mode = GLFW_CURSOR_DISABLED;
    } else if (!cursorVisible) {
      mode = GLFW_CURSOR_HIDDEN;
    }
    glfwSetInputMode(window, GLFW_CURSOR, mode);
  }

  private LuaValue modeTable() {
    Map<String, LuaValue> table = new LinkedHashMap<>();
    table.put("width", LuaValue.number(width));
    table.put("height", LuaValue.number(height));
    table.put("title", LuaValue.string(title));
    table.put("visible", LuaValue.bool(visible));
    table.put("vsync", LuaValue.bool(vsync));
    return LuaValue.table(table);
  }

  private void installCallbacks() {
    glfwSetFramebufferSizeCallback(
        window,
        (window, width, height) -> {
          this.width = width;
          this.height = height;
          events.add(
              new RasterEvent(
                  "resize", LuaValue.array(LuaValue.number(width), LuaValue.number(height))));
        });
    glfwSetKeyCallback(
        window,
        (window, key, scancode, action, mods) -> {
          if (action == GLFW_PRESS || (action == GLFW_REPEAT && keyRepeat)) {
            events.add(
                new RasterEvent(
                    "keypressed",
                    LuaValue.array(
                        LuaValue.string(keyName(key)),
                        LuaValue.number(scancode),
                        LuaValue.bool(action == GLFW_REPEAT))));
          } else if (action == GLFW_RELEASE) {
            events.add(
                new RasterEvent(
                    "keyreleased",
                    LuaValue.array(LuaValue.string(keyName(key)), LuaValue.number(scancode))));
            if (key == GLFW_KEY_ESCAPE) {
              events.add(new RasterEvent("quit", LuaValue.array()));
            }
          }
        });
    glfwSetCharCallback(
        window,
        (window, codepoint) ->
            events.add(
                new RasterEvent(
                    "textinput",
                    LuaValue.array(LuaValue.string(new String(Character.toChars(codepoint)))))));
    glfwSetCursorPosCallback(
        window,
        (window, x, y) -> {
          double dx = x - mouseX;
          double dy = y - mouseY;
          mouseX = x;
          mouseY = y;
          events.add(
              new RasterEvent(
                  "mousemoved",
                  LuaValue.array(
                      LuaValue.number(x),
                      LuaValue.number(y),
                      LuaValue.number(dx),
                      LuaValue.number(dy))));
        });
    glfwSetMouseButtonCallback(
        window,
        (window, button, action, mods) -> {
          double[] position = mousePosition();
          String name = action == GLFW_RELEASE ? "mousereleased" : "mousepressed";
          events.add(
              new RasterEvent(
                  name,
                  LuaValue.array(
                      LuaValue.number(position[0]),
                      LuaValue.number(position[1]),
                      LuaValue.number(button + 1))));
        });
    glfwSetScrollCallback(
        window,
        (window, x, y) ->
            events.add(
                new RasterEvent(
                    "wheelmoved", LuaValue.array(LuaValue.number(x), LuaValue.number(y)))));
  }

  private static String keyName(int key) {
    return switch (key) {
      case GLFW_KEY_ESCAPE -> "escape";
      default ->
          key >= 32 && key <= 126 ? String.valueOf((char) Character.toLowerCase(key)) : "unknown";
    };
  }
}
