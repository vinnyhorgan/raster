local gl = rs.gl
local time = 0

local function setup_camera()
	local width, height = rs.window.getDimensions()
	gl.viewport(0, 0, width, height)
	gl.matrixMode(gl.PROJECTION)
	gl.loadIdentity()
	gl.ortho(-450, 450, -325, 325, -1, 1)
	gl.matrixMode(gl.MODELVIEW)
	gl.loadIdentity()
end

local function diamond(x, y, radius, r, g, b, a)
	gl.pushMatrix()
	gl.translate(x, y, 0)
	gl.rotate(time * 25 + x * 0.1, 0, 0, 1)
	gl.color4(r, g, b, a)
	gl.begin(gl.QUADS)
	gl.vertex2(0, -radius)
	gl.vertex2(radius, 0)
	gl.vertex2(0, radius)
	gl.vertex2(-radius, 0)
	gl.end_()
	gl.popMatrix()
end

function rs.load()
	setup_camera()
	gl.enable(gl.BLEND)
	gl.blendFunc(gl.SRC_ALPHA, gl.ONE_MINUS_SRC_ALPHA)
end

function rs.update(dt)
	time = time + dt
	if rs.keyboard.isDown("escape") then
		rs.window.close()
	end
end

function rs.draw()
	gl.clearColor(0.02, 0.025, 0.035, 1)
	gl.clear(gl.COLOR_BUFFER_BIT)
	setup_camera()
	for i = 0, 9 do
		local angle = time * 0.7 + i / 10 * math.pi * 2
		diamond(math.cos(angle) * 170, math.sin(angle) * 95, 135, i / 10, 0.35, 1 - i / 12, 0.35)
	end
	diamond(0, 0, 170, 1, 0.85, 0.1, 0.3)
end
