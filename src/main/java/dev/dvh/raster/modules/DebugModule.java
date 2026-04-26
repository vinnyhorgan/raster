package dev.dvh.raster.modules;

import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_NEAREST;
import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_RED;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.GL_UNPACK_ALIGNMENT;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glBlendFunc;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL11.glDeleteTextures;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glGenTextures;
import static org.lwjgl.opengl.GL11.glIsEnabled;
import static org.lwjgl.opengl.GL11.glPixelStorei;
import static org.lwjgl.opengl.GL11.glTexImage2D;
import static org.lwjgl.opengl.GL11.glTexParameteri;
import static org.lwjgl.opengl.GL11.glTexSubImage2D;
import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_DYNAMIC_DRAW;
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
import static org.lwjgl.opengl.GL20.glUniform2f;
import static org.lwjgl.opengl.GL20.glUniform4f;
import static org.lwjgl.opengl.GL20.glUseProgram;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.GL_R8;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glDeleteVertexArrays;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;
import static org.lwjgl.stb.STBTruetype.stbtt_FindGlyphIndex;
import static org.lwjgl.stb.STBTruetype.stbtt_GetFontVMetrics;
import static org.lwjgl.stb.STBTruetype.stbtt_GetGlyphBitmapBox;
import static org.lwjgl.stb.STBTruetype.stbtt_GetGlyphHMetrics;
import static org.lwjgl.stb.STBTruetype.stbtt_GetGlyphKernAdvance;
import static org.lwjgl.stb.STBTruetype.stbtt_InitFont;
import static org.lwjgl.stb.STBTruetype.stbtt_MakeGlyphBitmap;
import static org.lwjgl.stb.STBTruetype.stbtt_ScaleForPixelHeight;

import dev.dvh.raster.lua.LuaJit;
import dev.dvh.raster.lua.LuaValue;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.HashMap;
import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBTTFontinfo;

public final class DebugModule implements AutoCloseable {

  private static final int FIRST_CHAR = 32;
  private static final int CHAR_COUNT = 96;
  private static final int ATLAS_SIZE = 1024;
  private static final int GLYPH_PAD = 1;
  private static final int STRIDE = 4 * Float.BYTES;
  private static final int MAX_VERTEX_FLOATS = 65536;

  private final WindowModule window;
  private final Font regular = new Font("/font/BoldPixels.ttf");
  private final Font bold = new Font("/font/BoldPixels.ttf");
  private final FloatBuffer vertexData = BufferUtils.createFloatBuffer(MAX_VERTEX_FLOATS);
  private int vao;
  private int vbo;
  private int program;
  private int viewportUniform;
  private int colorUniform;
  private int textureUniform;

  public DebugModule(WindowModule window) {
    this.window = window;
  }

  public void install(LuaJit lua) {
    lua.execute("rs.debug = rs.debug or {}", "=(rs.debug bootstrap)");
    lua.registerFunction("rs.debug.print", this::print);
    lua.registerFunction("rs.debug.text", this::print);
  }

  public void renderError(int width, int height, String message) {
    glViewport(0, 0, width, height);
    glClearColor(0.055f, 0.06f, 0.085f, 1);
    glClear(GL_COLOR_BUFFER_BIT);
    drawText("Raster crashed", 34, 42, 34, 1, 0.88f, 0.28f, 1, true, width, height);
    drawText(
        "Press Escape or close the window to quit.",
        36,
        88,
        17,
        0.74f,
        0.78f,
        0.86f,
        1,
        false,
        width,
        height);
    drawText(
        wrap(cleanMessage(message), width, 16, 38),
        36,
        134,
        16,
        0.93f,
        0.95f,
        1,
        1,
        false,
        width,
        height);
  }

  @Override
  public void close() {
    regular.close();
    bold.close();
    if (program != 0) {
      glDeleteProgram(program);
      program = 0;
    }
    if (vbo != 0) {
      glDeleteBuffers(vbo);
      vbo = 0;
    }
    if (vao != 0) {
      glDeleteVertexArrays(vao);
      vao = 0;
    }
  }

  private LuaValue[] print(LuaValue[] args) {
    String text = args.length > 0 ? args[0].asString() : "";
    int x = intNumber(args, 1, 0);
    int y = intNumber(args, 2, 0);
    float size = number(args, 3, 16);
    float red = number(args, 4, 1);
    float green = number(args, 5, 1);
    float blue = number(args, 6, 1);
    float alpha = number(args, 7, 1);
    boolean useBold = args.length > 8 && args[8].asBoolean();
    int width = intNumber(args, 9, window.getWidth());
    int height = intNumber(args, 10, window.getHeight());
    drawText(text, x, y, size, red, green, blue, alpha, useBold, width, height);
    return LuaValue.array();
  }

  private void drawText(
      String text,
      float x,
      float y,
      float size,
      float red,
      float green,
      float blue,
      float alpha,
      boolean useBold,
      int width,
      int height) {
    if (text == null || text.isEmpty()) {
      return;
    }
    ensureGlObjects();
    Font font = useBold ? bold : regular;
    font.ensureLoaded();
    int pixelSize = Math.max(16, Math.round(size / 16f) * 16);
    float scale = stbtt_ScaleForPixelHeight(font.fontInfo, pixelSize);
    float lineAdvance = (font.ascent - font.descent + font.lineGap) * scale;
    float ascentPx = font.ascent * scale;
    vertexData.clear();
    float cursorX = x;
    float cursorY = Math.round(y + ascentPx);
    int prevGlyphIndex = 0;
    for (int offset = 0; offset < text.length(); ) {
      int codepoint = text.codePointAt(offset);
      offset += Character.charCount(codepoint);
      if (codepoint == '\n') {
        cursorX = x;
        cursorY += lineAdvance;
        prevGlyphIndex = 0;
        continue;
      }
      if (codepoint == '\t') {
        cursorX = Math.round(cursorX + size * 2);
        prevGlyphIndex = 0;
        continue;
      }
      if (codepoint < FIRST_CHAR || codepoint >= FIRST_CHAR + CHAR_COUNT) {
        codepoint = '?';
      }
      CachedGlyph g = font.getGlyph(codepoint, pixelSize);
      if (prevGlyphIndex != 0 && g.glyphIndex != 0) {
        cursorX += stbtt_GetGlyphKernAdvance(font.fontInfo, prevGlyphIndex, g.glyphIndex) * scale;
      }
      prevGlyphIndex = g.glyphIndex;
      float x0 = Math.round(cursorX + g.xoff);
      float y0 = Math.round(cursorY + g.yoff);
      float x1 = Math.round(cursorX + g.xoff2);
      float y1 = Math.round(cursorY + g.yoff2);
      putQuad(x0, y0, x1, y1, g.u0, g.v0, g.u1, g.v1);
      cursorX = Math.round(cursorX + g.xadvance);
    }
    font.flushAtlas();
    if (vertexData.position() == 0) {
      return;
    }
    int floatCount = vertexData.position();
    vertexData.flip();
    boolean blendWasEnabled = glIsEnabled(GL_BLEND);
    boolean depthWasEnabled = glIsEnabled(GL_DEPTH_TEST);
    glEnable(GL_BLEND);
    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    glDisable(GL_DEPTH_TEST);
    glUseProgram(program);
    glUniform2f(viewportUniform, width, height);
    glUniform4f(colorUniform, red, green, blue, alpha);
    glUniform1i(textureUniform, 0);
    glBindTexture(GL_TEXTURE_2D, font.texture);
    glBindVertexArray(vao);
    glBindBuffer(GL_ARRAY_BUFFER, vbo);
    glBufferData(GL_ARRAY_BUFFER, vertexData, GL_DYNAMIC_DRAW);
    glDrawArrays(GL_TRIANGLES, 0, floatCount / 4);
    glBindBuffer(GL_ARRAY_BUFFER, 0);
    glBindVertexArray(0);
    glBindTexture(GL_TEXTURE_2D, 0);
    glUseProgram(0);
    if (!blendWasEnabled) {
      glDisable(GL_BLEND);
    }
    if (depthWasEnabled) {
      glEnable(GL_DEPTH_TEST);
    }
  }

  private void ensureGlObjects() {
    if (program != 0) {
      return;
    }
    program = createProgram();
    viewportUniform = glGetUniformLocation(program, "uViewport");
    colorUniform = glGetUniformLocation(program, "uColor");
    textureUniform = glGetUniformLocation(program, "uTexture");
    vao = glGenVertexArrays();
    vbo = glGenBuffers();
    glBindVertexArray(vao);
    glBindBuffer(GL_ARRAY_BUFFER, vbo);
    glVertexAttribPointer(0, 2, GL_FLOAT, false, STRIDE, 0);
    glEnableVertexAttribArray(0);
    glVertexAttribPointer(1, 2, GL_FLOAT, false, STRIDE, 2L * Float.BYTES);
    glEnableVertexAttribArray(1);
    glBindBuffer(GL_ARRAY_BUFFER, 0);
    glBindVertexArray(0);
  }

  private void putQuad(
      float x0, float y0, float x1, float y1, float u0, float v0, float u1, float v1) {
    if (vertexData.remaining() < 24) {
      return;
    }
    vertexData.put(x0).put(y0).put(u0).put(v0);
    vertexData.put(x1).put(y0).put(u1).put(v0);
    vertexData.put(x1).put(y1).put(u1).put(v1);
    vertexData.put(x0).put(y0).put(u0).put(v0);
    vertexData.put(x1).put(y1).put(u1).put(v1);
    vertexData.put(x0).put(y1).put(u0).put(v1);
  }

  private static int createProgram() {
    int vertex = compile(GL_VERTEX_SHADER, vertexShader());
    int fragment = compile(GL_FRAGMENT_SHADER, fragmentShader());
    int shaderProgram = glCreateProgram();
    glAttachShader(shaderProgram, vertex);
    glAttachShader(shaderProgram, fragment);
    glLinkProgram(shaderProgram);
    if (glGetProgrami(shaderProgram, GL_LINK_STATUS) == 0) {
      throw new IllegalStateException(
          "Unable to link rs.debug text shader: " + glGetProgramInfoLog(shaderProgram));
    }
    glDeleteShader(vertex);
    glDeleteShader(fragment);
    return shaderProgram;
  }

  private static int compile(int type, String source) {
    int shader = glCreateShader(type);
    glShaderSource(shader, source);
    glCompileShader(shader);
    if (glGetShaderi(shader, GL_COMPILE_STATUS) == 0) {
      throw new IllegalStateException(
          "Unable to compile rs.debug text shader: " + glGetShaderInfoLog(shader));
    }
    return shader;
  }

  private static String vertexShader() {
    return "#version 330 core\n"
        + "layout(location = 0) in vec2 aPosition;\n"
        + "layout(location = 1) in vec2 aTexCoord;\n"
        + "uniform vec2 uViewport;\n"
        + "out vec2 vTexCoord;\n"
        + "void main() {\n"
        + "  vec2 ndc = vec2((aPosition.x / uViewport.x) * 2.0 - 1.0, "
        + "1.0 - (aPosition.y / uViewport.y) * 2.0);\n"
        + "  gl_Position = vec4(ndc, 0.0, 1.0);\n"
        + "  vTexCoord = aTexCoord;\n"
        + "}\n";
  }

  private static String fragmentShader() {
    return "#version 330 core\n"
        + "uniform sampler2D uTexture;\n"
        + "uniform vec4 uColor;\n"
        + "in vec2 vTexCoord;\n"
        + "out vec4 fragColor;\n"
        + "void main() {\n"
        + "  float alpha = texture(uTexture, vTexCoord).r;\n"
        + "  fragColor = vec4(uColor.rgb, uColor.a * alpha);\n"
        + "}\n";
  }

  private static String wrap(String text, int width, int size, int margin) {
    int maxColumns = Math.max(20, (width - margin * 2) / Math.max(1, size / 2));
    StringBuilder result = new StringBuilder();
    for (String rawLine : text.split("\\R", -1)) {
      String line = rawLine;
      while (line.length() > maxColumns) {
        int breakAt = line.lastIndexOf(' ', maxColumns);
        if (breakAt < maxColumns / 2) {
          breakAt = maxColumns;
        }
        result.append(line, 0, breakAt).append('\n');
        line = line.substring(Math.min(breakAt + 1, line.length()));
      }
      result.append(line).append('\n');
    }
    return result.toString().stripTrailing();
  }

  private static String cleanMessage(String message) {
    if (message == null || message.isBlank()) {
      return "Unknown runtime error";
    }
    return message.replace('\t', ' ');
  }

  private static int intNumber(LuaValue[] args, int index, int fallback) {
    return args.length > index ? args[index].asInt(fallback) : fallback;
  }

  private static float number(LuaValue[] args, int index, float fallback) {
    return args.length > index && args[index].isNumber()
        ? (float) args[index].asNumber()
        : fallback;
  }

  private static ByteBuffer resourceBuffer(String path) {
    try (InputStream input = DebugModule.class.getResourceAsStream(path)) {
      if (input == null) {
        throw new IllegalStateException("Missing debug font resource: " + path);
      }
      byte[] bytes = input.readAllBytes();
      ByteBuffer buffer = BufferUtils.createByteBuffer(bytes.length);
      buffer.put(bytes).flip();
      return buffer;
    } catch (IOException e) {
      throw new IllegalStateException("Unable to read debug font resource: " + path, e);
    }
  }

  private static final class CachedGlyph {
    final float u0;
    final float v0;
    final float u1;
    final float v1;
    final float xoff;
    final float yoff;
    final float xoff2;
    final float yoff2;
    final float xadvance;
    final int glyphIndex;

    CachedGlyph(
        float u0,
        float v0,
        float u1,
        float v1,
        float xoff,
        float yoff,
        float xoff2,
        float yoff2,
        float xadvance,
        int glyphIndex) {
      this.u0 = u0;
      this.v0 = v0;
      this.u1 = u1;
      this.v1 = v1;
      this.xoff = xoff;
      this.yoff = yoff;
      this.xoff2 = xoff2;
      this.yoff2 = yoff2;
      this.xadvance = xadvance;
      this.glyphIndex = glyphIndex;
    }
  }

  private static final class Font implements AutoCloseable {

    private final String resource;
    private STBTTFontinfo fontInfo;
    private ByteBuffer fontData;
    private ByteBuffer atlasData;
    private int texture;
    private boolean atlasDirty;
    private int packCursorX;
    private int packCursorY;
    private int packRowHeight;
    private final HashMap<Long, CachedGlyph> cache = new HashMap<>();
    private int ascent;
    private int descent;
    private int lineGap;

    private Font(String resource) {
      this.resource = resource;
    }

    private void ensureLoaded() {
      if (texture != 0) {
        return;
      }
      fontData = resourceBuffer(resource);
      fontInfo = STBTTFontinfo.malloc();
      if (!stbtt_InitFont(fontInfo, fontData)) {
        throw new IllegalStateException("Unable to init debug font: " + resource);
      }
      int[] a = new int[1];
      int[] d = new int[1];
      int[] lg = new int[1];
      stbtt_GetFontVMetrics(fontInfo, a, d, lg);
      ascent = a[0];
      descent = d[0];
      lineGap = lg[0];
      atlasData = BufferUtils.createByteBuffer(ATLAS_SIZE * ATLAS_SIZE);
      texture = glGenTextures();
      glBindTexture(GL_TEXTURE_2D, texture);
      glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
      glTexImage2D(
          GL_TEXTURE_2D, 0, GL_R8, ATLAS_SIZE, ATLAS_SIZE, 0, GL_RED, GL_UNSIGNED_BYTE, atlasData);
      glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
      glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
      glTexParameteri(GL_TEXTURE_2D, org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
      glTexParameteri(GL_TEXTURE_2D, org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
      glBindTexture(GL_TEXTURE_2D, 0);
    }

    private CachedGlyph getGlyph(int codepoint, int pixelSize) {
      long key = ((long) pixelSize << 16) | codepoint;
      CachedGlyph g = cache.get(key);
      if (g != null) {
        return g;
      }
      g = rasterizeGlyph(codepoint, pixelSize);
      cache.put(key, g);
      return g;
    }

    private CachedGlyph rasterizeGlyph(int codepoint, int pixelSize) {
      float scale = stbtt_ScaleForPixelHeight(fontInfo, pixelSize);
      int glyphIndex = stbtt_FindGlyphIndex(fontInfo, codepoint);
      int[] advanceWidth = new int[1];
      stbtt_GetGlyphHMetrics(fontInfo, glyphIndex, advanceWidth, null);
      float xadvance = advanceWidth[0] * scale;
      int[] ix0 = new int[1];
      int[] iy0 = new int[1];
      int[] ix1 = new int[1];
      int[] iy1 = new int[1];
      stbtt_GetGlyphBitmapBox(fontInfo, glyphIndex, scale, scale, ix0, iy0, ix1, iy1);
      int glyphW = ix1[0] - ix0[0];
      int glyphH = iy1[0] - iy0[0];
      if (glyphW <= 0 || glyphH <= 0) {
        return new CachedGlyph(0, 0, 0, 0, 0, 0, 0, 0, xadvance, glyphIndex);
      }
      int packW = glyphW + GLYPH_PAD * 2;
      int packH = glyphH + GLYPH_PAD * 2;
      if (packCursorX + packW > ATLAS_SIZE) {
        packCursorY += packRowHeight;
        packCursorX = 0;
        packRowHeight = 0;
      }
      if (packCursorY + packH > ATLAS_SIZE) {
        return new CachedGlyph(0, 0, 0, 0, 0, 0, 0, 0, xadvance, glyphIndex);
      }
      int px = packCursorX + GLYPH_PAD;
      int py = packCursorY + GLYPH_PAD;
      packCursorX += packW;
      packRowHeight = Math.max(packRowHeight, packH);
      ByteBuffer bitmap = BufferUtils.createByteBuffer(glyphW * glyphH);
      stbtt_MakeGlyphBitmap(fontInfo, bitmap, glyphW, glyphH, glyphW, scale, scale, glyphIndex);
      for (int row = 0; row < glyphH; row++) {
        for (int col = 0; col < glyphW; col++) {
          byte pixel = bitmap.get();
          atlasData.put(
              (py + row) * ATLAS_SIZE + px + col, (pixel & 0xFF) >= 128 ? (byte) 0xFF : 0);
        }
      }
      atlasDirty = true;
      float invAtlas = 1.0f / ATLAS_SIZE;
      return new CachedGlyph(
          px * invAtlas,
          py * invAtlas,
          (px + glyphW) * invAtlas,
          (py + glyphH) * invAtlas,
          ix0[0],
          iy0[0],
          ix1[0],
          iy1[0],
          xadvance,
          glyphIndex);
    }

    private void flushAtlas() {
      if (!atlasDirty) {
        return;
      }
      atlasDirty = false;
      atlasData.position(0);
      glBindTexture(GL_TEXTURE_2D, texture);
      glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
      glTexSubImage2D(
          GL_TEXTURE_2D, 0, 0, 0, ATLAS_SIZE, ATLAS_SIZE, GL_RED, GL_UNSIGNED_BYTE, atlasData);
      glBindTexture(GL_TEXTURE_2D, 0);
    }

    @Override
    public void close() {
      if (texture != 0) {
        glDeleteTextures(texture);
        texture = 0;
      }
      if (fontInfo != null) {
        fontInfo.free();
        fontInfo = null;
      }
      fontData = null;
      atlasData = null;
      cache.clear();
    }
  }
}
