package Zeze.World;

import Zeze.Builtin.World.Query;

/**
 * World组件实现数据查询的Rpc处理器。
 */
public interface IQuery {
	long handle(String playerId, Query r) throws Exception;
}
