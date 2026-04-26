package dev.dvh.raster.modules;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.GLFW_CURSOR;
import static org.lwjgl.glfw.GLFW.GLFW_CURSOR_DISABLED;
import static org.lwjgl.glfw.GLFW.GLFW_CURSOR_HIDDEN;
import static org.lwjgl.glfw.GLFW.GLFW_CURSOR_NORMAL;
import static org.lwjgl.glfw.GLFW.GLFW_DONT_CARE;
import static org.lwjgl.glfw.GLFW.GLFW_FALSE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER;
import static org.lwjgl.glfw.GLFW.GLFW_MOD_ALT;
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
import static org.lwjgl.glfw.GLFW.glfwGetFramebufferSize;
import static org.lwjgl.glfw.GLFW.glfwGetKey;
import static org.lwjgl.glfw.GLFW.glfwGetMouseButton;
import static org.lwjgl.glfw.GLFW.glfwGetPrimaryMonitor;
import static org.lwjgl.glfw.GLFW.glfwGetVideoMode;
import static org.lwjgl.glfw.GLFW.glfwGetWindowPos;
import static org.lwjgl.glfw.GLFW.glfwGetWindowSize;
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
import static org.lwjgl.glfw.GLFW.glfwSetWindowMonitor;
import static org.lwjgl.glfw.GLFW.glfwSetWindowShouldClose;
import static org.lwjgl.glfw.GLFW.glfwSetWindowSizeLimits;
import static org.lwjgl.glfw.GLFW.glfwSetWindowTitle;
import static org.lwjgl.glfw.GLFW.glfwShowWindow;
import static org.lwjgl.glfw.GLFW.glfwSwapBuffers;
import static org.lwjgl.glfw.GLFW.glfwSwapInterval;
import static org.lwjgl.glfw.GLFW.glfwTerminate;
import static org.lwjgl.glfw.GLFW.glfwWindowHint;
import static org.lwjgl.glfw.GLFW.glfwWindowShouldClose;
import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_LINEAR;
import static org.lwjgl.opengl.GL11.GL_RGBA;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL11.glDeleteTextures;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL11.glGenTextures;
import static org.lwjgl.opengl.GL11.glTexImage2D;
import static org.lwjgl.opengl.GL11.glTexParameteri;
import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glDeleteBuffers;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL20.GL_COMPILE_STATUS;
import static org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20.GL_LINK_STATUS;
import static org.lwjgl.opengl.GL20.GL_VERTEX_SHADER;
import static org.lwjgl.opengl.GL20.glAttachShader;
import static org.lwjgl.opengl.GL20.glCompileShader;
import static org.lwjgl.opengl.GL20.glCreateProgram;
import static org.lwjgl.opengl.GL20.glCreateShader;
import static org.lwjgl.opengl.GL20.glDeleteProgram;
import static org.lwjgl.opengl.GL20.glDeleteShader;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glGetProgramInfoLog;
import static org.lwjgl.opengl.GL20.glGetProgrami;
import static org.lwjgl.opengl.GL20.glGetShaderInfoLog;
import static org.lwjgl.opengl.GL20.glGetShaderi;
import static org.lwjgl.opengl.GL20.glGetUniformLocation;
import static org.lwjgl.opengl.GL20.glLinkProgram;
import static org.lwjgl.opengl.GL20.glShaderSource;
import static org.lwjgl.opengl.GL20.glUniform1i;
import static org.lwjgl.opengl.GL20.glUseProgram;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.GL_COLOR_ATTACHMENT0;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER_COMPLETE;
import static org.lwjgl.opengl.GL30.GL_RENDERBUFFER;
import static org.lwjgl.opengl.GL30.glBindFramebuffer;
import static org.lwjgl.opengl.GL30.glBindRenderbuffer;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glCheckFramebufferStatus;
import static org.lwjgl.opengl.GL30.glDeleteFramebuffers;
import static org.lwjgl.opengl.GL30.glDeleteRenderbuffers;
import static org.lwjgl.opengl.GL30.glDeleteVertexArrays;
import static org.lwjgl.opengl.GL30.glFramebufferRenderbuffer;
import static org.lwjgl.opengl.GL30.glFramebufferTexture2D;
import static org.lwjgl.opengl.GL30.glGenFramebuffers;
import static org.lwjgl.opengl.GL30.glGenRenderbuffers;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;
import static org.lwjgl.opengl.GL30.glRenderbufferStorage;
import static org.lwjgl.system.MemoryUtil.NULL;

import dev.dvh.raster.lua.LuaJit;
import dev.dvh.raster.lua.LuaValue;
import dev.dvh.raster.runtime.RasterEvent;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Queue;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;

public final class WindowModule implements AutoCloseable {

  public static final int LOGICAL_WIDTH = 640;
  public static final int LOGICAL_HEIGHT = 480;
  public static final int MIN_WINDOW_WIDTH = 320;
  public static final int MIN_WINDOW_HEIGHT = 240;

  private static final int STRIDE = 4 * Float.BYTES;

  private long window;
  private boolean glfwStarted;
  private String title = "Raster";
  private int width = LOGICAL_WIDTH;
  private int height = LOGICAL_HEIGHT;
  private int framebufferWidth = LOGICAL_WIDTH;
  private int framebufferHeight = LOGICAL_HEIGHT;
  private int windowedX;
  private int windowedY;
  private int windowedWidth = LOGICAL_WIDTH;
  private int windowedHeight = LOGICAL_HEIGHT;
  private boolean visible = true;
  private boolean vsync = true;
  private boolean fullscreen;
  private boolean keyRepeat = true;
  private boolean cursorVisible = true;
  private boolean cursorGrabbed;
  private boolean relativeMouseMode;
  private double mouseX;
  private double mouseY;
  private Queue<RasterEvent> events;
  private int framebuffer;
  private int colorTexture;
  private int depthRenderbuffer;
  private int presentVao;
  private int presentVbo;
  private int presentProgram;

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
    width = LOGICAL_WIDTH;
    height = LOGICAL_HEIGHT;
    title = config.fieldString("title", title);
    visible = config.fieldBoolean("visible", visible);
    vsync = config.fieldBoolean("vsync", vsync);
    glfwDefaultWindowHints();
    glfwWindowHint(GLFW_VISIBLE, visible ? GLFW_TRUE : GLFW_FALSE);
    glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
    window = glfwCreateWindow(width, height, title, NULL, NULL);
    if (window == NULL) {
      throw new IllegalStateException("Failed to create the GLFW window");
    }
    glfwSetWindowSizeLimits(
        window, MIN_WINDOW_WIDTH, MIN_WINDOW_HEIGHT, GLFW_DONT_CARE, GLFW_DONT_CARE);
    installCallbacks();
    glfwMakeContextCurrent(window);
    glfwSwapInterval(vsync ? 1 : 0);
    GL.createCapabilities();
    updateFramebufferSize();
    createCanvas();
    double[] position = rawMousePosition();
    mouseX = position[0];
    mouseY = position[1];
    applyCursorMode();
    glClearColor(0.07f, 0.07f, 0.08f, 1.0f);
    if (visible) {
      glfwShowWindow(window);
    }
  }

  public boolean setMode(LuaValue options) {
    title = options.fieldString("title", title);
    visible = options.fieldBoolean("visible", visible);
    vsync = options.fieldBoolean("vsync", vsync);
    if (window != 0) {
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
    presentCanvas();
    glfwSwapBuffers(window);
  }

  public void beginFrame() {
    glBindFramebuffer(GL_FRAMEBUFFER, framebuffer);
    glViewport(0, 0, LOGICAL_WIDTH, LOGICAL_HEIGHT);
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
    double[] position = rawMousePosition();
    return logicalMouse(position[0], position[1]);
  }

  private double[] rawMousePosition() {
    double[] x = new double[1];
    double[] y = new double[1];
    if (window != 0) {
      glfwGetCursorPos(window, x, y);
    }
    return new double[] {x[0], y[0]};
  }

  private double[] logicalMouse(double x, double y) {
    double[] viewport = canvasViewport();
    double logicalX = (x - viewport[0]) / viewport[2] * LOGICAL_WIDTH;
    double logicalY = (y - viewport[1]) / viewport[3] * LOGICAL_HEIGHT;
    return new double[] {logicalX, logicalY};
  }

  private double[] logicalDelta(double dx, double dy) {
    double[] viewport = canvasViewport();
    return new double[] {dx / viewport[2] * LOGICAL_WIDTH, dy / viewport[3] * LOGICAL_HEIGHT};
  }

  private double[] canvasViewport() {
    double scale =
        Math.min(
            framebufferWidth / (double) LOGICAL_WIDTH, framebufferHeight / (double) LOGICAL_HEIGHT);
    double viewportWidth = LOGICAL_WIDTH * scale;
    double viewportHeight = LOGICAL_HEIGHT * scale;
    double viewportX = (framebufferWidth - viewportWidth) * 0.5;
    double viewportY = (framebufferHeight - viewportHeight) * 0.5;
    return new double[] {viewportX, viewportY, viewportWidth, viewportHeight};
  }

  private void updateFramebufferSize() {
    int[] width = new int[1];
    int[] height = new int[1];
    glfwGetFramebufferSize(window, width, height);
    framebufferWidth = Math.max(1, width[0]);
    framebufferHeight = Math.max(1, height[0]);
  }

  @Override
  public void close() {
    closeCanvas();
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
    table.put("fullscreen", LuaValue.bool(fullscreen));
    return LuaValue.table(table);
  }

  private void toggleFullscreen() {
    if (window == 0) {
      return;
    }
    if (fullscreen) {
      glfwSetWindowMonitor(
          window, NULL, windowedX, windowedY, windowedWidth, windowedHeight, GLFW_DONT_CARE);
      glfwSetWindowSizeLimits(
          window, MIN_WINDOW_WIDTH, MIN_WINDOW_HEIGHT, GLFW_DONT_CARE, GLFW_DONT_CARE);
      fullscreen = false;
    } else {
      int[] x = new int[1];
      int[] y = new int[1];
      int[] width = new int[1];
      int[] height = new int[1];
      glfwGetWindowPos(window, x, y);
      glfwGetWindowSize(window, width, height);
      windowedX = x[0];
      windowedY = y[0];
      windowedWidth = Math.max(MIN_WINDOW_WIDTH, width[0]);
      windowedHeight = Math.max(MIN_WINDOW_HEIGHT, height[0]);

      long monitor = glfwGetPrimaryMonitor();
      if (monitor == NULL) {
        return;
      }
      var mode = glfwGetVideoMode(monitor);
      if (mode == null) {
        return;
      }
      glfwSetWindowMonitor(window, monitor, 0, 0, mode.width(), mode.height(), mode.refreshRate());
      fullscreen = true;
    }
    glfwSwapInterval(vsync ? 1 : 0);
    updateFramebufferSize();
  }

  private void createCanvas() {
    framebuffer = glGenFramebuffers();
    colorTexture = glGenTextures();
    depthRenderbuffer = glGenRenderbuffers();

    glBindTexture(GL_TEXTURE_2D, colorTexture);
    glTexImage2D(
        GL_TEXTURE_2D, 0, GL_RGBA, LOGICAL_WIDTH, LOGICAL_HEIGHT, 0, GL_RGBA, GL_UNSIGNED_BYTE, 0L);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D, org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    glTexParameteri(GL_TEXTURE_2D, org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

    glBindRenderbuffer(GL_RENDERBUFFER, depthRenderbuffer);
    glRenderbufferStorage(
        GL_RENDERBUFFER, org.lwjgl.opengl.GL14.GL_DEPTH_COMPONENT24, LOGICAL_WIDTH, LOGICAL_HEIGHT);

    glBindFramebuffer(GL_FRAMEBUFFER, framebuffer);
    glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, colorTexture, 0);
    glFramebufferRenderbuffer(
        GL_FRAMEBUFFER,
        org.lwjgl.opengl.GL30.GL_DEPTH_ATTACHMENT,
        GL_RENDERBUFFER,
        depthRenderbuffer);
    if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
      throw new IllegalStateException("Unable to create 640x480 Raster canvas framebuffer");
    }
    glBindFramebuffer(GL_FRAMEBUFFER, 0);
    glBindTexture(GL_TEXTURE_2D, 0);
    glBindRenderbuffer(GL_RENDERBUFFER, 0);

    presentProgram = createPresentProgram();
    presentVao = glGenVertexArrays();
    presentVbo = glGenBuffers();
    float[] vertices = {
      -1, -1, 0, 0,
      1, -1, 1, 0,
      1, 1, 1, 1,
      -1, -1, 0, 0,
      1, 1, 1, 1,
      -1, 1, 0, 1
    };
    var data = BufferUtils.createFloatBuffer(vertices.length);
    data.put(vertices).flip();
    glBindVertexArray(presentVao);
    glBindBuffer(GL_ARRAY_BUFFER, presentVbo);
    glBufferData(GL_ARRAY_BUFFER, data, GL_STATIC_DRAW);
    glVertexAttribPointer(0, 2, GL_FLOAT, false, STRIDE, 0);
    glEnableVertexAttribArray(0);
    glVertexAttribPointer(1, 2, GL_FLOAT, false, STRIDE, 2L * Float.BYTES);
    glEnableVertexAttribArray(1);
    glBindBuffer(GL_ARRAY_BUFFER, 0);
    glBindVertexArray(0);
  }

  private void presentCanvas() {
    glBindFramebuffer(GL_FRAMEBUFFER, 0);
    updateFramebufferSize();
    double[] viewport = canvasViewport();
    glViewport(0, 0, framebufferWidth, framebufferHeight);
    glClearColor(0, 0, 0, 1);
    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    glDisable(GL_DEPTH_TEST);
    glDisable(GL_BLEND);
    glViewport(
        (int) Math.round(viewport[0]),
        (int) Math.round(viewport[1]),
        (int) Math.round(viewport[2]),
        (int) Math.round(viewport[3]));
    glActiveTexture(GL_TEXTURE0);
    glUseProgram(presentProgram);
    glBindTexture(GL_TEXTURE_2D, colorTexture);
    glBindVertexArray(presentVao);
    glDrawArrays(GL_TRIANGLES, 0, 6);
    glBindVertexArray(0);
    glBindTexture(GL_TEXTURE_2D, 0);
    glUseProgram(0);
    glViewport(0, 0, LOGICAL_WIDTH, LOGICAL_HEIGHT);
  }

  private void closeCanvas() {
    if (presentProgram != 0) {
      glDeleteProgram(presentProgram);
      presentProgram = 0;
    }
    if (presentVbo != 0) {
      glDeleteBuffers(presentVbo);
      presentVbo = 0;
    }
    if (presentVao != 0) {
      glDeleteVertexArrays(presentVao);
      presentVao = 0;
    }
    if (depthRenderbuffer != 0) {
      glDeleteRenderbuffers(depthRenderbuffer);
      depthRenderbuffer = 0;
    }
    if (colorTexture != 0) {
      glDeleteTextures(colorTexture);
      colorTexture = 0;
    }
    if (framebuffer != 0) {
      glDeleteFramebuffers(framebuffer);
      framebuffer = 0;
    }
  }

  private static int createPresentProgram() {
    int vertex = compile(GL_VERTEX_SHADER, presentVertexShader());
    int fragment = compile(GL_FRAGMENT_SHADER, presentFragmentShader());
    int program = glCreateProgram();
    glAttachShader(program, vertex);
    glAttachShader(program, fragment);
    glLinkProgram(program);
    if (glGetProgrami(program, GL_LINK_STATUS) == 0) {
      throw new IllegalStateException(
          "Unable to link Raster canvas shader: " + glGetProgramInfoLog(program));
    }
    glDeleteShader(vertex);
    glDeleteShader(fragment);
    glUseProgram(program);
    glUniform1i(glGetUniformLocation(program, "uTexture"), 0);
    glUseProgram(0);
    return program;
  }

  private static int compile(int type, String source) {
    int shader = glCreateShader(type);
    glShaderSource(shader, source);
    glCompileShader(shader);
    if (glGetShaderi(shader, GL_COMPILE_STATUS) == 0) {
      throw new IllegalStateException(
          "Unable to compile Raster canvas shader: " + glGetShaderInfoLog(shader));
    }
    return shader;
  }

  private static String presentVertexShader() {
    return "#version 330 core\n"
        + "layout(location = 0) in vec2 aPosition;\n"
        + "layout(location = 1) in vec2 aTexCoord;\n"
        + "out vec2 vTexCoord;\n"
        + "void main() {\n"
        + "  gl_Position = vec4(aPosition, 0.0, 1.0);\n"
        + "  vTexCoord = aTexCoord;\n"
        + "}\n";
  }

  private static String presentFragmentShader() {
    return "#version 330 core\n"
        + "uniform sampler2D uTexture;\n"
        + "in vec2 vTexCoord;\n"
        + "out vec4 fragColor;\n"
        + "void main() {\n"
        + "  fragColor = texture(uTexture, vTexCoord);\n"
        + "}\n";
  }

  private void installCallbacks() {
    glfwSetFramebufferSizeCallback(
        window,
        (window, framebufferWidth, framebufferHeight) -> {
          this.framebufferWidth = Math.max(1, framebufferWidth);
          this.framebufferHeight = Math.max(1, framebufferHeight);
          events.add(
              new RasterEvent(
                  "resize",
                  LuaValue.array(LuaValue.number(LOGICAL_WIDTH), LuaValue.number(LOGICAL_HEIGHT))));
        });
    glfwSetKeyCallback(
        window,
        (window, key, scancode, action, mods) -> {
          if (action == GLFW_PRESS
              && (key == GLFW_KEY_ENTER || key == GLFW_KEY_KP_ENTER)
              && (mods & GLFW_MOD_ALT) != 0) {
            toggleFullscreen();
            return;
          }
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
          double[] logical = logicalMouse(x, y);
          double[] logicalDelta = logicalDelta(dx, dy);
          events.add(
              new RasterEvent(
                  "mousemoved",
                  LuaValue.array(
                      LuaValue.number(logical[0]),
                      LuaValue.number(logical[1]),
                      LuaValue.number(logicalDelta[0]),
                      LuaValue.number(logicalDelta[1]))));
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
