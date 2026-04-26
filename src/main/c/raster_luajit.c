#include <jni.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "compat-5.3.h"
#include "lauxlib.h"
#include "lua.h"
#include "lualib.h"

extern int luaopen_compat53_utf8(lua_State* L);
extern int luaopen_compat53_string(lua_State* L);
extern int luaopen_compat53_table(lua_State* L);
extern int luaopen_compat53_io(lua_State* L);

static JavaVM* raster_vm = NULL;

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {
  (void)reserved;
  raster_vm = vm;
  return JNI_VERSION_1_8;
}

static int absolute_index(lua_State* lua, int index) {
  if (index > 0 || index <= LUA_REGISTRYINDEX) {
    return index;
  }
  return lua_gettop(lua) + index + 1;
}

static lua_State* to_state(JNIEnv* env, jlong state) {
  if (state == 0) {
    jclass type = (*env)->FindClass(env, "java/lang/IllegalStateException");
    if (type != NULL) {
      (*env)->ThrowNew(env, type, "LuaJIT state is closed");
    }
    return NULL;
  }
  return (lua_State*)(intptr_t)state;
}

static void throw_by_name(JNIEnv* env, const char* class_name,
                          const char* message) {
  jclass type = (*env)->FindClass(env, class_name);
  if (type != NULL) {
    (*env)->ThrowNew(env, type, message == NULL ? "LuaJIT error" : message);
  }
}

static const char* get_utf_chars(JNIEnv* env, jstring value) {
  if (value == NULL) {
    return NULL;
  }
  return (*env)->GetStringUTFChars(env, value, NULL);
}

static void release_utf_chars(JNIEnv* env, jstring value, const char* chars) {
  if (value != NULL && chars != NULL) {
    (*env)->ReleaseStringUTFChars(env, value, chars);
  }
}

static jobject make_lua_value(JNIEnv* env, lua_State* lua, int index,
                              int depth);

static int owner_ref_gc(lua_State* lua) {
  if (raster_vm == NULL) return 0;
  JNIEnv* env = NULL;
  if ((*raster_vm)->GetEnv(raster_vm, (void**)&env, JNI_VERSION_1_8) !=
      JNI_OK) {
    return 0;
  }
  jobject* ref = (jobject*)lua_touserdata(lua, 1);
  if (ref != NULL && *ref != NULL) {
    (*env)->DeleteGlobalRef(env, *ref);
    *ref = NULL;
  }
  return 0;
}

static void ensure_owner_ref_metatable(lua_State* lua) {
  if (luaL_newmetatable(lua, "raster.owner_ref")) {
    lua_pushcfunction(lua, owner_ref_gc);
    lua_setfield(lua, -2, "__gc");
  }
  lua_pop(lua, 1);
}

static void push_owner_ref(JNIEnv* env, lua_State* lua, jobject owner) {
  ensure_owner_ref_metatable(lua);
  jobject* ref = (jobject*)lua_newuserdata(lua, sizeof(jobject));
  *ref = (*env)->NewGlobalRef(env, owner);
  luaL_getmetatable(lua, "raster.owner_ref");
  lua_setmetatable(lua, -2);
}

static int string_to_positive_integer_key(const char* chars, lua_Integer* key) {
  if (chars == NULL || *chars == '\0') return 0;
  lua_Integer value = 0;
  for (const char* cursor = chars; *cursor != '\0'; cursor++) {
    if (*cursor < '0' || *cursor > '9') return 0;
    value = value * 10 + (*cursor - '0');
  }
  if (value < 1) return 0;
  *key = value;
  return 1;
}

static jobject make_nil(JNIEnv* env) {
  jclass cls = (*env)->FindClass(env, "dev/dvh/raster/lua/LuaValue");
  if (cls == NULL) return NULL;
  jmethodID method = (*env)->GetStaticMethodID(
      env, cls, "nil", "()Ldev/dvh/raster/lua/LuaValue;");
  if (method == NULL) return NULL;
  return (*env)->CallStaticObjectMethod(env, cls, method);
}

static jobject make_boolean(JNIEnv* env, int value) {
  jclass cls = (*env)->FindClass(env, "dev/dvh/raster/lua/LuaValue");
  if (cls == NULL) return NULL;
  jmethodID method = (*env)->GetStaticMethodID(
      env, cls, "bool", "(Z)Ldev/dvh/raster/lua/LuaValue;");
  if (method == NULL) return NULL;
  return (*env)->CallStaticObjectMethod(env, cls, method,
                                        value ? JNI_TRUE : JNI_FALSE);
}

static jobject make_number(JNIEnv* env, lua_Number value) {
  jclass cls = (*env)->FindClass(env, "dev/dvh/raster/lua/LuaValue");
  if (cls == NULL) return NULL;
  jmethodID method = (*env)->GetStaticMethodID(
      env, cls, "number", "(D)Ldev/dvh/raster/lua/LuaValue;");
  if (method == NULL) return NULL;
  return (*env)->CallStaticObjectMethod(env, cls, method, (jdouble)value);
}

static jobject make_string(JNIEnv* env, const char* bytes, size_t length) {
  jclass string_cls = (*env)->FindClass(env, "java/lang/String");
  if (string_cls == NULL) return NULL;
  jbyteArray data = (*env)->NewByteArray(env, (jsize)length);
  if (data == NULL) return NULL;
  (*env)->SetByteArrayRegion(env, data, 0, (jsize)length, (const jbyte*)bytes);
  jmethodID string_ctor =
      (*env)->GetMethodID(env, string_cls, "<init>", "([BLjava/lang/String;)V");
  if (string_ctor == NULL) return NULL;
  jstring charset = (*env)->NewStringUTF(env, "UTF-8");
  jstring value =
      (*env)->NewObject(env, string_cls, string_ctor, data, charset);
  if (value == NULL) return NULL;

  jclass cls = (*env)->FindClass(env, "dev/dvh/raster/lua/LuaValue");
  if (cls == NULL) return NULL;
  jmethodID method = (*env)->GetStaticMethodID(
      env, cls, "string", "(Ljava/lang/String;)Ldev/dvh/raster/lua/LuaValue;");
  if (method == NULL) return NULL;
  return (*env)->CallStaticObjectMethod(env, cls, method, value);
}

static jobject make_table(JNIEnv* env, lua_State* lua, int index, int depth) {
  jclass map_cls = (*env)->FindClass(env, "java/util/LinkedHashMap");
  if (map_cls == NULL) return NULL;
  jmethodID map_ctor = (*env)->GetMethodID(env, map_cls, "<init>", "()V");
  jmethodID put = (*env)->GetMethodID(
      env, map_cls, "put",
      "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
  if (map_ctor == NULL || put == NULL) return NULL;
  jobject map = (*env)->NewObject(env, map_cls, map_ctor);
  if (map == NULL) return NULL;

  int absolute = absolute_index(lua, index);
  lua_pushnil(lua);
  while (lua_next(lua, absolute) != 0) {
    jstring key = NULL;
    if (lua_type(lua, -2) == LUA_TSTRING) {
      size_t key_len = 0;
      const char* key_bytes = lua_tolstring(lua, -2, &key_len);
      char* copy = malloc(key_len + 1);
      if (copy != NULL) {
        memcpy(copy, key_bytes, key_len);
        copy[key_len] = '\0';
        key = (*env)->NewStringUTF(env, copy);
        free(copy);
      }
    } else if (lua_type(lua, -2) == LUA_TNUMBER) {
      char buffer[32];
      snprintf(buffer, sizeof(buffer), "%d", (int)lua_tointeger(lua, -2));
      key = (*env)->NewStringUTF(env, buffer);
    }
    if (key != NULL) {
      jobject value = make_lua_value(env, lua, -1, depth + 1);
      if ((*env)->ExceptionCheck(env)) {
        lua_pop(lua, 1);
        return NULL;
      }
      (*env)->CallObjectMethod(env, map, put, key, value);
      if ((*env)->ExceptionCheck(env)) {
        lua_pop(lua, 1);
        return NULL;
      }
    }
    lua_pop(lua, 1);
  }

  jclass cls = (*env)->FindClass(env, "dev/dvh/raster/lua/LuaValue");
  if (cls == NULL) return NULL;
  jmethodID method = (*env)->GetStaticMethodID(
      env, cls, "table", "(Ljava/util/Map;)Ldev/dvh/raster/lua/LuaValue;");
  if (method == NULL) return NULL;
  return (*env)->CallStaticObjectMethod(env, cls, method, map);
}

static jobject make_lua_value(JNIEnv* env, lua_State* lua, int index,
                              int depth) {
  if (depth > 8) return make_nil(env);
  switch (lua_type(lua, index)) {
    case LUA_TNIL:
    case LUA_TNONE:
      return make_nil(env);
    case LUA_TBOOLEAN:
      return make_boolean(env, lua_toboolean(lua, index));
    case LUA_TNUMBER:
      return make_number(env, lua_tonumber(lua, index));
    case LUA_TSTRING: {
      size_t length = 0;
      const char* bytes = lua_tolstring(lua, index, &length);
      return make_string(env, bytes, length);
    }
    case LUA_TTABLE:
      return make_table(env, lua, index, depth);
    default:
      return make_nil(env);
  }
}

static jobjectArray make_argument_array(JNIEnv* env, lua_State* lua, int first,
                                        int count) {
  jclass value_cls = (*env)->FindClass(env, "dev/dvh/raster/lua/LuaValue");
  if (value_cls == NULL) return NULL;
  jobjectArray result = (*env)->NewObjectArray(env, count, value_cls, NULL);
  if (result == NULL) return NULL;
  for (int i = 0; i < count; i++) {
    jobject value = make_lua_value(env, lua, first + i, 0);
    if ((*env)->ExceptionCheck(env)) return NULL;
    (*env)->SetObjectArrayElement(env, result, i, value);
  }
  return result;
}

static void push_java_value(JNIEnv* env, lua_State* lua, jobject value) {
  if (value == NULL) {
    lua_pushnil(lua);
    return;
  }
  jclass cls = (*env)->FindClass(env, "dev/dvh/raster/lua/LuaValue");
  jmethodID type = (*env)->GetMethodID(env, cls, "type", "()I");
  jint value_type = (*env)->CallIntMethod(env, value, type);
  if ((*env)->ExceptionCheck(env)) return;
  if (value_type == 0) {
    lua_pushnil(lua);
  } else if (value_type == 1) {
    jmethodID as_boolean = (*env)->GetMethodID(env, cls, "asBoolean", "()Z");
    lua_pushboolean(lua, (*env)->CallBooleanMethod(env, value, as_boolean));
  } else if (value_type == 2) {
    jmethodID as_number = (*env)->GetMethodID(env, cls, "asNumber", "()D");
    lua_pushnumber(lua, (*env)->CallDoubleMethod(env, value, as_number));
  } else if (value_type == 3) {
    jmethodID as_string =
        (*env)->GetMethodID(env, cls, "asString", "()Ljava/lang/String;");
    jstring string = (*env)->CallObjectMethod(env, value, as_string);
    const char* chars = get_utf_chars(env, string);
    if (chars == NULL) {
      lua_pushnil(lua);
    } else {
      lua_pushstring(lua, chars);
      release_utf_chars(env, string, chars);
    }
  } else if (value_type == 4) {
    jmethodID as_table =
        (*env)->GetMethodID(env, cls, "asTable", "()Ljava/util/Map;");
    jobject map = (*env)->CallObjectMethod(env, value, as_table);
    jclass map_cls = (*env)->FindClass(env, "java/util/Map");
    jclass set_cls = (*env)->FindClass(env, "java/util/Set");
    jclass iterator_cls = (*env)->FindClass(env, "java/util/Iterator");
    jclass entry_cls = (*env)->FindClass(env, "java/util/Map$Entry");
    jmethodID entry_set =
        (*env)->GetMethodID(env, map_cls, "entrySet", "()Ljava/util/Set;");
    jmethodID iterator =
        (*env)->GetMethodID(env, set_cls, "iterator", "()Ljava/util/Iterator;");
    jmethodID has_next =
        (*env)->GetMethodID(env, iterator_cls, "hasNext", "()Z");
    jmethodID next =
        (*env)->GetMethodID(env, iterator_cls, "next", "()Ljava/lang/Object;");
    jmethodID get_key =
        (*env)->GetMethodID(env, entry_cls, "getKey", "()Ljava/lang/Object;");
    jmethodID get_value =
        (*env)->GetMethodID(env, entry_cls, "getValue", "()Ljava/lang/Object;");
    lua_newtable(lua);
    jobject set = (*env)->CallObjectMethod(env, map, entry_set);
    jobject it = (*env)->CallObjectMethod(env, set, iterator);
    while ((*env)->CallBooleanMethod(env, it, has_next)) {
      jobject entry = (*env)->CallObjectMethod(env, it, next);
      jstring key = (*env)->CallObjectMethod(env, entry, get_key);
      jobject child = (*env)->CallObjectMethod(env, entry, get_value);
      const char* chars = get_utf_chars(env, key);
      if (chars != NULL) {
        lua_Integer integer_key = 0;
        if (string_to_positive_integer_key(chars, &integer_key)) {
          lua_pushinteger(lua, integer_key);
        } else {
          lua_pushstring(lua, chars);
        }
        push_java_value(env, lua, child);
        lua_settable(lua, -3);
        release_utf_chars(env, key, chars);
      }
    }
  } else {
    lua_pushnil(lua);
  }
}

static int java_function_dispatch(lua_State* lua) {
  JNIEnv* env = NULL;
  if (raster_vm == NULL ||
      (*raster_vm)->GetEnv(raster_vm, (void**)&env, JNI_VERSION_1_8) !=
          JNI_OK) {
    return luaL_error(lua, "Java VM is unavailable");
  }
  jobject* owner_ref = (jobject*)lua_touserdata(lua, lua_upvalueindex(1));
  jobject owner = owner_ref == NULL ? NULL : *owner_ref;
  if (owner == NULL) {
    return luaL_error(lua, "Java callback owner is unavailable");
  }
  int function_id = (int)lua_tointeger(lua, lua_upvalueindex(2));
  int count = lua_gettop(lua);
  jobjectArray args = make_argument_array(env, lua, 1, count);
  if ((*env)->ExceptionCheck(env))
    return luaL_error(lua, "Java callback argument conversion failed");
  jclass owner_cls = (*env)->GetObjectClass(env, owner);
  jmethodID invoke = (*env)->GetMethodID(
      env, owner_cls, "invoke", "(I[Ljava/lang/Object;)[Ljava/lang/Object;");
  jobjectArray result =
      (*env)->CallObjectMethod(env, owner, invoke, function_id, args);
  if ((*env)->ExceptionCheck(env)) {
    (*env)->ExceptionDescribe(env);
    (*env)->ExceptionClear(env);
    return luaL_error(lua, "Java callback failed");
  }
  if (result == NULL) return 0;
  jsize length = (*env)->GetArrayLength(env, result);
  for (jsize i = 0; i < length; i++) {
    jobject value = (*env)->GetObjectArrayElement(env, result, i);
    push_java_value(env, lua, value);
    if ((*env)->ExceptionCheck(env))
      return luaL_error(lua, "Java return conversion failed");
  }
  return (int)length;
}

JNIEXPORT jlong JNICALL Java_dev_dvh_raster_lua_LuaJit_open(JNIEnv* env,
                                                            jclass cls) {
  (void)cls;
  lua_State* state = luaL_newstate();
  if (state == NULL) {
    throw_by_name(env, "java/lang/OutOfMemoryError",
                  "Unable to allocate LuaJIT state");
    return 0;
  }
  return (jlong)(intptr_t)state;
}

JNIEXPORT void JNICALL Java_dev_dvh_raster_lua_LuaJit_close(JNIEnv* env,
                                                            jclass cls,
                                                            jlong state) {
  (void)env;
  (void)cls;
  if (state != 0) {
    lua_close((lua_State*)(intptr_t)state);
  }
}

JNIEXPORT void JNICALL Java_dev_dvh_raster_lua_LuaJit_openLibraries(
    JNIEnv* env, jclass cls, jlong state) {
  (void)cls;
  lua_State* lua = to_state(env, state);
  if (lua == NULL) {
    return;
  }
  luaL_openlibs(lua);
  luaL_requiref(lua, "compat53.utf8", luaopen_compat53_utf8, 0);
  luaL_requiref(lua, "compat53.string", luaopen_compat53_string, 0);
  luaL_requiref(lua, "compat53.table", luaopen_compat53_table, 0);
  luaL_requiref(lua, "compat53.io", luaopen_compat53_io, 0);
  lua_pop(lua, 4);
}

JNIEXPORT void JNICALL
Java_dev_dvh_raster_lua_LuaJit_execute(JNIEnv* env, jclass cls, jlong state,
                                       jbyteArray source, jstring chunk_name) {
  (void)cls;
  lua_State* lua = to_state(env, state);
  if (lua == NULL) {
    return;
  }
  if (source == NULL) {
    throw_by_name(env, "java/lang/NullPointerException", "source");
    return;
  }

  const char* name = get_utf_chars(env, chunk_name);
  if ((*env)->ExceptionCheck(env)) {
    return;
  }

  jsize length = (*env)->GetArrayLength(env, source);
  jbyte* bytes = (*env)->GetByteArrayElements(env, source, NULL);
  if (bytes == NULL) {
    release_utf_chars(env, chunk_name, name);
    return;
  }

  const char* chunk = name == NULL ? "=(java)" : name;
  int status = luaL_loadbuffer(lua, (const char*)bytes, (size_t)length, chunk);
  (*env)->ReleaseByteArrayElements(env, source, bytes, JNI_ABORT);
  release_utf_chars(env, chunk_name, name);

  if (status == 0) {
    status = lua_pcall(lua, 0, 0, 0);
  }
  if (status != 0) {
    const char* message = lua_tostring(lua, -1);
    throw_by_name(env, "dev/dvh/raster/lua/LuaJitException", message);
    lua_pop(lua, 1);
  }
}

JNIEXPORT void JNICALL Java_dev_dvh_raster_lua_LuaJit_setGlobalNil(
    JNIEnv* env, jclass cls, jlong state, jstring name) {
  (void)cls;
  lua_State* lua = to_state(env, state);
  if (lua == NULL) {
    return;
  }
  const char* key = get_utf_chars(env, name);
  if (key == NULL) {
    return;
  }
  lua_pushnil(lua);
  lua_setglobal(lua, key);
  release_utf_chars(env, name, key);
}

JNIEXPORT void JNICALL Java_dev_dvh_raster_lua_LuaJit_setGlobalString(
    JNIEnv* env, jclass cls, jlong state, jstring name, jbyteArray value) {
  (void)cls;
  lua_State* lua = to_state(env, state);
  if (lua == NULL) {
    return;
  }
  const char* key = get_utf_chars(env, name);
  if (key == NULL) {
    return;
  }
  if (value == NULL) {
    lua_pushnil(lua);
  } else {
    jsize length = (*env)->GetArrayLength(env, value);
    jbyte* bytes = (*env)->GetByteArrayElements(env, value, NULL);
    if (bytes == NULL) {
      release_utf_chars(env, name, key);
      return;
    }
    lua_pushlstring(lua, (const char*)bytes, (size_t)length);
    (*env)->ReleaseByteArrayElements(env, value, bytes, JNI_ABORT);
  }
  lua_setglobal(lua, key);
  release_utf_chars(env, name, key);
}

JNIEXPORT void JNICALL Java_dev_dvh_raster_lua_LuaJit_setGlobalNumber(
    JNIEnv* env, jclass cls, jlong state, jstring name, jdouble value) {
  (void)cls;
  lua_State* lua = to_state(env, state);
  if (lua == NULL) {
    return;
  }
  const char* key = get_utf_chars(env, name);
  if (key == NULL) {
    return;
  }
  lua_pushnumber(lua, (lua_Number)value);
  lua_setglobal(lua, key);
  release_utf_chars(env, name, key);
}

JNIEXPORT void JNICALL Java_dev_dvh_raster_lua_LuaJit_setGlobalBoolean(
    JNIEnv* env, jclass cls, jlong state, jstring name, jboolean value) {
  (void)cls;
  lua_State* lua = to_state(env, state);
  if (lua == NULL) {
    return;
  }
  const char* key = get_utf_chars(env, name);
  if (key == NULL) {
    return;
  }
  lua_pushboolean(lua, value == JNI_TRUE);
  lua_setglobal(lua, key);
  release_utf_chars(env, name, key);
}

JNIEXPORT jbyteArray JNICALL Java_dev_dvh_raster_lua_LuaJit_getGlobalString(
    JNIEnv* env, jclass cls, jlong state, jstring name) {
  (void)cls;
  lua_State* lua = to_state(env, state);
  if (lua == NULL) {
    return NULL;
  }
  const char* key = get_utf_chars(env, name);
  if (key == NULL) {
    return NULL;
  }

  lua_getglobal(lua, key);
  release_utf_chars(env, name, key);
  if (lua_isnil(lua, -1)) {
    lua_pop(lua, 1);
    return NULL;
  }

  size_t length = 0;
  const char* bytes = lua_tolstring(lua, -1, &length);
  if (bytes == NULL || length > INT32_MAX) {
    lua_pop(lua, 1);
    throw_by_name(env, "dev/dvh/raster/lua/LuaJitException",
                  "global is not a string");
    return NULL;
  }

  jbyteArray result = (*env)->NewByteArray(env, (jsize)length);
  if (result != NULL) {
    (*env)->SetByteArrayRegion(env, result, 0, (jsize)length,
                               (const jbyte*)bytes);
  }
  lua_pop(lua, 1);
  return result;
}

JNIEXPORT jdouble JNICALL Java_dev_dvh_raster_lua_LuaJit_getGlobalNumber(
    JNIEnv* env, jclass cls, jlong state, jstring name) {
  (void)cls;
  lua_State* lua = to_state(env, state);
  if (lua == NULL) {
    return 0;
  }
  const char* key = get_utf_chars(env, name);
  if (key == NULL) {
    return 0;
  }

  lua_getglobal(lua, key);
  release_utf_chars(env, name, key);
  if (!lua_isnumber(lua, -1)) {
    lua_pop(lua, 1);
    throw_by_name(env, "dev/dvh/raster/lua/LuaJitException",
                  "global is not a number");
    return 0;
  }
  lua_Number value = lua_tonumber(lua, -1);
  lua_pop(lua, 1);
  return (jdouble)value;
}

JNIEXPORT jboolean JNICALL Java_dev_dvh_raster_lua_LuaJit_getGlobalBoolean(
    JNIEnv* env, jclass cls, jlong state, jstring name) {
  (void)cls;
  lua_State* lua = to_state(env, state);
  if (lua == NULL) {
    return JNI_FALSE;
  }
  const char* key = get_utf_chars(env, name);
  if (key == NULL) {
    return JNI_FALSE;
  }

  lua_getglobal(lua, key);
  release_utf_chars(env, name, key);
  if (!lua_isboolean(lua, -1)) {
    lua_pop(lua, 1);
    throw_by_name(env, "dev/dvh/raster/lua/LuaJitException",
                  "global is not a boolean");
    return JNI_FALSE;
  }
  int value = lua_toboolean(lua, -1);
  lua_pop(lua, 1);
  return value ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jobjectArray JNICALL Java_dev_dvh_raster_lua_LuaJit_call(
    JNIEnv* env, jclass cls, jlong state, jstring expression,
    jobjectArray arguments) {
  (void)cls;
  lua_State* lua = to_state(env, state);
  if (lua == NULL) {
    return NULL;
  }
  const char* expr = get_utf_chars(env, expression);
  if (expr == NULL) {
    return NULL;
  }
  size_t expr_len = strlen(expr);
  const char prefix[] = "return ";
  char* source = malloc(sizeof(prefix) + expr_len);
  if (source == NULL) {
    release_utf_chars(env, expression, expr);
    throw_by_name(env, "java/lang/OutOfMemoryError",
                  "Unable to build Lua call");
    return NULL;
  }
  memcpy(source, prefix, sizeof(prefix) - 1);
  memcpy(source + sizeof(prefix) - 1, expr, expr_len + 1);

  int status = luaL_loadbuffer(lua, source, strlen(source), "=(java call)");
  free(source);
  release_utf_chars(env, expression, expr);
  if (status == 0) {
    status = lua_pcall(lua, 0, 1, 0);
  }
  if (status != 0) {
    const char* message = lua_tostring(lua, -1);
    throw_by_name(env, "dev/dvh/raster/lua/LuaJitException", message);
    lua_pop(lua, 1);
    return NULL;
  }
  if (!lua_isfunction(lua, -1)) {
    lua_pop(lua, 1);
    jclass object_cls = (*env)->FindClass(env, "java/lang/Object");
    return (*env)->NewObjectArray(env, 0, object_cls, NULL);
  }

  jsize count = arguments == NULL ? 0 : (*env)->GetArrayLength(env, arguments);
  for (jsize i = 0; i < count; i++) {
    jobject value = (*env)->GetObjectArrayElement(env, arguments, i);
    push_java_value(env, lua, value);
    if ((*env)->ExceptionCheck(env)) {
      lua_pop(lua, (int)i + 1);
      return NULL;
    }
  }
  int base = lua_gettop(lua) - (int)count;
  status = lua_pcall(lua, (int)count, LUA_MULTRET, 0);
  if (status != 0) {
    const char* message = lua_tostring(lua, -1);
    throw_by_name(env, "dev/dvh/raster/lua/LuaJitException", message);
    lua_pop(lua, 1);
    return NULL;
  }
  int result_count = lua_gettop(lua) - base + 1;
  if (result_count < 0) result_count = 0;
  jclass value_cls = (*env)->FindClass(env, "dev/dvh/raster/lua/LuaValue");
  if (value_cls == NULL) return NULL;
  jobjectArray result =
      (*env)->NewObjectArray(env, result_count, value_cls, NULL);
  if (result == NULL) return NULL;
  for (int i = 0; i < result_count; i++) {
    jobject value = make_lua_value(env, lua, base + i, 0);
    if ((*env)->ExceptionCheck(env)) return NULL;
    (*env)->SetObjectArrayElement(env, result, i, value);
  }
  lua_pop(lua, result_count);
  return result;
}

JNIEXPORT void JNICALL Java_dev_dvh_raster_lua_LuaJit_registerFunction(
    JNIEnv* env, jclass cls, jlong state, jobject owner, jint function_id,
    jstring qualified_name) {
  (void)cls;
  lua_State* lua = to_state(env, state);
  if (lua == NULL) {
    return;
  }
  if (owner == NULL) {
    throw_by_name(env, "java/lang/NullPointerException", "owner");
    return;
  }
  const char* name = get_utf_chars(env, qualified_name);
  if (name == NULL) {
    return;
  }
  char* copy = strdup(name);
  release_utf_chars(env, qualified_name, name);
  if (copy == NULL) {
    throw_by_name(env, "java/lang/OutOfMemoryError",
                  "Unable to copy function name");
    return;
  }

  char* save = NULL;
  char* token = strtok_r(copy, ".", &save);
  if (token == NULL) {
    free(copy);
    throw_by_name(env, "java/lang/IllegalArgumentException",
                  "empty function name");
    return;
  }
  lua_getglobal(lua, token);
  if (!lua_istable(lua, -1)) {
    lua_pop(lua, 1);
    lua_newtable(lua);
    lua_pushvalue(lua, -1);
    lua_setglobal(lua, token);
  }

  char* next = strtok_r(NULL, ".", &save);
  while (next != NULL) {
    char* after = strtok_r(NULL, ".", &save);
    if (after == NULL) {
      push_owner_ref(env, lua, owner);
      lua_pushinteger(lua, function_id);
      lua_pushcclosure(lua, java_function_dispatch, 2);
      lua_setfield(lua, -2, next);
      lua_pop(lua, 1);
      free(copy);
      return;
    }
    lua_getfield(lua, -1, next);
    if (!lua_istable(lua, -1)) {
      lua_pop(lua, 1);
      lua_newtable(lua);
      lua_pushvalue(lua, -1);
      lua_setfield(lua, -3, next);
    }
    lua_remove(lua, -2);
    next = after;
  }

  push_owner_ref(env, lua, owner);
  lua_pushinteger(lua, function_id);
  lua_pushcclosure(lua, java_function_dispatch, 2);
  lua_setfield(lua, -2, token);
  lua_pop(lua, 1);
  free(copy);
}
