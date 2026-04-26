local gl = rs.gl
local time = 0

local function setup_camera()
	gl.matrixMode(gl.PROJECTION)
	gl.loadIdentity()
	gl.frustum(-1, 1, -0.78, 0.78, 1, 20)
	gl.matrixMode(gl.MODELVIEW)
	gl.loadIdentity()
	gl.translate(0, 0, -5)
end

local function face(r, g, b)
	gl.color4(r, g, b, 1)
	gl.begin(gl.QUADS)
	gl.vertex3(-1, -1, 0)
	gl.vertex3(1, -1, 0)
	gl.vertex3(1, 1, 0)
	gl.vertex3(-1, 1, 0)
	gl.end_()
end

local function cube()
	gl.pushMatrix()
	gl.rotate(time * 35, 0, 1, 0)
	gl.rotate(time * 21, 1, 0, 0)
	gl.pushMatrix()
	gl.translate(0, 0, 1)
	face(1, 0.2, 0.2)
	gl.popMatrix()
	gl.pushMatrix()
	gl.translate(0, 0, -1)
	gl.rotate(180, 0, 1, 0)
	face(0.2, 1, 0.2)
	gl.popMatrix()
	gl.pushMatrix()
	gl.translate(1, 0, 0)
	gl.rotate(90, 0, 1, 0)
	face(0.2, 0.4, 1)
	gl.popMatrix()
	gl.pushMatrix()
	gl.translate(-1, 0, 0)
	gl.rotate(-90, 0, 1, 0)
	face(1, 0.85, 0.2)
	gl.popMatrix()
	gl.popMatrix()
end

function rs.load()
	gl.enable(gl.DEPTH_TEST)
	gl.depthFunc(gl.LESS)
	gl.enable(gl.CULL_FACE)
	gl.cullFace(gl.BACK)
	gl.frontFace(gl.CCW)
end

function rs.update(dt)
	time = time + dt
	if rs.keyboard.isDown("escape") then
		rs.window.close()
	end
end

function rs.draw()
	gl.clearColor(0.015, 0.018, 0.028, 1)
	gl.clear(gl.COLOR_BUFFER_BIT + gl.DEPTH_BUFFER_BIT)
	setup_camera()
	cube()
end
