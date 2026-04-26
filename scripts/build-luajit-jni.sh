#!/usr/bin/env sh
set -eu

java_home=$1
native_platform=$2
native_output="target/classes/natives/${native_platform}"

mkdir -p "${native_output}"
cp -L "lj2/lib/libluajit-5.1.so.2" "${native_output}/libluajit-5.1.so.2"

gcc \
  -fPIC \
  -shared \
  -O2 \
  -Wall \
  -Wextra \
  -DCOMPAT53_PREFIX=compat53 \
  -I"${java_home}/include" \
  -I"${java_home}/include/linux" \
  -I"lj2/include/luajit-2.1" \
  -I"lua-compat-5.3" \
  -I"lua-compat-5.3/c-api" \
  "lua-compat-5.3/c-api/compat-5.3.c" \
  "lua-compat-5.3/lutf8lib.c" \
  "lua-compat-5.3/lstrlib.c" \
  "lua-compat-5.3/ltablib.c" \
  "lua-compat-5.3/liolib.c" \
  "src/main/c/raster_luajit.c" \
  -L"lj2/lib" \
  -lluajit-5.1 \
  -lm \
  -Wl,-rpath,'$ORIGIN' \
  -Wl,-z,defs \
  -o "${native_output}/libraster_luajit.so"
