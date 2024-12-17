-- usage: luajit proto2xml.lua 输入文件名.proto 输出文件名.xml

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

local function convertType(type, repeated)
	type = typeMap[type] or type
	return repeated and ("list[" .. type .. "]") or type
end

local function escape(txt)
	return (txt:gsub("<", "＜"):gsub(">", "＞"))
end

io.write(arg[1], " => ", arg[2], " ... ")
local f = io.open(arg[2], "wb")

local lineId = 0
local indent = ""
local lastProto = ""
local lastBean = ""
local lastMsgId = nil
for line in io.lines(arg[1]) do
	lineId = lineId + 1
	line = line:gsub("^%s+", ""):gsub("%s+$", "")
	local first = line:match "^%S+"
	if first then
		if first == "import" then
			-- ignore
		elseif first == "option" then
			local k, v = line:match '^option%s+([%w_]+)%s*=%s*"(.-)"'
			if k == "java_package" then
				v = v:match "^[%w_]+"
				f:write(string.format('%s<solution name="%s" equals="true" ModuleIdAllowRanges="0-0">\n', indent, v))
				indent = indent .. "\t"
			elseif k == "java_outer_classname" then
				f:write(string.format('%s<module name="%s" id="0">\n', indent, v))
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
			type = convertType(type, first == "repeated")
			if not type then
				error("ERROR(" .. lineId .. "): unknown type: " .. line)
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
	if #indent > 1 then
		indent = indent:sub(1, -2)
		f:write(indent, "</module>\n")
	end
	indent = indent:sub(1, -2)
	f:write(indent, "</solution>\n")
end

f:close()
print "OK!"
