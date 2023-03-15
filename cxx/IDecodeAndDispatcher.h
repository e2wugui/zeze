
#pragma once

#include "ByteBuffer.h"
#include "Net.h"

namespace Zeze
{
	namespace Net
	{
		class IDecodeAndDispatcher
		{
		public:
			virtual bool DecodeAndDispatch(Service* service, long long sessionId, int moduleId, int protocolId, ByteBuffer& _os_) = 0;
			virtual ~IDecodeAndDispatcher() { }
		};
	}
}
