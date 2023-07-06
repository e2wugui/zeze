package Zeze.World;

import Zeze.Builtin.World.Query;

public interface IQuery {
	long handle(Query r) throws Exception;
}
