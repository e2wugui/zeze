package metagame.World;

import metagame.builtin.World.Query;

/**
 * World组件实现数据查询的Rpc处理器。
 */
public interface IQuery {
	long handle(String account, String playerId, Query r) throws Exception;
}
