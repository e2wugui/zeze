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
		return !bean.getLinkName().isEmpty() && bean.getLinkSid() > 0;
	}

	/**
	 * 构建整颗树。
	 */
	public static void buildTree(Map<BObjectId, BAoiOperate.Data> result, Entity self, Function<Entity, Binary> encoder) {
		if (null != self.parent)
			throw new RuntimeException("is not a root.");

		if (!result.containsKey(self.id)) {
			var child = new BAoiOperate.Data();

			child.setOperateId(IAoi.eOperateIdFull);
			child.setParam(encoder.apply(self));

			result.put(self.id, child);
			for (var c : self.children) {
				buildTree(child.getChildren(), c, encoder);
			}
		}
	}

	/**
	 * 找到关联树的根，鼓励节点返回this。
	 * @return root
	 */
	public Entity root() {
		var root = this;
		for (var p = parent; p != null; p = p.parent) {
			root = p;
		}
		return root;
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
