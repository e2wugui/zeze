local old_version = "1.5.3"
local new_version = "1.5.4-SNAPSHOT"

local files = {
	{ 1, "build.gradle" },
--	{ 1, "pom.xml" },
	{ 1, "ZezeJava/pom.xml" },
	{ 2, "ZezeJavaTest/pom.xml" },
	{ 2, "ZezexJava/client/pom.xml" },
	{ 2, "ZezexJava/linkd/pom.xml" },
	{ 2, "ZezexJava/server/pom.xml" },
	{ 3, "test/Raft/raft.bat" },
	{ 7, "test/Raft/raft.more.bat" },
	{ 5, "test/Raft/raft.5x6node.bat" },
	{ 2, "test/GlobalRaft/service&global_raft3.bat" },
	{ 1, "test/InfiniteSimulate/simulate.bat" },
	{ 1, "test/InfiniteSimulate/simulate - Bug.bat" },
	{ 1, "test/InfiniteSimulate/simulate - ProcessDaemon.bat" },
	{ 2, "test/GlobalRaft/service&global_raft3.bat" },
	{ 1, "test/GlobalCacheManagerWithRaft/global_raft.bat" },
}

local old_version_pat = old_version:gsub("%.", "%%."):gsub("%-", "%%-")
local old_version_pom_pat = "<version>" .. old_version_pat .. "</version>"
local new_version_pom = "<version>" .. new_version .. "</version>"

local function find_count(str, pat)
	local i, j, n = 0, 0, 0
	while true do
		i, j = str:find(pat, j + 1)
		if not i then return n end
		n = n + 1
	end
end

local function check_version(filename, change_count)
	local f = io.open(filename, "rb")
	if not f then error("file not found: " .. filename) end
	local s = f:read "*a"
	f:close()

	local count = find_count(s, filename:find "pom.xml$" and old_version_pom_pat or old_version_pat)
	if count ~= change_count then
		error(filename .. ": need " .. change_count .. " '" .. old_version .. "', but found " .. count)
	end
end

local function change_version(filename, pat, replaced)
	local f = io.open(filename, "rb")
	local s = f:read "*a"
	f:close()

	io.write("modify " .. filename .. " ... ")
	local t = s:gsub(pat, replaced)
	if t ~= s then
		local f = io.open(filename, "wb")
		f:write(t)
		f:close()
	end
	print "OK"
end

for _, f in ipairs(files) do
	check_version(f[2], f[1])
end

print "--- CHECK DONE ---"

for _, f in ipairs(files) do
	if f[2]:find "pom.xml$" then
		change_version(f[2], old_version_pom_pat, new_version_pom)
	else
		change_version(f[2], old_version_pat, new_version)
	end
end

print "=== CHANGE DONE ==="
