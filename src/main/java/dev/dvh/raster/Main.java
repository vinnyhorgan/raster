package dev.dvh.raster;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.GLFW_FALSE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;
import static org.lwjgl.glfw.GLFW.GLFW_RESIZABLE;
import static org.lwjgl.glfw.GLFW.GLFW_TRUE;
import static org.lwjgl.glfw.GLFW.GLFW_VISIBLE;
import static org.lwjgl.glfw.GLFW.glfwCreateWindow;
import static org.lwjgl.glfw.GLFW.glfwDefaultWindowHints;
import static org.lwjgl.glfw.GLFW.glfwDestroyWindow;
import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwSetErrorCallback;
import static org.lwjgl.glfw.GLFW.glfwSetKeyCallback;
import static org.lwjgl.glfw.GLFW.glfwSetWindowShouldClose;
import static org.lwjgl.glfw.GLFW.glfwShowWindow;
import static org.lwjgl.glfw.GLFW.glfwSwapBuffers;
import static org.lwjgl.glfw.GLFW.glfwSwapInterval;
import static org.lwjgl.glfw.GLFW.glfwTerminate;
import static org.lwjgl.glfw.GLFW.glfwWindowHint;
import static org.lwjgl.glfw.GLFW.glfwWindowShouldClose;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.system.MemoryUtil.NULL;

import dev.dvh.raster.lua.LuaJit;
import org.lwjgl.Version;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;

public final class Main {

  private long window;

  public void run() {
    System.out.println("Hello LWJGL " + Version.getVersion() + "!");
    runLuaJitHelloWorld();

    init();
    loop();

    glfwFreeCallbacks(window);
    glfwDestroyWindow(window);
    glfwTerminate();
    glfwSetErrorCallback(null).free();
  }

  private void runLuaJitHelloWorld() {
    try (LuaJit lua = LuaJit.create()) {
      lua.setGlobal("engine", "Raster");
      lua.execute(
          "message = 'Hello from LuaJIT embedded in ' .. engine\n" + "answer = 40 + 2",
          "=raster_bootstrap");

      System.out.println(lua.getGlobalString("message"));
      System.out.println("LuaJIT answer: " + lua.getGlobalNumber("answer"));
    }
  }

  private void init() {
    GLFWErrorCallback.createPrint(System.err).set();

    if (!glfwInit()) {
      throw new IllegalStateException("Unable to initialize GLFW");
    }

    glfwDefaultWindowHints();
    glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
    glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);

    window = glfwCreateWindow(300, 300, "Raster", NULL, NULL);
    if (window == NULL) {
      throw new IllegalStateException("Failed to create the GLFW window");
    }

    glfwSetKeyCallback(
        window,
        (window, key, scancode, action, mods) -> {
          if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE) {
            glfwSetWindowShouldClose(window, true);
          }
        });

    glfwMakeContextCurrent(window);
    glfwSwapInterval(1);
    glfwShowWindow(window);
  }

  private void loop() {
    GL.createCapabilities();
    glClearColor(1.0f, 0.0f, 0.0f, 0.0f);

    while (!glfwWindowShouldClose(window)) {
      glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
      glfwSwapBuffers(window);
      glfwPollEvents();
    }
  }

  public static void main(String[] args) {
    new Main().run();
  }
}
