
IsMainRunning = true


require ('demo.Module1Impl'):Init()
require ('demo.Module2Impl'):Init()
require ('demo.Module1.Module11Impl'):Init()

while (IsMainRunning)
do
	local Zeze = require 'Zeze'
	ZezeUpdate(Zeze.ServiceClient)
end
