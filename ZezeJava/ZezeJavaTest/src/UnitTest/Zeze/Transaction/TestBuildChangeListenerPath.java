package UnitTest.Zeze.Transaction;

import java.util.ArrayList;

import Zeze.Transaction.Bean;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Util.KV;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND5-04：buildChangeListenerPath 深层路径 variableId 恒取 this.variableId——
 * 第一级（this 在其 parent 中的字段编号）碰巧正确，第二级起应取「当前层子 Bean
 * 自己的 variableId」，原实现仍用 this 的编号，层级≥2 构建出错误路径。仓内零调用方
 * （预留 API），以 initRootInfo(null, parent) 脱离数据库构造三层 parent 链直接钉契约。
 */
@Fast
public class TestBuildChangeListenerPath {

	/** 最小桩 Bean：path 构建只走 parent/variableId 簿记，不触碰序列化内容。 */
	private static final class StubBean extends Bean {
		StubBean(int variableId) {
			super(variableId);
		}

		@Override
		public void encode(ByteBuffer bb) {
		}

		@Override
		public void decode(IByteBuffer bb) {
		}
	}

	@Test
	public void testDeepPathVariableIds() {
		var root = new StubBean(0); // 根：编号无意义
		var child = new StubBean(11); // child 在 root 中的字段编号
		var grand = new StubBean(22); // grand 在 child 中的字段编号
		// rootInfo=null 的非受管场景 parent 照样设置（initRootInfo 注释口径），仅搭簿记链
		child.initRootInfo(null, root);
		grand.initRootInfo(null, child);

		var path = new ArrayList<KV<Bean, Integer>>();
		grand.buildChangeListenerPath(path);

		Assertions.assertEquals(2, path.size(), "三层链应产出两条路径元素");
		Assertions.assertSame(child, path.get(0).getKey());
		Assertions.assertEquals(22, path.get(0).getValue(), "第1层编号=grand(22) 在 child 中的 variableId");
		Assertions.assertSame(root, path.get(1).getKey());
		Assertions.assertEquals(11, path.get(1).getValue(), "第2层编号=child(11) 在 root 中的 variableId，不得沿用 this 的 22");
	}

	@Test
	public void testSingleLevelUnchanged() {
		// 兼容红线：单层（this.parent 存在、再无更深层）行为与修复前一致
		var root = new StubBean(0);
		var child = new StubBean(33);
		child.initRootInfo(null, root);

		var path = new ArrayList<KV<Bean, Integer>>();
		child.buildChangeListenerPath(path);

		Assertions.assertEquals(1, path.size());
		Assertions.assertSame(root, path.get(0).getKey());
		Assertions.assertEquals(33, path.get(0).getValue());
	}
}
