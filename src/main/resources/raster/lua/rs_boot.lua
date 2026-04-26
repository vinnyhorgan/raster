local rs = rawget(_G, "rs") or {}
_G.rs = rs

local function defaults()
  return {
    identity = "raster",
    appendidentity = true,
    modules = {
      window = true,
      timer = true,
      system = true,
      mouse = true,
      keyboard = true,
      filesystem = true,
    },
    window = {
      width = 800,
      height = 600,
      title = "Raster",
      x = nil,
      y = nil,
      minwidth = 1,
      minheight = 1,
      fullscreen = false,
      vsync = true,
      resizable = true,
      borderless = false,
      centered = true,
      visible = true,
    },
  }
end

local function merge_window_title(config)
  if config.title and not config.window.title then
    config.window.title = config.title
  end
end

function rs.boot(mainfile)
  local config = defaults()
  local conf = rs.filesystem.load("conf.lua")
  if conf then
    conf()
    if rs.conf then
      rs.conf(config)
    end
  end
  merge_window_title(config)
  if config.identity then
    rs.filesystem.setIdentity(config.identity)
  end

  local main, err = rs.filesystem.load(mainfile or "main.lua")
  if not main then
    error(err or "missing main.lua", 0)
  end
  main()
  rs.createhandlers()
  return config
end

return rs
