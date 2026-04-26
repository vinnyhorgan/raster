package dev.dvh.raster.lua;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class LuaValue {

  public static final int NIL = 0;
  public static final int BOOLEAN = 1;
  public static final int NUMBER = 2;
  public static final int STRING = 3;
  public static final int TABLE = 4;

  public static final LuaValue NIL_VALUE = new LuaValue(NIL, null);

  private final int type;
  private final Object value;

  private LuaValue(int type, Object value) {
    this.type = type;
    this.value = value;
  }

  public static LuaValue nil() {
    return NIL_VALUE;
  }

  public static LuaValue bool(boolean value) {
    return new LuaValue(BOOLEAN, value);
  }

  public static LuaValue number(double value) {
    return new LuaValue(NUMBER, value);
  }

  public static LuaValue string(String value) {
    return value == null ? NIL_VALUE : new LuaValue(STRING, value);
  }

  public static LuaValue table(Map<String, LuaValue> value) {
    return new LuaValue(TABLE, Collections.unmodifiableMap(new LinkedHashMap<>(value)));
  }

  public static LuaValue[] array(LuaValue... values) {
    return values == null ? new LuaValue[0] : values;
  }

  public static LuaValue[] strings(List<String> values) {
    LuaValue[] result = new LuaValue[values.size()];
    for (int i = 0; i < values.size(); i++) {
      result[i] = string(values.get(i));
    }
    return result;
  }

  public int type() {
    return type;
  }

  public boolean isNil() {
    return type == NIL;
  }

  public boolean isBoolean() {
    return type == BOOLEAN;
  }

  public boolean isNumber() {
    return type == NUMBER;
  }

  public boolean isString() {
    return type == STRING;
  }

  public boolean isTable() {
    return type == TABLE;
  }

  public boolean asBoolean() {
    return type == BOOLEAN && (Boolean) value;
  }

  public double asNumber() {
    if (type != NUMBER) {
      return 0;
    }
    return (Double) value;
  }

  public int asInt(int fallback) {
    return type == NUMBER ? (int) Math.round((Double) value) : fallback;
  }

  public String asString() {
    if (type == STRING) {
      return (String) value;
    }
    if (type == NUMBER || type == BOOLEAN) {
      return String.valueOf(value);
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  public Map<String, LuaValue> asTable() {
    if (type != TABLE) {
      return Map.of();
    }
    return (Map<String, LuaValue>) value;
  }

  public LuaValue field(String name) {
    return asTable().getOrDefault(name, NIL_VALUE);
  }

  public String fieldString(String name, String fallback) {
    LuaValue field = field(name);
    String value = field.asString();
    return value == null ? fallback : value;
  }

  public int fieldInt(String name, int fallback) {
    return field(name).asInt(fallback);
  }

  public boolean fieldBoolean(String name, boolean fallback) {
    LuaValue field = field(name);
    return field.isBoolean() ? field.asBoolean() : fallback;
  }

  byte[] stringBytes() {
    String string = asString();
    return string == null ? null : string.getBytes(UTF_8);
  }
}
