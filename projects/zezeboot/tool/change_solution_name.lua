local modify_dirs = {
	{ "..\\protocol",  ".xml" },
	{ "..\\client",    ".java" },
	{ "..\\link",      ".java" },
	{ "..\\server",    ".java" },
	{ "..\\client_cs", ".cs" },
	{ "..\\client_cs", ".csproj" },
	{ "..",            ".bat" },
}

local move_dirs = {
	[[..\client\src\main\java\zezeboot]],
	[[..\client_cs\zezeboot]],
	[[..\link\src\main\java\zezeboot]],
	[[..\server\src\main\java\zezeboot]],

	[[..\client\src\main\java\ZezeBootClient.java]],
	[[..\link\src\main\java\ZezeBootLink.java]],
	[[..\server\src\main\java\ZezeBootServer.java]],
}

local sn = arg[1]
if not sn then
	io.write "new solution english name: "
	sn = io.read "*l":gsub("^%s+", ""):gsub("%s+$", "")
end
if not sn:find "^%a[%w]*$" then
	error("ERROR: invalid solution name: '" .. sn .. "'")
end
sn = sn:sub(1, 1):upper() .. sn:sub(2, -1)
local sn1 = sn:lower()
local sn2 = sn:upper()
io.write("INFO: new solution name: '", sn, "', '", sn1, "', '", sn2, "'\n")

local function convert(s)
	return s
		:gsub("ZezeBoot", sn)
		:gsub("zezeboot", sn1)
		:gsub("ZEZEBOOT", sn2)
end

local modify_n = 0
local function modify(fn, c)
	local f = io.open(fn, "rb")
	local s = f:read "*a"
	f:close()

	local d = c(s)
	if d ~= s then
		io.write("modify ", fn, " ... ")
		f = io.open(fn, "wb")
		f:write(d)
		f:close()
		io.write("OK\n")
		modify_n = modify_n + 1
		return true
	end
end

for _, dir in ipairs(modify_dirs) do
	local f = io.popen('dir /s/b/a "' .. dir[1] .. '\\*' .. dir[2] .. '" 2>nul')
	local s = f:read "*a"
	f:close()

	for line in s:gmatch "%C+" do
		modify(line, convert)
	end
end

modify("..\\.idea\\vcs.xml", function(s) return s:gsub("/%.%./%.%.", "") end)

local move_n = 0
for _, dir in ipairs(move_dirs) do
	io.write("move ", dir, " ... ")
	local f = io.popen('move "' .. dir .. '" "' .. convert(dir) .. '"')
	local s = f:read "*a"
	f:close()

	local msg = s:match "%C+"
	if msg then
		io.write("OK\n")
		move_n = move_n + 1
	end
end

io.write("INFO: done! (modified ", modify_n, " files, moved ", move_n, " dirs)\n")
