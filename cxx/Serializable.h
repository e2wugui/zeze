#pragma once

namespace Zeze {
	class ByteBuffer;

	class Serializable
	{
	public:
		virtual ~Serializable() {}
		virtual void Encode(ByteBuffer& bb) const = 0;
		virtual void Decode(ByteBuffer& bb) = 0;
	};
}
