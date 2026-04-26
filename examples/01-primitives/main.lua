local gl = rs.gl
local time = 0

local function setup_camera()
	local width, height = rs.window.getDimensions()
	gl.viewport(0, 0, width, height)
	gl.matrixMode(gl.PROJECTION)
	gl.loadIdentity()
	gl.ortho(0, 1100, 760, 0, -1, 1)
	gl.matrixMode(gl.MODELVIEW)
	gl.loadIdentity()
end

local function vertex(x, y)
	gl.vertex2(x, y)
end

local function draw_points()
	gl.pointSize(8)
	gl.begin(gl.POINTS)
	for i = 0, 8 do
		gl.color4(1, i / 8, 0.2, 1)
		vertex(80 + i * 28, 90 + math.sin(time * 3 + i) * 24)
	end
	gl.end_()
	gl.pointSize(1)
end

local function draw_lines()
	gl.lineWidth(4)
	gl.color3(0.2, 0.8, 1)
	gl.begin(gl.LINES)
	for i = 0, 5 do
		vertex(360 + i * 34, 58)
		vertex(386 + i * 34, 125)
	end
	gl.end_()
	gl.lineWidth(1)
end

local function draw_line_strip()
	gl.lineWidth(3)
	gl.begin(gl.LINE_STRIP)
	for i = 0, 14 do
		gl.color3(i / 14, 1 - i / 20, 1)
		vertex(650 + i * 24, 94 + math.sin(time * 4 + i * 0.7) * 36)
	end
	gl.end_()
	gl.lineWidth(1)
end

local function draw_line_loop()
	gl.lineWidth(3)
	gl.color3(1, 0.75, 0.15)
	gl.begin(gl.LINE_LOOP)
	for i = 0, 5 do
		local angle = time + i / 6 * math.pi * 2
		vertex(160 + math.cos(angle) * 70, 265 + math.sin(angle) * 70)
	end
	gl.end_()
	gl.lineWidth(1)
end

local function draw_triangles()
	gl.begin(gl.TRIANGLES)
	gl.color3(1, 0.2, 0.2)
	vertex(330, 330)
	gl.color3(0.2, 1, 0.2)
	vertex(460, 330)
	gl.color3(0.2, 0.4, 1)
	vertex(395, 210)
	gl.end_()
end

local function draw_triangle_strip()
	gl.begin(gl.TRIANGLE_STRIP)
	for i = 0, 7 do
		gl.color3(0.2 + i * 0.1, 0.4, 1 - i * 0.08)
		vertex(600 + i * 48, i % 2 == 0 and 320 or 220)
	end
	gl.end_()
end

local function draw_triangle_fan()
	gl.begin(gl.TRIANGLE_FAN)
	gl.color3(1, 1, 1)
	vertex(175, 530)
	for i = 0, 12 do
		local angle = time * 0.7 + i / 12 * math.pi * 2
		gl.color3(0.4 + math.cos(angle) * 0.4, 0.5, 0.8 + math.sin(angle) * 0.2)
		vertex(175 + math.cos(angle) * 90, 530 + math.sin(angle) * 90)
	end
	gl.end_()
end

local function draw_quads()
	gl.begin(gl.QUADS)
	gl.color3(0.1, 0.9, 0.8)
	vertex(340, 460)
	gl.color3(0.9, 0.9, 0.2)
	vertex(500, 460)
	gl.color3(0.9, 0.2, 0.5)
	vertex(500, 620)
	gl.color3(0.2, 0.4, 1)
	vertex(340, 620)
	gl.end_()
end

local function draw_quad_strip()
	gl.begin(gl.QUAD_STRIP)
	for i = 0, 6 do
		local x = 620 + i * 45
		gl.color3(i / 6, 0.3, 1 - i / 8)
		vertex(x, 475 + math.sin(time + i) * 18)
		vertex(x, 610 + math.cos(time + i) * 18)
	end
	gl.end_()
end

local function draw_polygon()
	gl.begin(gl.POLYGON)
	for i = 0, 8 do
		local angle = -time + i / 9 * math.pi * 2
		gl.color3(1, 0.35 + i / 18, 0.15)
		vertex(965 + math.cos(angle) * 75, 530 + math.sin(angle) * 75)
	end
	gl.end_()
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
	gl.clearColor(0.035, 0.04, 0.055, 1)
	gl.clear(gl.COLOR_BUFFER_BIT)
	setup_camera()
	draw_points()
	draw_lines()
	draw_line_strip()
	draw_line_loop()
	draw_triangles()
	draw_triangle_strip()
	draw_triangle_fan()
	draw_quads()
	draw_quad_strip()
	draw_polygon()
end
