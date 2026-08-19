package Zeze.Game;

import Zeze.Net.Binary;
import Zeze.Net.Protocol;

public sealed interface OnlineSpec permits ProtocolOnlineSpec, EncodedPrototolOnlineSpec {
	static ProtocolOnlineSpec ofProtocol(Protocol<?> p) {
		return new ProtocolOnlineSpec(p);
	}

	static EncodedPrototolOnlineSpec ofProtocolEncoded(long typeId, Binary fullEncodedProtocol) {
		return new EncodedPrototolOnlineSpec(typeId, fullEncodedProtocol);
	}
}
