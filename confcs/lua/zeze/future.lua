local future = {}
future.__index = future

local _PENDING = 0
local _CANCELLED = 1
local _FINISHED = 2

---@class future
function future:new()
    local o = {}
    setmetatable(o, future)
    self._state = _PENDING
    self._result = nil
    self._exception = nil
    return o
end

function future:cancel()
    if self._state ~= _PENDING then
        return false
    end
    self._state = _CANCELLED
    return true
end

function future:result()
    return self._result
end

function future:set_result(result)
    if self._state ~= _PENDING then
        return
    end
    self._result = result
    self._state = _FINISHED
end

function future:set_exception()

end

function future:is_done()
    return self._state ~= _PENDING
end

function future:is_cancelled()
    return self._state == _CANCELLED
end

function future:add_done_callback()

end

function future:remove_done_callback()

end

function future:exception()

end

return future