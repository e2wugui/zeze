-- usage: luajit proto2xml.lua -solutionName 方案名 -moduleId 模块ID 输入文件名.proto 输出文件名.xml

local args = {}
local argId = 1
while true do
	local a = arg[argId]
	if a and a:sub(1, 1) == "-" then
		args[a:sub(2, -1)] = arg[argId + 1]
		argId = argId + 2
	else
		break
	end
end

io.write(arg[argId], " => ", arg[argId + 1], " ... ")
local path = arg[argId]:match "^(.*[/\\])[^/\\]*$" or ""

local imported = {}
local nameMap = {}
local function import(filename)
	if imported[filename] then return end
	imported[filename] = true
	-- print("import " .. filename)

	local package = ""
	local class = ""
	for line in io.lines(filename) do
		line = line:gsub("^%s+", ""):gsub("%s+$", "")
		local first = line:match "^%S+"
		if first == "import" then
			local name = line:match '^import%s*"(.-)"'
			if name then
				import(path .. name)
			end
		elseif first == "option" then
			local k, v = line:match '^option%s+([%w_]+)%s*=%s*"(.-)"'
			if k == "java_package" then
				package = args.solutionName or ""
				if package ~= "" then
					package = package .. "."
				end
				package = package .. v
			elseif k == "java_outer_classname" then
				class = v
			end
		elseif first == "message" then
			if class ~= "" then
				package = package .. "." .. class
				class = ""
			end
			local name = line:match '^message%s+([%w_]+)'
			if nameMap[name] then
				error("ERROR: duplicated message name: " .. name .. " in '" .. nameMap[name] .. "' and '" .. package .. "'")
			end
			nameMap[name] = package
		end
	end
end
import(arg[argId])

local f = io.open(arg[argId + 1], "wb")
local lineId = 0
local indent = ""
local package = args.solutionName or ""
local class = ""
local lastProto = ""
local lastBean = ""
local lastMsgId = nil

local typeMap = {
	bool = "bool",
	int32 = "int",
	sint32 = "int",
	uint32 = "int",
	fixed32 = "int",
	sfixed32 = "int",
	int64 = "long",
	sint64 = "long",
	uint64 = "long",
	fixed64 = "long",
	sfixed64 = "long",
	float = "float",
	double = "double",
	string = "string",
	bytes = "binary",
}

local unsigneds = {
	uint32 = true,
	uint64 = true,
}

local function convertType(type, repeated)
	local t = typeMap[type]
	if not t then
		local p = nameMap[type]
		if not p then
			return
		end
		if p == package then
			t = type
		else
			t = p .. "." .. type
		end
	end
	return (repeated and ("list[" .. t .. "]") or t), unsigneds[type]
end

local function escape(txt)
	return (txt:gsub("<", "＜"):gsub(">", "＞"))
end

for line in io.lines(arg[argId]) do
	lineId = lineId + 1
	line = line:gsub("^%s+", ""):gsub("%s+$", "")
	local first = line:match "^%S+"
	if first then
		if first == "import" then
			local name = line:match '^import "(.-)"'
			if name then
				f:write(string.format('%s<!--import name="%s"-->\n', indent, name))
			else
				error("ERROR(" .. lineId .. "): unknown import: " .. line)
			end
		elseif first == "option" then
			local k, v = line:match '^option%s+([%w_]+)%s*=%s*"(.-)"'
			if k == "java_package" then
				package = args.solutionName or ""
				if package ~= "" then
					package = package .. "."
				end
				package = package .. v
				if class ~= "" then
					package = package .. "." .. class
				end
				-- for name in v:gmatch "[^.]+" do
				-- 	if indent == "" then
				-- 	f:write(string.format('<solution name="%s" equals="true" ModuleIdAllowRanges="0-0">\n', v))
				-- 	indent = indent .. "\t"
				-- end
			elseif k == "java_outer_classname" then
				class = v
				if package ~= "" then
					package = package .. "." .. class
				end
				f:write(string.format('%s<module name="%s" id="%s">\n', indent, v, args.moduleId or "0"))
				indent = indent .. "\t"
			elseif k then
				error("ERROR(" .. lineId .. "): unknown option: " .. line)
			else
				lastMsgId = line:match "^option%s+%(msgid%)%s*=%s*(%d+)"
				if not lastMsgId then
					error("ERROR(" .. lineId .. "): unknown option: " .. line)
				end
			end
		elseif first == "message" then
			local name = line:match '^message%s+([%w_]+)'
			if not name then
				error("ERROR(" .. lineId .. "): unknown message: " .. line)
			end
			lastProto = name
			if name:find "^SC" or name:find "^CS" then
				lastBean = "B" .. lastProto:sub(3, -1)
			else
				lastBean = lastProto
			end
			lastMsgId = nil
			f:write(string.format('%s<bean name="%s">\n', indent, lastBean))
			indent = indent .. "\t"
		elseif first == "required" or first == "optional" or first == "repeated" then
			local type, name, id, comment = line:match "^%S+%s+([%w_]+)%s+([%w_]+)%s*=%s*(%d+)[%s;/]+(.*)$"
			if not type then
				error("ERROR(" .. lineId .. "): unknown field: " .. line)
			end
			local type, unsigned = convertType(type, first == "repeated")
			if not type then
				error("ERROR(" .. lineId .. "): unknown type: " .. line)
			end
			if unsigned then
				comment = "[unsigned] " .. comment
			end
			if comment ~= "" then
				comment = " " .. escape(comment)
			end
			f:write(string.format('%s<variable id="%s" name="%s" type="%s"/>%s\n', indent, id, name, type, comment))
		elseif first == "}" then
			indent = indent:sub(1, -2)
			f:write(indent, "</bean>\n")
			if lastMsgId then
				local handle
				if lastProto:find "^SC" then
					handle = "client"
				elseif lastProto:find "^CS" then
					handle = "server"
				else
					handle = ""
				end
				f:write(string.format('%s<protocol id="%s" name="%s" argument="%s" handle="%s"/>\n', indent, lastMsgId, lastProto, lastBean, handle))
				lastMsgId = nil
			end
		elseif first:find "^//" then
			f:write(indent, escape(line:sub(3, -1):gsub("^%s+", "")), "\n")
		else
			error("ERROR(" .. lineId .. "): unknown line: " .. line)
		end
	else
		f:write "\n"
	end
end

if #indent > 0 then
	while #indent > 1 do
		indent = indent:sub(1, -2)
		f:write(indent, "</module>\n")
	end
	f:write "</module>\n"
--	f:write "</solution>\n"
end

f:close()
print "OK!"
