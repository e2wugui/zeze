package Zeze.World;

import Zeze.Builtin.World.Query;

public interface QueryHandler {
	long handle(Query r) throws Exception;
}
