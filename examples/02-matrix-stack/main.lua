local gl = rs.gl
local time = 0

local function setup_camera()
	local width, height = rs.window.getDimensions()
	gl.viewport(0, 0, width, height)
	gl.matrixMode(gl.PROJECTION)
	gl.loadIdentity()
	gl.ortho(-450, 450, -350, 350, -1, 1)
	gl.matrixMode(gl.MODELVIEW)
	gl.loadIdentity()
end

local function quad(size, r, g, b)
	gl.color4(r, g, b, 1)
	gl.begin(gl.QUADS)
	gl.vertex2(-size, -size)
	gl.vertex2(size, -size)
	gl.vertex2(size, size)
	gl.vertex2(-size, size)
	gl.end_()
end

local function orbit(radius, speed, size, r, g, b)
	gl.pushMatrix()
	gl.rotate(time * speed, 0, 0, 1)
	gl.translate(radius, 0, 0)
	gl.rotate(time * speed * 2, 0, 0, 1)
	quad(size, r, g, b)
	gl.popMatrix()
end

function rs.load()
	setup_camera()
end

function rs.update(dt)
	time = time + dt
	if rs.keyboard.isDown("escape") then
		rs.window.close()
	end
end

function rs.draw()
	gl.clearColor(0.02, 0.02, 0.035, 1)
	gl.clear(gl.COLOR_BUFFER_BIT)
	setup_camera()
	gl.rotate(time * 12, 0, 0, 1)
	quad(70, 1, 0.75, 0.2)
	orbit(170, 55, 36, 0.25, 0.7, 1)
	orbit(270, -32, 28, 1, 0.25, 0.45)
	gl.pushMatrix()
	gl.rotate(-time * 38, 0, 0, 1)
	gl.translate(270, 0, 0)
	orbit(72, 120, 14, 0.7, 1, 0.5)
	gl.popMatrix()
end
