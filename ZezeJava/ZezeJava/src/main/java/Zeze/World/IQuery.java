package Zeze.World;

import Zeze.Builtin.World.Query;

public interface IQuery {
	long handle(String playerId, Query r) throws Exception;
}
