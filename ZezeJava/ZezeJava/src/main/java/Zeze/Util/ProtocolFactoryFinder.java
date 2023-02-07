package Zeze.Util;

import Zeze.Net.Service;

@FunctionalInterface
public interface ProtocolFactoryFinder {
	Service.ProtocolFactoryHandle<?> find(long typeId);
}
