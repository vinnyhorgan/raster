local gl = rs.gl
local time = 0

local function camera()
	gl.matrixMode(gl.PROJECTION)
	gl.loadIdentity()
	gl.ortho(-1, 1, -1, 1, -1, 1)
	gl.matrixMode(gl.MODELVIEW)
	gl.loadIdentity()
end

local function shape(sides, phase)
	gl.begin(gl.TRIANGLE_FAN)
	gl.color3(1, 1, 1)
	gl.vertex2(0, 0)
	for i = 0, sides do
		local angle = phase + i / sides * math.pi * 2
		gl.color3(math.abs(math.cos(angle)), 0.45, math.abs(math.sin(angle)))
		gl.vertex2(math.cos(angle) * 0.8, math.sin(angle) * 0.8)
	end
	gl.end_()
end

local function draw_viewport(x, y, width, height, sides, phase)
	gl.viewport(x, y, width, height)
	camera()
	gl.rotate(time * 45 + phase * 30, 0, 0, 1)
	shape(sides, phase)
end

function rs.update(dt)
	time = time + dt
	if rs.keyboard.isDown("escape") then
		rs.window.close()
	end
end

function rs.draw()
	local width, height = rs.window.getDimensions()
	gl.clearColor(0.03, 0.03, 0.045, 1)
	gl.clear(gl.COLOR_BUFFER_BIT)
	draw_viewport(0, 0, width / 2, height / 2, 3, 0)
	draw_viewport(width / 2, 0, width / 2, height / 2, 4, 0.4)
	draw_viewport(0, height / 2, width / 2, height / 2, 5, 0.8)
	draw_viewport(width / 2, height / 2, width / 2, height / 2, 8, 1.2)
	gl.viewport(0, 0, width, height)
end
