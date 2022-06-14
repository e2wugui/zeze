-- 需要当前正在运行 simulate.bat 测试, 否则分析上次分析时保存的栈信息(stack_global.log)

print "============================================================ BEGIN"
local f = io.popen("jps", "rb")
local s = f:read "*a"
f:close()

local pid_global = s:match "(%d+)%s+GlobalCacheManager[Async]*Server[\r\n]"
-- local pid_glo = s:match "(%d+)%s+GlobalCacheManager[Async]*Server[\r\n]"
if pid_global then
	f = io.popen("jstack -l -e " .. pid_global, "rb")
	s = f:read "*a"
	f:close()

	f = io.open("stack_global.log", "wb")
	f:write(s)
	f:close()
else
	print "没有找到 GlobalCacheManager(Async)Server 进程"
	f = io.open("stack_global.log", "rb")
	if not f then return end
	print "从 stack_global.log 读取线程栈信息"
	s = f:read "*a"
	f:close()
	print "------------------------------------------------------------"
end
f = nil

local threads = {} -- threadName, lines, text, wait, owns
local threadName
for line in s:gmatch "[^\r\n]+" do
	line = line:gsub("%s+$", "")
	if line == "" then
		if threadName then
			threads[threadName].text = table.concat(threads[threadName].lines, "\n")
			threadName = nil
		end
	else
		local first = line:sub(1, 1)
		if first == " " or first == "\t" then
			if threadName then
				threads[threadName].lines[#threads[threadName].lines + 1] = line
				local wt = line:match "%- parking to wait for  <(.-)>"
				if wt then threads[threadName].wait = wt end
				local ow = line:match "%- <(.-)>"
				if ow then threads[threadName].owns[ow] = true end
			end
		elseif first == "\"" then
			if threadName then
				threads[threadName].text = table.concat(threads[threadName].lines, "\n")
			end
			threadName = line:match "\"(.-)\""
			threads[threadName] = { threadName = threadName, lines = { line }, owns={} }
		end
	end
end
if threadName then
	threads[threadName].text = table.concat(threads[threadName].lines, "\n")
end

local function findDeadLock(lock, firstThreadName)
	for threadName, thread in pairs(threads) do
		if thread.owns[lock] then
			if threadName == firstThreadName then
				print "发现死锁!!!"
				print("    at " .. threadName)
				return true
			end
			if thread.wait then
				local r = findDeadLock(thread.wait, firstThreadName)
				if r then
					print("    at " .. threadName)
					return r
				end
			end
			return false
		end
	end
end
local foundDeadLock
for threadName, thread in pairs(threads) do
	if thread.wait then
		if findDeadLock(thread.wait, threadName) then
			print("    at " .. threadName)
			foundDeadLock = true
		end
	end
end
if not foundDeadLock then
	print "没有发现死锁"
end

local knowns = {
	{ "在Load等待Record1锁",                           ".EnterFairLock(", ".Load(" },
	{ "在Load等待Acquire(Share)回复",                  ".Acquire(", ".Load(" },
	{ "在_lock_and_check_等待Lockey写锁",              "Lockey.EnterWriteLock(", "._lock_and_check_(" },
	{ "在_check_等待Record1锁",                        ".EnterFairLock(", "._check_(" },
	{ "在_check_等待Acquire(Modify)回复",              ".Acquire(", "._check_(" },
	{ "在ReduceInvalid等待Lockey写锁",                 ".EnterWriteLock(", ".ReduceInvalid(" },
	{ "在ReduceInvalid等待Record1锁",                  ".EnterFairLock(", ".ReduceInvalid(" },
	{ "在ReduceInvalidAllLocalOnly等待Lockey写锁",     ".EnterWriteLock(", ".ReduceInvalidAllLocalOnly(" },
	{ "在TableCache.CleanNow等待Acquire(Invalid)回复", ".Acquire(", ".CleanNow(" },
	{ "在TableCache.CleanNow里等待下次循环",           ".CleanNow(TableCache.java:166)" },
	{ "在__TryWaitFlushWhenReduce里等待sleep",         ".__TryWaitFlushWhenReduce(Application.java:341)" },
	{ "在Checkpoint线程等待定时器",                    ".Object.wait(", ".Checkpoint.Run(" },
	{ "在Selector线程等待NIO事件",                     ".Selector.run(Selector.java:67)" },
	{ "AchillesHeelDaemon线程",                        "(AchillesHeelDaemon.java:146)" },
	{ "永久等待(主线程)",                              ".Object.wait(", ".main(GlobalCacheManagerServer.java:" },
}

local needKnowns = {
	"at Zeze.",
	"at Infinite.",
}

local counts = {}
for _, thread in pairs(threads) do
	local isKnown
	for i, known in ipairs(knowns) do
		local found = true
		for j = 2, 999 do
			if not known[j] then break end
			if not thread.text:find(known[j], 1, true) then
				found = false
				break
			end
		end
		if found then
			counts[i] = (counts[i] or 0) + 1
			isKnown = true
			break
		end
	end
	if not isKnown then
		local found
		for _, needKnown in ipairs(needKnowns) do
			if thread.text:find(needKnown, 1, true) then
				found = true
				break
			end
		end
		if found then
			counts[0] = (counts[0] or 0) + 1
			if not f then f = io.open("stack_global_others.log", "wb") end
			f:write(thread.text)
			f:write "\n\n"
		else
			counts[-1] = (counts[-1] or 0) + 1
		end
	end
end
if f then f:close() end
for i, known in ipairs(knowns) do
	if counts[i] then
		print(string.format("%4d 个线程 %s", counts[i], known[1]))
	end
end
print "------------------------------------------------------------"
if counts[0] then
	print(string.format("%4d 个线程 未知 (通过stack_global_others.log补充线程信息)", counts[0]))
end
if counts[-1] then
	print(string.format("%4d 个线程 非Zeze和Infinite所属,或线程池中的空闲线程", counts[-1]))
end
print "============================================================ END"
