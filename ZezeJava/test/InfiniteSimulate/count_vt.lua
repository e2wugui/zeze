-- jcmd <pid> Thread.dump_to_file dump.txt

local ignored = {
	["Zeze.Util.TaskCompletionSource.get(TaskCompletionSource.java:135)"] = true,
}

local tn = 0
local t = {}
local inVT = false
for line in io.lines(arg[1]) do
	if line:find '^#%d+ ".-" virtual$' then
		tn = tn + 1
		inVT = true
	elseif line:find '^ ' then
		if inVT and line:find '^ +Zeze' then
			local k = line:gsub('^ +', '')
			if not ignored[k] then
				t[k] = (t[k] or 0) + 1
				inVT = false
			end
		end
	else
		inVT = false
	end
end

local s = {}
local vn = 0
for k, v in pairs(t) do
	s[#s + 1] = k
	vn = vn + v
end
table.sort(s, function(a, b) return t[a] > t[b] end)
for _, k in ipairs(s) do
	print(string.format("%6.2f%% %6d %s", t[k] * 100 / tn, t[k], k))
end
print(string.format("%6.2f%% %6d not in zeze", (tn - vn) * 100 / tn, tn - vn))
print(string.format(" TOTAL: %6d virtual threads", tn))
