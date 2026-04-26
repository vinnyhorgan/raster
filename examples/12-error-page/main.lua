local elapsed = 0

function rs.update(dt)
	elapsed = elapsed + dt
	if elapsed > 1.2 then
		error("Intentional example crash: the Raster error page catches Lua failures and keeps the window open.")
	end
	if rs.keyboard.isDown("escape") then
		rs.window.close()
	end
end

function rs.draw()
	local width, height = rs.window.getDimensions()
	rs.gl.viewport(0, 0, width, height)
	rs.gl.clearColor(0.05, 0.045, 0.08, 1)
	rs.gl.clear(rs.gl.COLOR_BUFFER_BIT)
	rs.debug.print("This example intentionally crashes in a moment...", 40, 44, 22, 1, 0.86, 0.36, 1, true)
	rs.debug.print("It exists to test the Love-like Raster error page.", 42, 84, 17, 0.82, 0.88, 1, 1)
end
