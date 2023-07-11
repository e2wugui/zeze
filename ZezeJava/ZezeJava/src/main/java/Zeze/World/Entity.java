package Zeze.World;

import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.function.Function;
import Zeze.Builtin.World.BAoiOperate;
import Zeze.Builtin.World.BObject;
import Zeze.Builtin.World.BObjectId;
import Zeze.Net.Binary;

public class Entity {
	private Entity parent;
	private final HashSet<Entity> children = new HashSet<>();
	private final BObjectId id;
	private final BObject bean;

	public Entity(BObjectId id) {
		this.id = id;
		this.bean = new BObject();
	}

	public boolean isPlayer() {
		return !bean.getLinkName().isEmpty();
	}

	public static void buildPlayer(Map<BObjectId, BAoiOperate.Data> result,
								   Entity self, Function<Entity, Binary> encoder) {
		if (self != null && self.isPlayer()) {
			var child = new BAoiOperate.Data();

			child.setOperateId(IAoi.eOperateIdFull);
			child.setParam(encoder.apply(self));

			result.put(self.id, child);
		}
	}

	/**
	 * 构建整颗树。
	 */
	public static void buildNonePlayerTree(Map<BObjectId, BAoiOperate.Data> result,
										   Entity self, Function<Entity, Binary> encoder) {
		if (null == self)
			return; // helper 方便外面不检查就调用这个。

		// 过滤掉玩家
		if (!self.isPlayer() && !result.containsKey(self.id)) {
			var child = new BAoiOperate.Data();

			child.setOperateId(IAoi.eOperateIdFull);
			child.setParam(encoder.apply(self));

			result.put(self.id, child);
			for (var c : self.children) {
				buildNonePlayerTree(child.getChildren(), c, encoder);
			}
		}
	}

	/**
	 * 如果存在关联树，找到关联树的根。
	 * @return last parent, if no parent return null.
	 */
	public Entity lastParent() {
		Entity last = null;
		for (var p = parent; p != null; p = p.parent) {
			last = p;
		}
		return last;
	}

	public BObjectId getId() {
		return id;
	}

	public BObject getBean() {
		return bean;
	}

	public Set<Entity> getChildren() {
		return children;
	}

	public Entity getParent() {
		return parent;
	}

	public void setParent(Entity parent) {
		this.parent = parent;
	}
}
