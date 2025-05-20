local module = {}

--- [[ AUTO GENERATE START ]] ---
function module.InternalInit()
    module.demo = {}
    module.demo.Module1 = require "module.demo.Module1.ModuleModule1"
    module.demo.Module1.Init()
end
--- [[ AUTO GENERATE END ]] ---

function module.Init()
    module.InternalInit()
end

return module
