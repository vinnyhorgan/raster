#include <jni.h>
#include <stdint.h>
#include <string.h>

#include "lauxlib.h"
#include "lua.h"
#include "lualib.h"

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
