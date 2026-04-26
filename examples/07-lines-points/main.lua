local gl = rs.gl
local time = 0

local function setup_camera()
	local width, height = rs.window.getDimensions()
	gl.viewport(0, 0, width, height)
	gl.matrixMode(gl.PROJECTION)
	gl.loadIdentity()
	gl.ortho(0, 1000, 700, 0, -1, 1)
	gl.matrixMode(gl.MODELVIEW)
	gl.loadIdentity()
end

function rs.update(dt)
	time = time + dt
	if rs.keyboard.isDown("escape") then
		rs.window.close()
	end
end

function rs.draw()
	gl.clearColor(0.018, 0.024, 0.032, 1)
	gl.clear(gl.COLOR_BUFFER_BIT)
	setup_camera()
	for row = 0, 5 do
		gl.lineWidth(row + 1)
		gl.color3(0.25 + row * 0.12, 0.85, 1 - row * 0.1)
		gl.begin(gl.LINE_STRIP)
		for i = 0, 32 do
			gl.vertex2(80 + i * 26, 90 + row * 70 + math.sin(time * 2 + i * 0.35 + row) * 24)
		end
		gl.end_()
	end
	gl.pointSize(14)
	gl.begin(gl.POINTS)
	for i = 0, 23 do
		local angle = time + i / 24 * math.pi * 2
		gl.color3(1, i / 24, 0.25)
		gl.vertex2(500 + math.cos(angle) * 280, 560 + math.sin(angle) * 70)
	end
	gl.end_()
	gl.pointSize(1)
	gl.lineWidth(1)
end
