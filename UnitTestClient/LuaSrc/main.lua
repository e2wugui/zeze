
IsMainRunning = true


require ('demo.Module1Impl'):Init()
require ('demo.Module2Impl'):Init()
require ('demo.Module1.Module11Impl'):Init()

local Zeze = require 'Zeze'
ZezeConnect(Zeze.ServiceClient, '127.0.0.1', 9999, true)
while (IsMainRunning)
do
	ZezeUpdate(Zeze.ServiceClient)
end
