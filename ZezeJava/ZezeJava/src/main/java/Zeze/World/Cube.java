package Zeze.World;

import java.util.HashMap;
import Zeze.Builtin.World.BObject;
import Zeze.Builtin.World.ObjectId;

public class Cube {
	private final HashMap<ObjectId, BObject> objects = new HashMap<>();

	public final BObject getObject(ObjectId oid) {
		return objects.get(oid);
	}

	public final void addObject(ObjectId oid, BObject object) {
		objects.put(oid, object);
	}

	public final HashMap<ObjectId, BObject> getObjects() {
		return objects;
	}
}
