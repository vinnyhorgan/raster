local gl = rs.gl

local time = 0
local twinkle = {}

local function setup_camera()
	local width, height = rs.window.getDimensions()
	gl.viewport(0, 0, width, height)
	gl.matrixMode(gl.PROJECTION)
	gl.loadIdentity()
	gl.ortho(0, width, height, 0, -1, 1)
	gl.matrixMode(gl.MODELVIEW)
	gl.loadIdentity()
	return width, height
end

local function quad(x, y, width, height, r, g, b, a)
	gl.color4(r, g, b, a)
	gl.begin(gl.QUADS)
	gl.vertex2(x, y)
	gl.vertex2(x + width, y)
	gl.vertex2(x + width, y + height)
	gl.vertex2(x, y + height)
	gl.end_()
end

local function heart(cx, cy, scale, r, g, b)
	gl.begin(gl.TRIANGLE_FAN)
	gl.color4(r, g, b, 1)
	gl.vertex2(cx, cy + 8 * scale)
	for i = 0, 48 do
		local t = i / 48 * math.pi * 2
		local x = 16 * math.sin(t) ^ 3
		local y = -(13 * math.cos(t) - 5 * math.cos(2 * t) - 2 * math.cos(3 * t) - math.cos(4 * t))
		gl.vertex2(cx + x * scale, cy + y * scale)
	end
	gl.end_()
end

local function star(cx, cy, radius, phase)
	gl.begin(gl.TRIANGLE_FAN)
	gl.color4(1, 0.92, 0.45, 1)
	gl.vertex2(cx, cy)
	for i = 0, 10 do
		local angle = phase + i / 10 * math.pi * 2
		local length = i % 2 == 0 and radius or radius * 0.42
		gl.color4(1, 0.78 + math.sin(time * 4 + i) * 0.12, 0.32, 1)
		gl.vertex2(cx + math.cos(angle) * length, cy + math.sin(angle) * length)
	end
	gl.end_()
end

local function mascot(cx, cy)
	gl.pushMatrix()
	gl.translate(cx, cy + math.sin(time * 3) * 8, 0)
	gl.rotate(math.sin(time * 2) * 5, 0, 0, 1)
	quad(-84, -52, 168, 112, 0.93, 0.25, 0.55, 1)
	quad(-70, -38, 140, 84, 1, 0.68, 0.33, 1)
	quad(-52, -88, 104, 38, 0.24, 0.78, 1, 1)
	quad(-58, -82, 18, 18, 1, 0.9, 0.45, 1)
	quad(40, -82, 18, 18, 1, 0.9, 0.45, 1)
	quad(-42, -18, 22, 22, 0.05, 0.06, 0.09, 1)
	quad(20, -18, 22, 22, 0.05, 0.06, 0.09, 1)
	quad(-34, -10, 8, 8, 1, 1, 1, 1)
	quad(28, -10, 8, 8, 1, 1, 1, 1)
	quad(-22, 26, 44, 10, 0.05, 0.06, 0.09, 1)
	heart(0, 62, 1.7, 1, 0.18, 0.42)
	gl.popMatrix()
end

function rs.load()
	for i = 1, 34 do
		twinkle[i] = {
			x = 40 + (i * 73) % 880,
			y = 36 + (i * 47) % 530,
			phase = i * 0.83,
		}
	end
end

function rs.update(dt)
	time = time + dt
	if rs.keyboard.isDown("escape") then
		rs.window.close()
	end
end

function rs.draw()
	local width, height = setup_camera()
	gl.clearColor(0.055, 0.045, 0.1, 1)
	gl.clear(gl.COLOR_BUFFER_BIT)
	gl.enable(gl.BLEND)
	gl.blendFunc(gl.SRC_ALPHA, gl.ONE_MINUS_SRC_ALPHA)
	quad(0, 0, width, height, 0.045, 0.04, 0.095, 1)
	quad(0, height * 0.58, width, height * 0.42, 0.12, 0.055, 0.13, 1)
	for _, item in ipairs(twinkle) do
		local pulse = 0.45 + math.sin(time * 2.5 + item.phase) * 0.35
		star(item.x, item.y, 5 + pulse * 8, time * 0.8 + item.phase)
	end
	for i = 0, 9 do
		local x = i / 9 * width
		local y = height * 0.78 + math.sin(time * 2 + i) * 18
		quad(x - 26, y, 52, 12, 0.32, 0.9, 1, 0.26)
	end
	mascot(width * 0.5, height * 0.47)
	rs.debug.print("hello from Raster", width * 0.5 - 176, 44, 38, 1, 0.86, 0.34, 1, true)
	rs.debug.print(
		"a tiny LuaJIT fantasy runtime with an rs.gl playground",
		width * 0.5 - 244,
		92,
		17,
		0.83,
		0.89,
		1,
		1
	)
	rs.debug.print(
		"drop a project folder on the executable, or press Esc to quit",
		width * 0.5 - 242,
		height - 58,
		16,
		0.7,
		0.76,
		0.9,
		1
	)
end
