local gl = rs.gl
local time = 0

local function translation(x, y, z)
	return {
		1,
		0,
		0,
		0,
		0,
		1,
		0,
		0,
		0,
		0,
		1,
		0,
		x,
		y,
		z,
		1,
	}
end

local function rotation(angle)
	local c = math.cos(angle)
	local s = math.sin(angle)
	return {
		c,
		s,
		0,
		0,
		-s,
		c,
		0,
		0,
		0,
		0,
		1,
		0,
		0,
		0,
		0,
		1,
	}
end

local function triangle(r, g, b)
	gl.begin(gl.TRIANGLES)
	gl.color3(r, g, b)
	gl.vertex2(0, 90)
	gl.color3(b, r, g)
	gl.vertex2(-90, -70)
	gl.color3(g, b, r)
	gl.vertex2(90, -70)
	gl.end_()
end

function rs.update(dt)
	time = time + dt
	if rs.keyboard.isDown("escape") then
		rs.window.close()
	end
end

function rs.draw()
	local width, height = rs.window.getDimensions()
	gl.viewport(0, 0, width, height)
	gl.clearColor(0.025, 0.02, 0.035, 1)
	gl.clear(gl.COLOR_BUFFER_BIT)
	gl.matrixMode(gl.PROJECTION)
	gl.loadIdentity()
	gl.ortho(-450, 450, -325, 325, -1, 1)
	gl.matrixMode(gl.MODELVIEW)
	gl.loadMatrix(translation(-170, 0, 0))
	gl.multMatrix(rotation(time))
	triangle(1, 0.25, 0.25)
	gl.loadMatrix(translation(170, 0, 0))
	gl.multMatrix(rotation(-time * 1.4))
	triangle(0.25, 0.75, 1)
end
