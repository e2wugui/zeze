package Zeze.World;

import java.util.HashMap;
import Zeze.Builtin.World.BObject;
import Zeze.Builtin.World.ObjectId;

public class Grid {
	private final HashMap<ObjectId, BObject> objectIds = new HashMap<>();

	public final BObject getObject(ObjectId oid) {
		return objectIds.get(oid);
	}
}
