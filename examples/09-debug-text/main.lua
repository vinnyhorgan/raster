local gl = rs.gl
local time = 0

local function camera()
	gl.matrixMode(gl.PROJECTION)
	gl.loadIdentity()
	gl.ortho(-450, 450, -320, 320, -1, 1)
	gl.matrixMode(gl.MODELVIEW)
	gl.loadIdentity()
end

local function ribbon()
	gl.begin(gl.TRIANGLE_STRIP)
	for i = 0, 28 do
		local x = -390 + i * 28
		local wave = math.sin(time * 2 + i * 0.45) * 70
		gl.color4(i / 28, 0.35, 1 - i / 36, 0.95)
		gl.vertex2(x, wave - 42)
		gl.vertex2(x, wave + 42)
	end
	gl.end_()
end

function rs.update(dt)
	time = time + dt
	if rs.keyboard.isDown("escape") then
		rs.window.close()
	end
end

function rs.draw()
	gl.clearColor(0.018, 0.02, 0.032, 1)
	gl.clear(gl.COLOR_BUFFER_BIT)
	camera()
	ribbon()
	rs.debug.print("Raster debug text", 28, 28, 30, 1, 0.86, 0.25, 1, true)
	rs.debug.print("Inter Regular via LWJGL STB TrueType", 30, 72, 17, 0.82, 0.88, 1, 1)
	rs.debug.print(string.format("time %.2f", time), 30, 104, 15, 0.45, 1, 0.72, 1)
end
