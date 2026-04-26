package dev.dvh.raster.modules;

import static org.lwjgl.opengl.GL11.GL_BACK;
import static org.lwjgl.opengl.GL11.GL_CCW;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_LESS;
import static org.lwjgl.opengl.GL11.GL_LINES;
import static org.lwjgl.opengl.GL11.GL_LINE_LOOP;
import static org.lwjgl.opengl.GL11.GL_LINE_STRIP;
import static org.lwjgl.opengl.GL11.GL_ONE;
import static org.lwjgl.opengl.GL11.GL_POINTS;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.GL_TRIANGLE_FAN;
import static org.lwjgl.opengl.GL11.GL_TRIANGLE_STRIP;
import static org.lwjgl.opengl.GL11.GL_ZERO;
import static org.lwjgl.opengl.GL11.glBlendFunc;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL11.glCullFace;
import static org.lwjgl.opengl.GL11.glDepthFunc;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glFrontFace;
import static org.lwjgl.opengl.GL11.glLineWidth;
import static org.lwjgl.opengl.GL11.glPointSize;
import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_DYNAMIC_DRAW;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL20.GL_COMPILE_STATUS;
import static org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20.GL_LINK_STATUS;
import static org.lwjgl.opengl.GL20.GL_VERTEX_SHADER;
import static org.lwjgl.opengl.GL20.glAttachShader;
import static org.lwjgl.opengl.GL20.glCompileShader;
import static org.lwjgl.opengl.GL20.glCreateProgram;
import static org.lwjgl.opengl.GL20.glCreateShader;
import static org.lwjgl.opengl.GL20.glDeleteShader;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glGetProgramInfoLog;
import static org.lwjgl.opengl.GL20.glGetProgrami;
import static org.lwjgl.opengl.GL20.glGetShaderInfoLog;
import static org.lwjgl.opengl.GL20.glGetShaderi;
import static org.lwjgl.opengl.GL20.glGetUniformLocation;
import static org.lwjgl.opengl.GL20.glLinkProgram;
import static org.lwjgl.opengl.GL20.glShaderSource;
import static org.lwjgl.opengl.GL20.glUniformMatrix4fv;
import static org.lwjgl.opengl.GL20.glUseProgram;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

import dev.dvh.raster.lua.LuaJit;
import dev.dvh.raster.lua.LuaValue;
import java.nio.FloatBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;

public final class GlModule implements AutoCloseable {

  private static final int GL_MODELVIEW = 0x1700;
  private static final int GL_PROJECTION = 0x1701;
  private static final int GL_TEXTURE = 0x1702;
  private static final int GL_QUADS = 0x0007;
  private static final int GL_QUAD_STRIP = 0x0008;
  private static final int GL_POLYGON = 0x0009;
  private static final int STRIDE = 8 * Float.BYTES;

  private final Deque<Matrix4f> modelView = new ArrayDeque<>();
  private final Deque<Matrix4f> projection = new ArrayDeque<>();
  private final Deque<Matrix4f> texture = new ArrayDeque<>();
  private final List<Vertex> vertices = new ArrayList<>();
  private final FloatBuffer matrixBuffer = BufferUtils.createFloatBuffer(16);
  private int matrixMode = GL_MODELVIEW;
  private int beginMode = -1;
  private float red = 1;
  private float green = 1;
  private float blue = 1;
  private float alpha = 1;
  private int vao;
  private int vbo;
  private int program;
  private int projectionUniform;
  private int modelViewUniform;

  public GlModule() {
    modelView.push(new Matrix4f());
    projection.push(new Matrix4f());
    texture.push(new Matrix4f());
  }

  public void install(LuaJit lua) {
    lua.execute(constants(), "=(rs.gl constants)");
    lua.registerFunction("rs.gl.clearColor", this::clearColor);
    lua.registerFunction("rs.gl.clear", this::clear);
    lua.registerFunction("rs.gl.viewport", this::viewport);
    lua.registerFunction("rs.gl.enable", this::enable);
    lua.registerFunction("rs.gl.disable", this::disable);
    lua.registerFunction("rs.gl.blendFunc", this::blendFunc);
    lua.registerFunction("rs.gl.depthFunc", this::depthFunc);
    lua.registerFunction("rs.gl.cullFace", this::cullFace);
    lua.registerFunction("rs.gl.frontFace", this::frontFace);
    lua.registerFunction("rs.gl.pointSize", this::pointSize);
    lua.registerFunction("rs.gl.lineWidth", this::lineWidth);
    lua.registerFunction("rs.gl.matrixMode", this::matrixMode);
    lua.registerFunction("rs.gl.loadIdentity", this::loadIdentity);
    lua.registerFunction("rs.gl.loadMatrix", this::loadMatrix);
    lua.registerFunction("rs.gl.multMatrix", this::multMatrix);
    lua.registerFunction("rs.gl.pushMatrix", this::pushMatrix);
    lua.registerFunction("rs.gl.popMatrix", this::popMatrix);
    lua.registerFunction("rs.gl.translate", this::translate);
    lua.registerFunction("rs.gl.scale", this::scale);
    lua.registerFunction("rs.gl.rotate", this::rotate);
    lua.registerFunction("rs.gl.ortho", this::ortho);
    lua.registerFunction("rs.gl.frustum", this::frustum);
    lua.registerFunction("rs.gl.begin", this::begin);
    lua.registerFunction("rs.gl.end_", this::end);
    lua.registerFunction("rs.gl.end", this::end);
    lua.registerFunction("rs.gl.color3", this::color3);
    lua.registerFunction("rs.gl.color4", this::color4);
    lua.registerFunction("rs.gl.vertex2", this::vertex2);
    lua.registerFunction("rs.gl.vertex3", this::vertex3);
    lua.registerFunction("rs.gl.vertex4", this::vertex4);
  }

  @Override
  public void close() {
    vao = 0;
    vbo = 0;
    program = 0;
  }

  private LuaValue[] clearColor(LuaValue[] args) {
    glClearColor(number(args, 0, 0), number(args, 1, 0), number(args, 2, 0), number(args, 3, 1));
    return LuaValue.array();
  }

  private LuaValue[] clear(LuaValue[] args) {
    glClear(args.length == 0 ? GL_COLOR_BUFFER_BIT : args[0].asInt(GL_COLOR_BUFFER_BIT));
    return LuaValue.array();
  }

  private LuaValue[] viewport(LuaValue[] args) {
    glViewport(
        intNumber(args, 0, 0), intNumber(args, 1, 0), intNumber(args, 2, 0), intNumber(args, 3, 0));
    return LuaValue.array();
  }

  private LuaValue[] enable(LuaValue[] args) {
    glEnable(args.length == 0 ? 0 : args[0].asInt(0));
    return LuaValue.array();
  }

  private LuaValue[] disable(LuaValue[] args) {
    glDisable(args.length == 0 ? 0 : args[0].asInt(0));
    return LuaValue.array();
  }

  private LuaValue[] blendFunc(LuaValue[] args) {
    glBlendFunc(intNumber(args, 0, GL_ONE), intNumber(args, 1, GL_ZERO));
    return LuaValue.array();
  }

  private LuaValue[] depthFunc(LuaValue[] args) {
    glDepthFunc(intNumber(args, 0, GL_LESS));
    return LuaValue.array();
  }

  private LuaValue[] cullFace(LuaValue[] args) {
    glCullFace(intNumber(args, 0, GL_BACK));
    return LuaValue.array();
  }

  private LuaValue[] frontFace(LuaValue[] args) {
    glFrontFace(intNumber(args, 0, GL_CCW));
    return LuaValue.array();
  }

  private LuaValue[] pointSize(LuaValue[] args) {
    glPointSize(number(args, 0, 1));
    return LuaValue.array();
  }

  private LuaValue[] lineWidth(LuaValue[] args) {
    glLineWidth(number(args, 0, 1));
    return LuaValue.array();
  }

  private LuaValue[] matrixMode(LuaValue[] args) {
    int mode = intNumber(args, 0, GL_MODELVIEW);
    if (mode == GL_MODELVIEW || mode == GL_PROJECTION || mode == GL_TEXTURE) {
      matrixMode = mode;
    }
    return LuaValue.array();
  }

  private LuaValue[] loadIdentity(LuaValue[] args) {
    currentMatrix().identity();
    return LuaValue.array();
  }

  private LuaValue[] loadMatrix(LuaValue[] args) {
    currentMatrix().set(matrix(args.length == 0 ? LuaValue.nil() : args[0]));
    return LuaValue.array();
  }

  private LuaValue[] multMatrix(LuaValue[] args) {
    currentMatrix().mul(matrix(args.length == 0 ? LuaValue.nil() : args[0]));
    return LuaValue.array();
  }

  private LuaValue[] pushMatrix(LuaValue[] args) {
    stack().push(new Matrix4f(currentMatrix()));
    return LuaValue.array();
  }

  private LuaValue[] popMatrix(LuaValue[] args) {
    Deque<Matrix4f> stack = stack();
    if (stack.size() > 1) {
      stack.pop();
    }
    return LuaValue.array();
  }

  private LuaValue[] translate(LuaValue[] args) {
    currentMatrix().translate(number(args, 0, 0), number(args, 1, 0), number(args, 2, 0));
    return LuaValue.array();
  }

  private LuaValue[] scale(LuaValue[] args) {
    currentMatrix().scale(number(args, 0, 1), number(args, 1, 1), number(args, 2, 1));
    return LuaValue.array();
  }

  private LuaValue[] rotate(LuaValue[] args) {
    currentMatrix()
        .rotate(
            (float) Math.toRadians(number(args, 0, 0)),
            number(args, 1, 0),
            number(args, 2, 0),
            number(args, 3, 1));
    return LuaValue.array();
  }

  private LuaValue[] ortho(LuaValue[] args) {
    currentMatrix()
        .ortho(
            number(args, 0, -1),
            number(args, 1, 1),
            number(args, 2, -1),
            number(args, 3, 1),
            number(args, 4, -1),
            number(args, 5, 1));
    return LuaValue.array();
  }

  private LuaValue[] frustum(LuaValue[] args) {
    currentMatrix()
        .frustum(
            number(args, 0, -1),
            number(args, 1, 1),
            number(args, 2, -1),
            number(args, 3, 1),
            number(args, 4, 1),
            number(args, 5, 100));
    return LuaValue.array();
  }

  private LuaValue[] begin(LuaValue[] args) {
    beginMode = intNumber(args, 0, GL_TRIANGLES);
    vertices.clear();
    return LuaValue.array();
  }

  private LuaValue[] end(LuaValue[] args) {
    if (beginMode != -1 && !vertices.isEmpty()) {
      draw(expand(vertices, beginMode), drawMode(beginMode));
    }
    beginMode = -1;
    vertices.clear();
    return LuaValue.array();
  }

  private LuaValue[] color3(LuaValue[] args) {
    red = number(args, 0, 1);
    green = number(args, 1, 1);
    blue = number(args, 2, 1);
    alpha = 1;
    return LuaValue.array();
  }

  private LuaValue[] color4(LuaValue[] args) {
    red = number(args, 0, 1);
    green = number(args, 1, 1);
    blue = number(args, 2, 1);
    alpha = number(args, 3, 1);
    return LuaValue.array();
  }

  private LuaValue[] vertex2(LuaValue[] args) {
    vertices.add(new Vertex(number(args, 0, 0), number(args, 1, 0), 0, 1, red, green, blue, alpha));
    return LuaValue.array();
  }

  private LuaValue[] vertex3(LuaValue[] args) {
    vertices.add(
        new Vertex(
            number(args, 0, 0),
            number(args, 1, 0),
            number(args, 2, 0),
            1,
            red,
            green,
            blue,
            alpha));
    return LuaValue.array();
  }

  private LuaValue[] vertex4(LuaValue[] args) {
    vertices.add(
        new Vertex(
            number(args, 0, 0),
            number(args, 1, 0),
            number(args, 2, 0),
            number(args, 3, 1),
            red,
            green,
            blue,
            alpha));
    return LuaValue.array();
  }

  private void draw(List<Vertex> drawVertices, int mode) {
    ensureGlObjects();
    FloatBuffer data = BufferUtils.createFloatBuffer(drawVertices.size() * 8);
    for (Vertex vertex : drawVertices) {
      data.put(vertex.x).put(vertex.y).put(vertex.z).put(vertex.w);
      data.put(vertex.r).put(vertex.g).put(vertex.b).put(vertex.a);
    }
    data.flip();
    glUseProgram(program);
    uploadMatrix(projectionUniform, projection.peek());
    uploadMatrix(modelViewUniform, modelView.peek());
    glBindVertexArray(vao);
    glBindBuffer(GL_ARRAY_BUFFER, vbo);
    glBufferData(GL_ARRAY_BUFFER, data, GL_DYNAMIC_DRAW);
    org.lwjgl.opengl.GL11.glDrawArrays(mode, 0, drawVertices.size());
    glBindBuffer(GL_ARRAY_BUFFER, 0);
    glBindVertexArray(0);
    glUseProgram(0);
  }

  private void ensureGlObjects() {
    if (program != 0) {
      return;
    }
    program = createProgram();
    projectionUniform = glGetUniformLocation(program, "uProjection");
    modelViewUniform = glGetUniformLocation(program, "uModelView");
    vao = glGenVertexArrays();
    vbo = glGenBuffers();
    glBindVertexArray(vao);
    glBindBuffer(GL_ARRAY_BUFFER, vbo);
    glVertexAttribPointer(0, 4, org.lwjgl.opengl.GL11.GL_FLOAT, false, STRIDE, 0);
    glEnableVertexAttribArray(0);
    glVertexAttribPointer(1, 4, org.lwjgl.opengl.GL11.GL_FLOAT, false, STRIDE, 4L * Float.BYTES);
    glEnableVertexAttribArray(1);
    glBindBuffer(GL_ARRAY_BUFFER, 0);
    glBindVertexArray(0);
  }

  private int createProgram() {
    int vertex = compile(GL_VERTEX_SHADER, vertexShader());
    int fragment = compile(GL_FRAGMENT_SHADER, fragmentShader());
    int shaderProgram = glCreateProgram();
    glAttachShader(shaderProgram, vertex);
    glAttachShader(shaderProgram, fragment);
    glLinkProgram(shaderProgram);
    if (glGetProgrami(shaderProgram, GL_LINK_STATUS) == 0) {
      throw new IllegalStateException(
          "Unable to link rs.gl shader: " + glGetProgramInfoLog(shaderProgram));
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
          "Unable to compile rs.gl shader: " + glGetShaderInfoLog(shader));
    }
    return shader;
  }

  private void uploadMatrix(int location, Matrix4f matrix) {
    matrixBuffer.clear();
    matrix.get(matrixBuffer);
    glUniformMatrix4fv(location, false, matrixBuffer);
  }

  private Matrix4f currentMatrix() {
    return stack().peek();
  }

  private Deque<Matrix4f> stack() {
    return switch (matrixMode) {
      case GL_PROJECTION -> projection;
      case GL_TEXTURE -> texture;
      default -> modelView;
    };
  }

  private static Matrix4f matrix(LuaValue table) {
    Matrix4f matrix = new Matrix4f();
    Map<String, LuaValue> values = table.asTable();
    if (values.size() >= 16) {
      matrix.set(
          value(values, 1),
          value(values, 2),
          value(values, 3),
          value(values, 4),
          value(values, 5),
          value(values, 6),
          value(values, 7),
          value(values, 8),
          value(values, 9),
          value(values, 10),
          value(values, 11),
          value(values, 12),
          value(values, 13),
          value(values, 14),
          value(values, 15),
          value(values, 16));
    }
    return matrix;
  }

  private static float value(Map<String, LuaValue> values, int index) {
    return (float) values.getOrDefault(String.valueOf(index), LuaValue.nil()).asNumber();
  }

  private static List<Vertex> expand(List<Vertex> source, int mode) {
    if (mode == GL_QUADS) {
      List<Vertex> result = new ArrayList<>();
      for (int i = 0; i + 3 < source.size(); i += 4) {
        result.add(source.get(i));
        result.add(source.get(i + 1));
        result.add(source.get(i + 2));
        result.add(source.get(i));
        result.add(source.get(i + 2));
        result.add(source.get(i + 3));
      }
      return result;
    }
    if (mode == GL_QUAD_STRIP) {
      List<Vertex> result = new ArrayList<>();
      for (int i = 0; i + 3 < source.size(); i += 2) {
        result.add(source.get(i));
        result.add(source.get(i + 1));
        result.add(source.get(i + 2));
        result.add(source.get(i + 1));
        result.add(source.get(i + 3));
        result.add(source.get(i + 2));
      }
      return result;
    }
    if (mode == GL_POLYGON) {
      List<Vertex> result = new ArrayList<>();
      for (int i = 1; i + 1 < source.size(); i++) {
        result.add(source.get(0));
        result.add(source.get(i));
        result.add(source.get(i + 1));
      }
      return result;
    }
    return source;
  }

  private static int drawMode(int mode) {
    return switch (mode) {
      case GL_POINTS -> GL_POINTS;
      case GL_LINES -> GL_LINES;
      case GL_LINE_STRIP -> GL_LINE_STRIP;
      case GL_LINE_LOOP -> GL_LINE_LOOP;
      case GL_TRIANGLE_STRIP -> GL_TRIANGLE_STRIP;
      case GL_TRIANGLE_FAN -> GL_TRIANGLE_FAN;
      case GL_QUADS, GL_QUAD_STRIP, GL_POLYGON -> GL_TRIANGLES;
      default -> GL_TRIANGLES;
    };
  }

  private static int intNumber(LuaValue[] args, int index, int fallback) {
    return args.length > index ? args[index].asInt(fallback) : fallback;
  }

  private static float number(LuaValue[] args, int index, float fallback) {
    return args.length > index && args[index].isNumber()
        ? (float) args[index].asNumber()
        : fallback;
  }

  private static String constants() {
    return "rs.gl = rs.gl or {}\n"
        + "rs.gl.POINTS = 0x0000\n"
        + "rs.gl.LINES = 0x0001\n"
        + "rs.gl.LINE_LOOP = 0x0002\n"
        + "rs.gl.LINE_STRIP = 0x0003\n"
        + "rs.gl.TRIANGLES = 0x0004\n"
        + "rs.gl.TRIANGLE_STRIP = 0x0005\n"
        + "rs.gl.TRIANGLE_FAN = 0x0006\n"
        + "rs.gl.QUADS = 0x0007\n"
        + "rs.gl.QUAD_STRIP = 0x0008\n"
        + "rs.gl.POLYGON = 0x0009\n"
        + "rs.gl.COLOR_BUFFER_BIT = 0x4000\n"
        + "rs.gl.DEPTH_BUFFER_BIT = 0x0100\n"
        + "rs.gl.MODELVIEW = 0x1700\n"
        + "rs.gl.PROJECTION = 0x1701\n"
        + "rs.gl.TEXTURE = 0x1702\n"
        + "rs.gl.DEPTH_TEST = 0x0B71\n"
        + "rs.gl.BLEND = 0x0BE2\n"
        + "rs.gl.CULL_FACE = 0x0B44\n"
        + "rs.gl.FRONT = 0x0404\n"
        + "rs.gl.BACK = 0x0405\n"
        + "rs.gl.FRONT_AND_BACK = 0x0408\n"
        + "rs.gl.CW = 0x0900\n"
        + "rs.gl.CCW = 0x0901\n"
        + "rs.gl.ZERO = 0\n"
        + "rs.gl.ONE = 1\n"
        + "rs.gl.SRC_ALPHA = 0x0302\n"
        + "rs.gl.ONE_MINUS_SRC_ALPHA = 0x0303\n"
        + "rs.gl.LESS = 0x0201\n"
        + "rs.gl.LEQUAL = 0x0203\n";
  }

  private static String vertexShader() {
    return "#version 330 core\n"
        + "layout(location = 0) in vec4 aPosition;\n"
        + "layout(location = 1) in vec4 aColor;\n"
        + "uniform mat4 uProjection;\n"
        + "uniform mat4 uModelView;\n"
        + "out vec4 vColor;\n"
        + "void main() {\n"
        + "  gl_Position = uProjection * uModelView * aPosition;\n"
        + "  vColor = aColor;\n"
        + "}\n";
  }

  private static String fragmentShader() {
    return "#version 330 core\n"
        + "in vec4 vColor;\n"
        + "out vec4 fragColor;\n"
        + "void main() {\n"
        + "  fragColor = vColor;\n"
        + "}\n";
  }

  private record Vertex(float x, float y, float z, float w, float r, float g, float b, float a) {}
}
