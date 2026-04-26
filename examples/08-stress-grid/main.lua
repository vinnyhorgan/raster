local gl = rs.gl
local time = 0

local function setup_camera()
	local width, height = rs.window.getDimensions()
	gl.viewport(0, 0, width, height)
	gl.matrixMode(gl.PROJECTION)
	gl.loadIdentity()
	gl.ortho(0, 1000, 760, 0, -1, 1)
	gl.matrixMode(gl.MODELVIEW)
	gl.loadIdentity()
end

local function cell(x, y, size, phase)
	local pulse = (math.sin(time * 2 + phase) + 1) * 0.5
	gl.color4(0.15 + pulse * 0.7, x / 1000, y / 760, 1)
	gl.vertex2(x, y)
	gl.vertex2(x + size, y)
	gl.vertex2(x + size, y + size)
	gl.vertex2(x, y + size)
end

function rs.update(dt)
	time = time + dt
	if rs.keyboard.isDown("escape") then
		rs.window.close()
	end
end

function rs.draw()
	gl.clearColor(0.01, 0.012, 0.018, 1)
	gl.clear(gl.COLOR_BUFFER_BIT)
	setup_camera()
	gl.begin(gl.QUADS)
	for y = 30, 690, 42 do
		for x = 30, 930, 42 do
			cell(x, y, 32, x * 0.02 + y * 0.03)
		end
	end
	gl.end_()
end
