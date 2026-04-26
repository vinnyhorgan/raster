local gl = rs.gl
local time = 0
local dots = {}

local function camera()
	local width, height = rs.window.getDimensions()
	gl.viewport(0, 0, width, height)
	gl.matrixMode(gl.PROJECTION)
	gl.loadIdentity()
	gl.ortho(0, width, height, 0, -1, 1)
	gl.matrixMode(gl.MODELVIEW)
	gl.loadIdentity()
	return width, height
end

local function dot(x, y, radius, age)
	gl.begin(gl.TRIANGLE_FAN)
	gl.color4(1, 0.82 - age * 0.25, 0.25 + age * 0.4, 1 - age * 0.55)
	gl.vertex2(x, y)
	for i = 0, 24 do
		local angle = i / 24 * math.pi * 2
		gl.vertex2(x + math.cos(angle) * radius, y + math.sin(angle) * radius)
	end
	gl.end_()
end

function rs.update(dt)
	time = time + dt
	if rs.keyboard.isDown("escape") then
		rs.window.close()
	end
	if rs.mouse.isDown(1) then
		local x, y = rs.mouse.getPosition()
		dots[#dots + 1] = { x = x, y = y, born = time }
	end
	while #dots > 160 do
		table.remove(dots, 1)
	end
end

function rs.draw()
	camera()
	gl.clearColor(0.018, 0.02, 0.03, 1)
	gl.clear(gl.COLOR_BUFFER_BIT)
	gl.enable(gl.BLEND)
	gl.blendFunc(gl.SRC_ALPHA, gl.ONE_MINUS_SRC_ALPHA)
	for i, item in ipairs(dots) do
		local age = math.min(1, (time - item.born) / 2.8)
		dot(item.x, item.y, 22 + age * 36 + math.sin(i) * 4, age)
	end
	local x, y = rs.mouse.getPosition()
	gl.lineWidth(2)
	gl.color4(0.45, 0.85, 1, 0.7)
	gl.begin(gl.LINE_LOOP)
	for i = 0, 32 do
		local angle = i / 32 * math.pi * 2
		gl.vertex2(x + math.cos(angle) * 28, y + math.sin(angle) * 28)
	end
	gl.end_()
	gl.lineWidth(1)
	rs.debug.print("Hold left mouse to paint. Esc quits.", 24, 24, 18, 0.86, 0.92, 1, 1, true)
end
