local rs = rawget(_G, "rs") or {}
_G.rs = rs

function rs.createhandlers()
  rs.handlers = {
    keypressed = rs.keypressed,
    keyreleased = rs.keyreleased,
    textinput = rs.textinput,
    mousemoved = rs.mousemoved,
    mousepressed = rs.mousepressed,
    mousereleased = rs.mousereleased,
    wheelmoved = rs.wheelmoved,
    resize = rs.resize,
    focus = rs.focus,
    mousefocus = rs.mousefocus,
    visible = rs.visible,
    quit = rs.quit,
  }
end

function rs.__runLoad(args, rawargs)
  if rs.load then
    return rs.load(args or {}, rawargs or {})
  end
end

function rs.__dispatch(name, ...)
  local handler = rs.handlers and rs.handlers[name]
  if handler then
    return handler(...)
  end
end

function rs.__update(dt)
  if rs.update then
    return rs.update(dt)
  end
end

function rs.__draw()
  if rs.draw then
    return rs.draw()
  end
end

return rs
