
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
			virtual bool DecodeAndDispatch(Service* service, int64_t sessionId, int moduleId, int protocolId, ByteBuffer& _os_) = 0;
			virtual ~IDecodeAndDispatcher() { }
		};
	}
}
