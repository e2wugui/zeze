
IsMainRunning = true

local Zeze = require 'Zeze'

while (IsMainRunning)
do
	print(Zeze.ServiceClient)
	ZezeUpdate(Zeze.ServiceClient)
end
