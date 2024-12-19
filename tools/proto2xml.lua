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
		line = line:gsub("%s+$", "")
		local first = line:match "^%S+"
		if first then
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
					error("ERROR: duplicated message/enum name: " .. name .. " in '" .. nameMap[name] .. "' and '" .. package .. "'")
				end
				nameMap[name] = package
			elseif first == "enum" then
				if class ~= "" then
					package = package .. "." .. class
					class = ""
				end
				local name = line:match '^enum%s+([%w_]+)'
				if nameMap[name] then
					error("ERROR: duplicated message/enum name: " .. name .. " in '" .. nameMap[name] .. "' and '" .. package .. "'")
				end
				nameMap[name] = true
			end
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
local inEnum = nil
local msgStack = {}

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

local typeComment = {
	uint32 = "[uint32]",
	uint64 = "[uint64]",
	fixed32 = "[fixed32]",
}

local function convertType(type, repeated)
	local t = typeMap[type]
	local tc = nil
	if t then
		tc = typeComment[type]
	else
		local p = nameMap[type]
		if not p then
			if #msgStack > 0 then
				p = msgStack[#msgStack].inners[type]
				if p == "enum" then
					p = true
				elseif p == "message" then
					p = package
				end
			end
			if not p then
				return
			end
		end
		if p == true then -- enum
			t = "int"
			tc = "[" .. type .. "]"
		elseif p == package then
			t = type
		else
			t = p .. "." .. type
		end
	end
	return (repeated and ("list[" .. t .. "]") or t), tc
end

local inComment = false
local function splitComment(line)
	if inComment then
		local p3 = line:find("*/", 1, true)
		if p3 then
			inComment = false
			local t, c = splitComment(line:sub(p3 + 2, -1))
			return t, line:sub(1, p3 - 1) .. " " .. c
		end
		return "", line
	end
	local p1 = line:find("//", 1, true)
	local p2 = line:find("/*", 1, true)
	if p1 and p2 then
		if p1 < p2 then
			p2 = nil
		else
			p1 = nil
		end
	end
	if p1 then
		return line:sub(1, p1 - 1), line:sub(p1 + 2, -1)
	end
	if p2 then
		local p3 = line:find("*/", p2 + 2, true)
		if p3 then
			local t, c = splitComment(line:sub(p3 + 2, -1))
			return line:sub(1, p2 - 1) .. " " .. t, line:sub(p2 + 2, p3 - 1) .. " " .. c
		end
		inComment = true
		return line:sub(1, p2 - 1), line:sub(p2 + 2, -1)
	end
	return line, ""
end

local function escape(txt)
	return (txt:gsub("<", "＜"):gsub(">", "＞"))
end

f:write '<?xml version="1.0" encoding="utf-8"?>\n\n'
for srcLine in io.lines(arg[argId]) do
	lineId = lineId + 1
	local line, comment = splitComment(srcLine)
	line = line:gsub("^%s+", ""):gsub("%s+$", "")
	comment = comment:gsub("^%s+", ""):gsub("%s+$", "")
	local first = line:match "^[%w_}]+"
	if first == "import" then
		local name = line:match '^import "(.-)"'
		if name then
			f:write(string.format('%s<!--import name="%s"/-->\n', indent, name))
		else
			error("ERROR(" .. lineId .. "): unknown import: " .. srcLine)
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
			f:write(string.format('%s<!--java package="%s"/-->\n', indent, v))
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
			error("ERROR(" .. lineId .. "): unknown option: " .. srcLine)
		else
			local msgId = line:match "^option%s*%(%s*msgid%s*%)%s*=%s*(%d+)"
			if msgId and #msgStack > 0 then
				msgStack[#msgStack].msgId = msgId
			else
				error("ERROR(" .. lineId .. "): unknown option: " .. srcLine)
			end
		end
	elseif first == "message" then
		local name = line:match '^message%s+([%w_]+)'
		if not name then
			error("ERROR(" .. lineId .. "): unknown message: " .. srcLine)
		end
		msgStack[#msgStack + 1] = {
			name = name,
			enums = {},
			fields = {},
			inners = {},
		}
	elseif first == "enum" then
		local name = line:match '^enum%s+([%w_]+)'
		if name then
			if #msgStack > 0 then
				msgStack[#msgStack].inners[name] = "enum"
			end
			f:write(string.format('%s<!--enum name="%s"-->\n', indent, name))
			inEnum = name
		else
			error("ERROR(" .. lineId .. "): unknown enum: " .. srcLine)
		end
	elseif first == "}" then
		if inEnum then
			f:write(string.format('%s<!--/enum-->\n', indent))
			inEnum = nil
		elseif #msgStack > 0 then
			local msg = msgStack[#msgStack]
			msgStack[#msgStack] = nil
			if #msgStack > 0 then
				msgStack[#msgStack].inners[msg.name] = "message"
			end
			local beanName = msg.msgId and ("B" .. msg.name) or msg.name
			f:write(string.format('%s<bean name="%s">\n', indent, beanName))
			indent = indent .. "\t"
			for _, enum in ipairs(msg.enums) do
				f:write(string.format('%s<enum name="%s" value="%s"/>%s\n', indent, enum.k, enum.v, enum.comment))
			end
			for _, field in ipairs(msg.fields) do
				f:write(string.format('%s<variable id="%s" name="%s" type="%s"/>%s\n', indent, field.id, field.name, field.type, field.comment))
			end
			indent = indent:sub(1, -2)
			f:write(indent, "</bean>\n")
			if msg.msgId then
				local handle
				if msg.name:find "^SC" then
					handle = args.client or "client"
				elseif msg.name:find "^CS" then
					handle = "server"
				else
					handle = ""
				end
				f:write(string.format('%s<protocol id="%s" name="%s" argument="%s" handle="%s"/>\n', indent, msg.msgId, msg.name, beanName, handle))
			end
		else
			error("ERROR(" .. lineId .. "): unmatched brace: " .. srcLine)
		end
	elseif line == "" then
		f:write(indent, escape(comment), "\n")
	else
		local done = false
		if inEnum then
			local k, v = line:match "^(%S+)%s*=%s*([%w_]+)"
			if k then
				if comment ~= "" then
					comment = " [" .. inEnum .. "] " .. escape(comment)
				else
					comment = " [" .. inEnum .. "]"
				end
				if #msgStack > 0 then
					local enums = msgStack[#msgStack].enums
					enums[#enums + 1] = {
						k = k,
						v = v,
						comment = comment,
					}
				else
					f:write(string.format('%s<enum name="%s" value="%s"/>%s\n', indent, k, v, comment))
				end
				done = true
			end
		elseif #msgStack > 0 then
			local type, name, id
			if first == "required" or first == "optional" or first == "repeated" then
				type, name, id = line:match "^%S+%s+([%w_]+)%s+([%w_]+)%s*=%s*(%d+)"
			else
				type, name, id = line:match "^([%w_]+)%s+([%w_]+)%s*=%s*(%d+)"
			end
			if type then
				local type, typeComment = convertType(type, first == "repeated")
				if not type then
					error("ERROR(" .. lineId .. "): unknown type: " .. srcLine)
				end
				if typeComment then
					if comment == "" then
						comment = typeComment
					else
						comment = typeComment .. " " .. comment
					end
				end
				if comment ~= "" then
					comment = " " .. escape(comment)
				end
				local fields = msgStack[#msgStack].fields
				fields[#fields + 1] = {
					id = id,
					name = name,
					type = type,
					comment = comment,
				}
				done = true
			end
		end
		if not done then
			error("ERROR(" .. lineId .. "): unknown line: " .. srcLine)
		end
	end
end

if #indent > 0 then
	while #indent > 1 do
		indent = indent:sub(1, -2)
		f:write(indent, "</module>\n")
	end
	f:write "</module>\n" -- "</solution>\n"
end

f:close()
print "OK!"
