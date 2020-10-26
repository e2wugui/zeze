
IsMainRunning = true


print('1111')
local Module1Impl = require 'demo.Module1Impl"
Module1Impl.Init()
print('2222')
local Module2Impl = require 'demo.Module2Impl"
Module2Impl.Init()
print('3333')
local Module11Impl = require 'demo.Module1.Module11Impl"
Module11Impl.Init()
print('4444')

while (IsMainRunning)
do
	local Zeze = require 'Zeze'
	ZezeUpdate(Zeze.ServiceClient)
end
