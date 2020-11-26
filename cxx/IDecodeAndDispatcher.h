
#pragma once

#include "ByteBuffer.h"
#include "Net.h"

namespace Zeze
{
	namespace Net
	{
		class ToScriptDecodeAndDispatcher
		{
		public:
			virtual bool DecodeAndDispatch(Service* service, long long sessionId, int typeId, Zeze::Serialize::ByteBuffer& _os_) = 0;
		};
	}
}
