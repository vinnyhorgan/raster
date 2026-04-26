package dev.dvh.raster.vfs;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class VirtualFileSystem {

  private final Path workingDirectory = Path.of("").toAbsolutePath().normalize();
  private final Path userDirectory =
      Path.of(System.getProperty("user.home", ".")).toAbsolutePath().normalize();
  private final Path appDataDirectory;
  private final List<Path> readRoots = new ArrayList<>();
  private Path sourceDirectory;
  private Path saveDirectory;
  private String identity = "raster";
  private String requirePath = "?.lua;?/init.lua";

  public VirtualFileSystem(Path sourceDirectory) {
    this.sourceDirectory = sourceDirectory.toAbsolutePath().normalize();
    readRoots.add(this.sourceDirectory);
    String xdg = System.getenv("XDG_DATA_HOME");
    appDataDirectory =
        xdg == null || xdg.isBlank() ? userDirectory.resolve(".local/share") : Path.of(xdg);
    setIdentity(identity);
  }

  public Path sourceDirectory() {
    return sourceDirectory;
  }

  public String identity() {
    return identity;
  }

  public void setIdentity(String identity) {
    if (identity == null || identity.isBlank()) {
      return;
    }
    this.identity = sanitize(identity);
    saveDirectory = appDataDirectory.resolve("raster").resolve(this.identity).normalize();
  }

  public Path saveDirectory() {
    return saveDirectory;
  }

  public Path workingDirectory() {
    return workingDirectory;
  }

  public Path userDirectory() {
    return userDirectory;
  }

  public Path appDataDirectory() {
    return appDataDirectory;
  }

  public String requirePath() {
    return requirePath;
  }

  public void setRequirePath(String requirePath) {
    if (requirePath != null && !requirePath.isBlank()) {
      this.requirePath = requirePath;
    }
  }

  public boolean mount(Path path) {
    Path root = path.toAbsolutePath().normalize();
    if (!Files.isDirectory(root) || readRoots.contains(root)) {
      return false;
    }
    readRoots.add(root);
    return true;
  }

  public boolean unmount(Path path) {
    Path root = path.toAbsolutePath().normalize();
    if (root.equals(sourceDirectory)) {
      return false;
    }
    return readRoots.remove(root);
  }

  public boolean exists(String virtualPath) {
    return resolveReadable(virtualPath) != null;
  }

  public String read(String virtualPath) {
    Path path = resolveReadable(virtualPath);
    if (path == null) {
      return null;
    }
    try {
      return Files.readString(path, UTF_8);
    } catch (IOException e) {
      throw new IllegalStateException("Unable to read " + virtualPath, e);
    }
  }

  public void write(String virtualPath, String data, boolean append) {
    Path path = resolveWritable(virtualPath);
    try {
      Files.createDirectories(path.getParent());
      if (append) {
        Files.writeString(
            path,
            data == null ? "" : data,
            UTF_8,
            StandardOpenOption.CREATE,
            StandardOpenOption.APPEND);
      } else {
        Files.writeString(
            path,
            data == null ? "" : data,
            UTF_8,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING);
      }
    } catch (IOException e) {
      throw new IllegalStateException("Unable to write " + virtualPath, e);
    }
  }

  public boolean createDirectory(String virtualPath) {
    try {
      Files.createDirectories(resolveWritable(virtualPath));
      return true;
    } catch (IOException e) {
      return false;
    }
  }

  public boolean remove(String virtualPath) {
    try {
      return Files.deleteIfExists(resolveWritable(virtualPath));
    } catch (IOException e) {
      return false;
    }
  }

  public List<String> directoryItems(String virtualPath) {
    Path path = resolveReadable(virtualPath);
    if (path == null || !Files.isDirectory(path)) {
      return List.of();
    }
    try (var stream = Files.list(path)) {
      return stream.map(item -> item.getFileName().toString()).sorted().toList();
    } catch (IOException e) {
      return List.of();
    }
  }

  public Map<String, String> info(String virtualPath) {
    Path path = resolveReadable(virtualPath);
    if (path == null) {
      return Map.of();
    }
    try {
      Map<String, String> result = new LinkedHashMap<>();
      result.put("type", Files.isDirectory(path) ? "directory" : "file");
      result.put("size", String.valueOf(Files.isRegularFile(path) ? Files.size(path) : 0));
      result.put("modtime", String.valueOf(Files.getLastModifiedTime(path).toMillis() / 1000.0));
      result.put("readonly", String.valueOf(!Files.isWritable(path)));
      return result;
    } catch (IOException e) {
      return Map.of();
    }
  }

  public String findRequire(String module) {
    String modulePath = module.replace('.', '/');
    for (String template : requirePath.split(";")) {
      String candidate = template.replace("?", modulePath);
      if (exists(candidate)) {
        return candidate;
      }
    }
    return null;
  }

  private Path resolveReadable(String virtualPath) {
    Path relative = cleanVirtualPath(virtualPath);
    for (Path root : readRoots) {
      Path path = root.resolve(relative).normalize();
      if (path.startsWith(root) && Files.exists(path)) {
        return path;
      }
    }
    Path savePath = saveDirectory.resolve(relative).normalize();
    if (savePath.startsWith(saveDirectory) && Files.exists(savePath)) {
      return savePath;
    }
    return null;
  }

  private Path resolveWritable(String virtualPath) {
    Path relative = cleanVirtualPath(virtualPath);
    Path path = saveDirectory.resolve(relative).normalize();
    if (!path.startsWith(saveDirectory)) {
      throw new IllegalArgumentException("path escapes save directory: " + virtualPath);
    }
    return path;
  }

  private static Path cleanVirtualPath(String virtualPath) {
    String value = virtualPath == null ? "" : virtualPath.replace('\\', '/');
    if (value.startsWith("/") || value.matches("^[A-Za-z]:.*")) {
      throw new IllegalArgumentException("absolute paths are not virtual paths: " + virtualPath);
    }
    Path path = Path.of(value).normalize();
    if (path.startsWith("..")) {
      throw new IllegalArgumentException("path escapes virtual root: " + virtualPath);
    }
    return path;
  }

  private static String sanitize(String identity) {
    return identity.replaceAll("[^A-Za-z0-9_.-]", "_");
  }
}
