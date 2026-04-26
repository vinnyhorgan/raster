local gl = rs.gl
local time = 0

local function camera()
	local width, height = rs.window.getDimensions()
	gl.viewport(0, 0, width, height)
	gl.matrixMode(gl.PROJECTION)
	gl.loadIdentity()
	gl.ortho(-490, 490, -340, 340, -3, 3)
	gl.matrixMode(gl.MODELVIEW)
	gl.loadIdentity()
end

local function square(x, y, size, r, g, b, a)
	gl.color4(r, g, b, a)
	gl.begin(gl.QUADS)
	gl.vertex3(x - size, y - size, 0)
	gl.vertex3(x + size, y - size, 0)
	gl.vertex3(x + size, y + size, 0)
	gl.vertex3(x - size, y + size, 0)
	gl.end_()
end

function rs.update(dt)
	time = time + dt
	if rs.keyboard.isDown("escape") then
		rs.window.close()
	end
end

function rs.draw()
	camera()
	gl.clearColor(0.02, 0.018, 0.03, 1)
	gl.clear(gl.COLOR_BUFFER_BIT + gl.DEPTH_BUFFER_BIT)
	gl.disable(gl.BLEND)
	gl.disable(gl.DEPTH_TEST)
	square(-260, 0, 135, 0.95, 0.35, 0.55, 1)
	gl.enable(gl.BLEND)
	gl.blendFunc(gl.SRC_ALPHA, gl.ONE_MINUS_SRC_ALPHA)
	for i = 0, 5 do
		gl.pushMatrix()
		gl.translate(0, 0, 0)
		gl.rotate(time * 30 + i * 20, 0, 0, 1)
		square(0, 0, 118 - i * 12, 0.2 + i * 0.1, 0.85, 1, 0.18)
		gl.popMatrix()
	end
	gl.disable(gl.BLEND)
	gl.enable(gl.DEPTH_TEST)
	gl.depthFunc(gl.LESS)
	gl.pushMatrix()
	gl.translate(260, 0, 0)
	gl.rotate(time * 38, 0, 1, 0)
	square(0, 0, 112, 1, 0.82, 0.25, 1)
	gl.translate(0, 0, -0.5)
	gl.rotate(35, 0, 0, 1)
	square(0, 0, 112, 0.28, 0.55, 1, 1)
	gl.popMatrix()
	gl.disable(gl.DEPTH_TEST)
	rs.debug.print("Explicitly toggles blend and depth state in one frame.", 24, 24, 17, 0.84, 0.9, 1, 1)
end
