local pairs = pairs
local setmetatable = setmetatable

--------------------------------------------------------
--- lua 的table在foreach 遍历的时候可以删除更改（具体请查看next的说明），但不能增加
--- 这里的结构，可以foreach时候增加
--- 如果不会foreach增加，不要使用这个结构，直接用table

local StreamMap = {}

function StreamMap:New()
    local instance = {}
    setmetatable(instance, self)
    self.__index = self
    instance.map = {}
    instance.add = nil
    instance.depth = 0
    return instance
end

function StreamMap:Get(k)
    self:_trycompact()

    local v = self.map[k]
    if v ~= nil then
        return v
    end

    if self.add then
        for i = 1, #self.add do
            local a = self.add[i]
            if k == a.k then
                if a.del then
                    return nil
                else
                    return a.v
                end
            end
        end
    end

    return nil
end

function StreamMap:_trycompact()
    if self.depth == 0 and self.add then
        local addlen = #self.add
        if addlen > 0 then
            --- compact
            for i = 1, addlen do
                local a = self.add[i]
                if not a.del then
                    self.map[a.k] = a.v
                end
            end
            self.add = nil
        end
    end
end

function StreamMap:Put(k, v)
    self:_trycompact()

    if self.depth == 0 then
        local old = self.map[k]
        self.map[k] = v
        return old
    end

    local old = self.map[k]
    if old ~= nil then
        self.map[k] = v
        return old
    end

    if self.add then
        for i = 1, #self.add do
            local a = self.add[i]
            if k == a.k then
                local oldv = a.v
                local olddel = a.del
                a.v = v
                a.del = nil
                if olddel then
                    return nil
                else
                    return oldv
                end
            end
        end
    else
        self.add = {}
    end
    self.add[#self.add + 1] = { k = k, v = v }
    return nil
end

function StreamMap:Remove(k)
    self:_trycompact()

    local old = self.map[k]
    if old ~= nil then
        self.map[k] = nil
        return old
    end

    if self.add then
        for i = 1, #self.add do
            local a = self.add[i]
            if (not a.del) and k == a.k then
                local oldv = a.v
                a.del = true
                a.v = nil
                return oldv
            end
        end
    end
    return nil
end

function StreamMap:ForEach(func, ...)
    self:_trycompact()

    self.depth = self.depth + 1

    local addlen
    if self.add then
        addlen = #self.add
    end

    for k, v in pairs(self.map) do
        func(k, v, ...)
    end

    if self.add and self.depth > 1 and addlen then
        local addlen2 = #self.add
        if addlen > addlen2 then
            addlen = addlen2
        end

        for i = 1, addlen do
            local a = self.add[i]
            if not a.del then
                func(a.k, a.v, ...)
            end
        end
    end

    self.depth = self.depth - 1
end


function StreamMap:Find(match, ...)
    self:_trycompact()

    local addlen
    if self.add then
        addlen = #self.add
    end
    for _, v in pairs(self.map) do
        if match(v, ...) then
            return v
        end
    end

    if self.add and addlen then
        local addlen2 = #self.add
        if addlen > addlen2 then
            addlen = addlen2
        end

        for i = 1, addlen do
            local a = self.add[i]
            if (not a.del) and match(a.v, ...) then
                return a.v
            end
        end
    end

    return nil
end

function StreamMap:Clear()
    self.map = {}
    self.add = nil
end

return StreamMap