package Zeze.Util;

import Zeze.Net.Service;

// 按 typeId 查找协议工厂与处理器的函数接口
@FunctionalInterface
public interface ProtocolFactoryFinder {
	Service.ProtocolFactoryHandle<?> find(long typeId);
}
