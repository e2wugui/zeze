package Zeze.World;

import Zeze.Builtin.World.Query;

public interface IQueryHandler {
	long handle(Query r) throws Exception;
}
