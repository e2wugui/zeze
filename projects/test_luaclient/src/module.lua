local module = {}

--- [[ AUTO GENERATE START ]] ---
function module.InternalInit()
    module.luatest = {}
    module.luatest.Module1 = require "module.luatest.Module1.ModuleModule1"
    module.luatest.Module1.Init()
end
--- [[ AUTO GENERATE END ]] ---

function module.Init()
    module.InternalInit()
end

return module
