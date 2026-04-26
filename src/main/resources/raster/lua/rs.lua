local rs = rawget(_G, "rs") or {}
_G.rs = rs

rs._version = "0.1.0"
rs._version_major = 0
rs._version_minor = 1
rs._version_revision = 0
rs._version_codename = "bootstrap"

rs.filesystem = rs.filesystem or {}
rs.timer = rs.timer or {}
rs.window = rs.window or {}
rs.system = rs.system or {}
rs.keyboard = rs.keyboard or {}
rs.mouse = rs.mouse or {}

function rs.filesystem.load(path)
  local source = rs.__readFile(path)
  if not source then
    return nil, "file not found: " .. tostring(path)
  end
  return loadstring(source, "@" .. path)
end

function rs.filesystem.lines(path)
  local source = rs.__readFile(path)
  if not source then
    error("file not found: " .. tostring(path), 2)
  end
  local offset = 1
  return function()
    if offset > #source then
      return nil
    end
    local next_newline = source:find("\n", offset, true)
    local line
    if next_newline then
      line = source:sub(offset, next_newline - 1)
      offset = next_newline + 1
    else
      line = source:sub(offset)
      offset = #source + 1
    end
    return (line:gsub("\r$", ""))
  end
end

local function raster_loader(module)
  local path = rs.__findRequire(module)
  if not path then
    return "\n\tno Raster module file for " .. module
  end
  local chunk, err = rs.filesystem.load(path)
  if not chunk then
    error(err, 0)
  end
  return chunk
end

table.insert(package.loaders, 1, raster_loader)

package.preload.rs = function()
  return rs
end

return rs
