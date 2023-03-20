-- UTF-8 without BOM
local error = error
local table = table
local ipairs = ipairs
local print = print
local format = string.format
local io = io
local open = io.open

local vcxproj = "CxxTest.vcxproj"
local basepath = "..\\"

local includes =
{
	"^cxx\\",
	"^CxxTest\\",
}

local excludes =
{
	"\\Debug\\",
	"\\Release\\",
}

local compileExcludes =
{
	"\\ToLua.cpp$",
}

local filetypes =
{
	c = "ClCompile",
	cc = "ClCompile",
	cpp = "ClCompile",
	cxx = "ClCompile",

	h = "ClInclude",
	hh = "ClInclude",
	hpp = "ClInclude",
	hxx = "ClInclude",
	inl = "ClInclude",
	inc = "ClInclude",
	hrp = "ClInclude",
	[""] = "ClInclude",
}

local logDebug = print
-- local logDebug = function() end

local function runcmd(cmd)
	local f = io.popen(cmd)
	local s = f:read "*a"
	f:close()
	return s
end

local function inIncludes(filename)
	if #includes == 0 then return true end
	for i = 1, #includes do
		if filename:find(includes[i]) then return true end
	end
end

local function inExcludes(filename)
	for i = 1, #excludes do
		if filename:find(excludes[i]) then return true end
	end
end

local function inCompileExcludes(filename)
	for i = 1, #compileExcludes do
		if filename:find(compileExcludes[i]) then return true end
	end
end

local function checksave(fn, s, d)
	if s == d then return end
	print(" * " .. fn)
	local f = open(fn, "wb")
	if not f then error("ERROR: can not create file: " .. fn) end
	f:write(d)
	f:close()
end

local baseFullPath = runcmd("pushd " .. basepath .. " & cd & popd"):gsub("%s+", "")
local filenames = runcmd("dir /s/b/a-d " .. basepath .. "*.*")
local baseFullPathBegin = #baseFullPath + 2
logDebug("baseFullPath: '" .. baseFullPath .. "'")

local srcfiles = {}
local incfiles = {}
for filename in filenames:gmatch("%C+") do
	if filename:find(baseFullPath, 1, true) == 1 then
		filename = filename:sub(baseFullPathBegin)
		if inIncludes(filename) and not inExcludes(filename) then
			local ext = filename:match("%.([^\\]+)$") or ""
			local filetype = filetypes[ext]
			if filetype == "ClCompile" then
				srcfiles[#srcfiles + 1] = filename
			elseif filetype == "ClInclude" then
				incfiles[#incfiles + 1] = filename
			end
		end
	end
end
table.sort(srcfiles, function(a, b) return a:lower() < b:lower() end)

local usednames = {}
local t = { "\">\r\n" }
if #srcfiles > 0 then
	t[#t + 1] = "  <ItemGroup>\r\n"
	for _, filename in ipairs(srcfiles) do
		if inCompileExcludes(filename) then
			t[#t + 1] = "    <ClCompile Include=\"" .. basepath .. filename .. "\">\r\n"
			t[#t + 1] = "      <ExcludedFromBuild Condition=\"'$(Configuration)|$(Platform)'=='Debug|x64'\">true</ExcludedFromBuild>\r\n"
			t[#t + 1] = "      <ExcludedFromBuild Condition=\"'$(Configuration)|$(Platform)'=='Release|x64'\">true</ExcludedFromBuild>\r\n"
			t[#t + 1] = "      <ExcludedFromBuild Condition=\"'$(Configuration)|$(Platform)'=='Debug|Win32'\">true</ExcludedFromBuild>\r\n"
			t[#t + 1] = "      <ExcludedFromBuild Condition=\"'$(Configuration)|$(Platform)'=='Release|Win32'\">true</ExcludedFromBuild>\r\n"
			t[#t + 1] = "    </ClCompile>\r\n"
		else
			local name = filename:match("([^\\]+)%.[^\\.]+$")
			local objid = usednames[name]
			if objid then
				local newname
				repeat
					newname = name .. "_" .. objid
					objid = objid + 1
				until not usednames[newname]
				usednames[name] = objid
				t[#t + 1] = "    <ClCompile Include=\"" .. basepath .. filename .. "\">\r\n"
				t[#t + 1] = "      <ObjectFileName Condition=\"'$(Configuration)|$(Platform)'=='Debug|x64'\">$(IntDir)" .. newname .. ".obj</ObjectFileName>\r\n"
				t[#t + 1] = "      <ObjectFileName Condition=\"'$(Configuration)|$(Platform)'=='Release|x64'\">$(IntDir)" .. newname .. ".obj</ObjectFileName>\r\n"
				t[#t + 1] = "      <ObjectFileName Condition=\"'$(Configuration)|$(Platform)'=='Debug|Win32'\">$(IntDir)" .. newname .. ".obj</ObjectFileName>\r\n"
				t[#t + 1] = "      <ObjectFileName Condition=\"'$(Configuration)|$(Platform)'=='Release|Win32'\">$(IntDir)" .. newname .. ".obj</ObjectFileName>\r\n"
				t[#t + 1] = "    </ClCompile>\r\n"
			else
				usednames[name] = 1
				t[#t + 1] = "    <ClCompile Include=\"" .. basepath .. filename .. "\" />\r\n"
			end
		end
	end
	t[#t + 1] = "  </ItemGroup>\r\n"
end
if #incfiles > 0 then
	t[#t + 1] = "  <ItemGroup>\r\n"
	for _, filename in ipairs(incfiles) do
		t[#t + 1] = "    <ClInclude Include=\"" .. basepath .. filename .. "\" />\r\n"
	end
	t[#t + 1] = "  </ItemGroup>\r\n"
end

local f = open(vcxproj, "rb")
local s = f:read "*a"
f:close()

local d = s:gsub("[ \t]*<ItemGroup>.-</ItemGroup>[ \t]*%c*", "")
		   :gsub("\">\r\n", table.concat(t), 1)
checksave(vcxproj, s, d)

-----------------------------------------------------------------------------

local function getPath(filename)
	return filename:match("(.+)\\[^\\]*$")
end

local pathset = {}
local pathes = {}
for _, filename in ipairs(srcfiles) do
	local path = getPath(filename)
	while path do
		if pathset[path] then break end
		pathset[path] = true
		path = getPath(path)
	end
end
for _, filename in ipairs(incfiles) do
	local path = getPath(filename)
	while path do
		if pathset[path] then break end
		pathset[path] = true
		path = getPath(path)
	end
end
for path in pairs(pathset) do
	pathes[#pathes + 1] = path
end
table.sort(pathes, function(a, b) return a:lower() < b:lower() end)

t = { "\">\r\n" }
if #pathes > 0 then
	t[#t + 1] = "  <ItemGroup>\r\n"
	for i, path in ipairs(pathes) do
		t[#t + 1] = format("    <Filter Include=\"" .. path .. "\">\r\n      <UniqueIdentifier>{00000000-0000-0000-0000-%012x}</UniqueIdentifier>\r\n    </Filter>\r\n", i)
	end
	t[#t + 1] = "  </ItemGroup>\r\n"
end
if #srcfiles > 0 then
	t[#t + 1] = "  <ItemGroup>\r\n"
	for _, filename in ipairs(srcfiles) do
		t[#t + 1] = format("    <ClCompile Include=\"" .. basepath .. filename .. "\">\r\n      <Filter>%s</Filter>\r\n    </ClCompile>\r\n", getPath(filename))
	end
	t[#t + 1] = "  </ItemGroup>\r\n"
end
if #incfiles > 0 then
	t[#t + 1] = "  <ItemGroup>\r\n"
	for _, filename in ipairs(incfiles) do
		t[#t + 1] = format("    <ClInclude Include=\"" .. basepath .. filename .. "\">\r\n      <Filter>%s</Filter>\r\n    </ClInclude>\r\n", getPath(filename))
	end
	t[#t + 1] = "  </ItemGroup>\r\n"
end

vcxproj = vcxproj .. ".filters"

local f = open(vcxproj, "rb")
local s = f:read "*a"
f:close()

local d = s:gsub("[ \t]*<ItemGroup>.-</ItemGroup>[ \t]*%c*", "")
		   :gsub("\">\r\n", table.concat(t), 1)
checksave(vcxproj, s, d)

print "done!"

--[[
@echo off
setlocal
pushd %~dp0

rem 参数说明
rem 1. bat中的""里的%是转义符,需要写%%表示%
rem 2. lua会把参数""里结尾的反斜杠去掉,此时需要使用双反斜杠

luajit sync_vcxproj.lua sandbox_server.vcxproj .. ^
	"%%u$" ^
	"file$" ^
	"Log$" ^
	"^%%.git\\" ^
	"^share\perf\i386\\" ^
	"^share\perf\x86_64\\" ^
	"^share\storage\\" ^
	"^gameserver\gs\callstack%%.cpp$" ^
	"^gameserver\gs\cmds\back_home_effect%%.cpp$" ^
	"^gameserver\gs\item\impl\item_body_skill_impl%%." ^
	"^gameserver\gs\npccomponent\npcservice\npc_service_impl_pet_list%%." ^
	"^gdbclient\dbinterface%%." ^
	"^logclient\logservice" ^
	"^luahelper\\" ^
	"^tools\\" ^
	"^thirdparty\.*\benchmark\\" ^
	"^thirdparty\.*\benchmarks\\" ^
	"^thirdparty\.*\examples\\" ^
	"^thirdparty\.*\test\\" ^
	"^thirdparty\.*\tests\\" ^
	"^thirdparty\.*\tutorials\\" ^
	"^thirdparty\behaviac\.*\defaultsocketwrapper_vcc.cpp$" ^
	"^thirdparty\behaviac\build\\" ^
	"^thirdparty\concurrentqueue\build\\" ^
	"^thirdparty\jemalloc\\" ^
	"^thirdparty\mysql%%-connector\extra\\" ^
	"^vs2017\pthreads%%-master\\"

pause
--]]
