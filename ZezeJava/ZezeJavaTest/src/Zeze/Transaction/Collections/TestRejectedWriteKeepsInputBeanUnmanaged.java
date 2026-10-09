package Zeze.Transaction.Collections;

import Zeze.Transaction.EmptyBean;
import Zeze.Transaction.Record;
import Zeze.Transaction.TableKey;
import harness.Fast;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 受管容器的拒绝写入不得污染输入bean：旧实现先initRootInfoWithRedo（改写归属，
 * 普通字段写不受事务回滚保护）后getCurrentVerifyWrite（无事务/不可写时抛出）——
 * 拒绝后集合size不变而bean已isManaged=true，复用（再加入任何受管容器）抛
 * HasManagedException，异常恢复要求复制或丢弃整个对象。
 * 修复：先取写权限后挂接；批量路径先全量预检（null与已受管），任何一项失败
 * 不留下已挂接的前段项。
 */
@Fast
public class TestRejectedWriteKeepsInputBeanUnmanaged {

	private static final class Env {
		final Record.RootInfo root;
		final EmptyBean owner = new EmptyBean();

		Env() {
			root = new Record.RootInfo(null, new TableKey(123, 1L));
			owner.initRootInfo(root, null);
		}
	}

	@Test
	public void rejectedListAddKeepsBeanReusable() {
		var env = new Env();
		var list = new PList2<EmptyBean>(EmptyBean.class);
		list.initRootInfo(env.root, env.owner);
		var candidate = new EmptyBean();
		assertFalse(candidate.isManaged());

		assertThrows(RuntimeException.class, () -> list.add(candidate), "事务外写入必须被拒绝");

		assertFalse(candidate.isManaged(), "拒绝路径不得污染输入bean归属（修复前isManaged=true）");
		assertDoesNotThrow(() -> candidate.initRootInfoWithRedo(env.root, env.owner),
				"被拒绝的bean必须仍可复用（修复前抛HasManagedException）");
	}

	@Test
	public void rejectedMapPutKeepsValueUnmanaged() {
		var env = new Env();
		var map = new PMap2<Long, EmptyBean>(Long.class, EmptyBean.class);
		map.initRootInfo(env.root, env.owner);
		var candidate = new EmptyBean();

		assertThrows(RuntimeException.class, () -> map.put(1L, candidate), "事务外写入必须被拒绝");
		assertFalse(candidate.isManaged(), "拒绝路径不得污染输入bean归属");
	}

	@Test
	public void rejectedAddAllLeavesNoPartiallyAttachedItems() {
		var env = new Env();
		var list = new PList2<EmptyBean>(EmptyBean.class);
		list.initRootInfo(env.root, env.owner);
		var item1 = new EmptyBean();
		var item2 = new EmptyBean();
		item2.initRootInfo(env.root, env.owner); // 已受管：批量后段项注定HasManagedException

		assertThrows(RuntimeException.class, () -> list.addAll(java.util.List.of(item1, item2)));
		assertFalse(item1.isManaged(), "批量拒绝不得留下已挂接的前段项（修复前item1已污染）");
	}
}
