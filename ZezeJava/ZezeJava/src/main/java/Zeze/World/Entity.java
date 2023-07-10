package Zeze.World;

import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import Zeze.Builtin.World.BAoiOperate;
import Zeze.Builtin.World.BObject;
import Zeze.Builtin.World.BObjectId;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;

public class Entity {
	private Entity parent;
	private final HashSet<Entity> children = new HashSet<>();
	private final BObjectId id;
	private final BObject bean;

	public Entity(BObjectId id) {
		this.id = id;
		this.bean = new BObject();
	}

	/**
	 * 构建整颗树。
	 */
	public static void buildTree(Map<BObjectId, BAoiOperate.Data> result, Entity self) {
		if (null != self.parent)
			throw new RuntimeException("is not a root.");

		if (!result.containsKey(self.id)) {
			var child = new BAoiOperate.Data();

			child.setOperateId(IAoi.eOperateIdFull);
			// todo 暂定用继承来搞定自定义。
			self.encodeOperateFull(IAoi.eOperateIdFull, self.id, self.bean, child);

			result.put(self.id, child);
			for (var c : self.children) {
				buildTree(child.getChildren(), c);
			}
		}
	}

	@SuppressWarnings("MethodMayBeStatic")
	public void encodeOperateFull(int operateId, BObjectId oid, BObject data, BAoiOperate.Data operate) {
		if (operateId != IAoi.eOperateIdFull)
			throw new RuntimeException("special editId found, but encodeEdit not override.");

		// 默认实现是传输整个对象数据。
		// 【实际上这个一般也需要定制】
		// 【不能把服务器定义的数据全部都传给客户端】
		var bb = ByteBuffer.Allocate();
		data.encode(bb);
		operate.setParam(new Binary(bb));
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
