package Zeze.Util;

import Zeze.Net.Service;
import org.jspecify.annotations.Nullable;

// 按 typeId 查找协议工厂与处理器的函数接口
@FunctionalInterface
public interface ProtocolFactoryFinder {
	Service.@Nullable ProtocolFactoryHandle<?> find(long typeId);
}
