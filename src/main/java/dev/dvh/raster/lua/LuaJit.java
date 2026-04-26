package dev.dvh.raster.lua;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class LuaJit implements AutoCloseable {

  private static final String NATIVE_RESOURCE_DIR = "/natives/linux-x64";
  private static final String LUAJIT_LIBRARY = "libluajit-5.1.so.2";
  private static final String BINDING_LIBRARY = "libraster_luajit.so";

  static {
    NativeLoader.load();
  }

  private long handle;
  private final Map<Integer, LuaFunction> functions = new HashMap<>();
  private int nextFunctionId = 1;

  private LuaJit(long handle) {
    this.handle = handle;
  }

  public static LuaJit create() {
    return create(true);
  }

  public static LuaJit create(boolean openStandardLibraries) {
    long handle = open();
    LuaJit lua = new LuaJit(handle);
    if (openStandardLibraries) {
      lua.openLibraries();
    }
    return lua;
  }

  public synchronized void openLibraries() {
    ensureOpen();
    openLibraries(handle);
  }

  public synchronized void execute(String source) {
    execute(source, "=(java)");
  }

  public synchronized void execute(String source, String chunkName) {
    if (source == null) {
      throw new NullPointerException("source");
    }
    execute(source.getBytes(UTF_8), chunkName);
  }

  public synchronized void execute(byte[] source, String chunkName) {
    ensureOpen();
    if (source == null) {
      throw new NullPointerException("source");
    }
    execute(handle, source, chunkName == null ? "=(java)" : chunkName);
  }

  public synchronized LuaValue[] call(String expression, LuaValue... arguments) {
    ensureOpen();
    if (expression == null) {
      throw new NullPointerException("expression");
    }
    LuaValue[] values = arguments == null ? new LuaValue[0] : arguments;
    Object[] result = call(handle, expression, values);
    return toValues(result);
  }

  public synchronized void registerFunction(String qualifiedName, LuaFunction function) {
    ensureOpen();
    if (qualifiedName == null) {
      throw new NullPointerException("qualifiedName");
    }
    if (function == null) {
      throw new NullPointerException("function");
    }
    int id = nextFunctionId++;
    functions.put(id, function);
    registerFunction(handle, this, id, qualifiedName);
  }

  public synchronized void setGlobal(String name, String value) {
    ensureOpen();
    requireName(name);
    if (value == null) {
      setGlobalNil(handle, name);
      return;
    }
    setGlobalString(handle, name, value.getBytes(UTF_8));
  }

  public synchronized void setGlobal(String name, double value) {
    ensureOpen();
    requireName(name);
    setGlobalNumber(handle, name, value);
  }

  public synchronized void setGlobal(String name, boolean value) {
    ensureOpen();
    requireName(name);
    setGlobalBoolean(handle, name, value);
  }

  public synchronized String getGlobalString(String name) {
    ensureOpen();
    requireName(name);
    byte[] value = getGlobalString(handle, name);
    return value == null ? null : new String(value, UTF_8);
  }

  public synchronized double getGlobalNumber(String name) {
    ensureOpen();
    requireName(name);
    return getGlobalNumber(handle, name);
  }

  public synchronized boolean getGlobalBoolean(String name) {
    ensureOpen();
    requireName(name);
    return getGlobalBoolean(handle, name);
  }

  public synchronized boolean isOpen() {
    return handle != 0;
  }

  @Override
  public synchronized void close() {
    if (handle == 0) {
      return;
    }
    long state = handle;
    handle = 0;
    functions.clear();
    close(state);
  }

  private Object[] invoke(int functionId, Object[] arguments) {
    LuaFunction function = functions.get(functionId);
    if (function == null) {
      throw new LuaJitException("Unknown Java Lua function id: " + functionId);
    }
    LuaValue[] values = toValues(arguments);
    LuaValue[] result = function.call(values);
    return result == null ? new LuaValue[0] : result;
  }

  private static LuaValue[] toValues(Object[] arguments) {
    if (arguments == null || arguments.length == 0) {
      return new LuaValue[0];
    }
    LuaValue[] values = new LuaValue[arguments.length];
    for (int i = 0; i < arguments.length; i++) {
      Object argument = arguments[i];
      values[i] = argument instanceof LuaValue luaValue ? luaValue : LuaValue.nil();
    }
    return values;
  }

  private void ensureOpen() {
    if (handle == 0) {
      throw new IllegalStateException("LuaJIT state is closed");
    }
  }

  private static void requireName(String name) {
    if (name == null) {
      throw new NullPointerException("name");
    }
    if (name.isEmpty()) {
      throw new IllegalArgumentException("name must not be empty");
    }
  }

  private static native long open();

  private static native void close(long state);

  private static native void openLibraries(long state);

  private static native void execute(long state, byte[] source, String chunkName);

  private static native Object[] call(long state, String expression, LuaValue[] arguments);

  private static native void registerFunction(
      long state, LuaJit owner, int functionId, String qualifiedName);

  private static native void setGlobalNil(long state, String name);

  private static native void setGlobalString(long state, String name, byte[] value);

  private static native void setGlobalNumber(long state, String name, double value);

  private static native void setGlobalBoolean(long state, String name, boolean value);

  private static native byte[] getGlobalString(long state, String name);

  private static native double getGlobalNumber(long state, String name);

  private static native boolean getGlobalBoolean(long state, String name);

  private static final class NativeLoader {

    private NativeLoader() {}

    static void load() {
      requireLinuxX64();
      try {
        Path directory = Files.createTempDirectory("raster-luajit-");
        directory.toFile().deleteOnExit();
        Path luajit = extract(directory, LUAJIT_LIBRARY);
        Path binding = extract(directory, BINDING_LIBRARY);
        System.load(luajit.toAbsolutePath().toString());
        System.load(binding.toAbsolutePath().toString());
      } catch (IOException e) {
        throw new UnsatisfiedLinkError(
            "Unable to extract LuaJIT native libraries: " + e.getMessage());
      }
    }

    private static void requireLinuxX64() {
      String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
      String arch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
      boolean linux = os.contains("linux");
      boolean x64 = arch.equals("amd64") || arch.equals("x86_64");
      if (!linux || !x64) {
        throw new UnsatisfiedLinkError("Bundled LuaJIT natives support Linux x64 only");
      }
    }

    private static Path extract(Path directory, String library) throws IOException {
      String resource = NATIVE_RESOURCE_DIR + "/" + library;
      Path target = directory.resolve(library);
      try (InputStream input = LuaJit.class.getResourceAsStream(resource)) {
        if (input == null) {
          throw new UnsatisfiedLinkError("Missing native resource: " + resource);
        }
        Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
      }
      target.toFile().deleteOnExit();
      return target;
    }
  }
}
