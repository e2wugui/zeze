package Zeze.log;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;

import harness.Fast;

/**
 * 全服视图复用收敛判定（FileSessionManager.allViewMembersConverged）的纯逻辑直测：
 * 会话实际成员集是绑定与复用的唯一权威（FND30 zokermanager-01/02）——多余成员
 * （∈会话∉注册表，摘除/缩容方向）不收敛走关旧建新；缺失成员（∈注册表∉会话，构造期
 * 跳过/扩容上台）收敛可复用，由 SessionAll.operate 的缺册补员自愈承担（持续故障期
 * 缺员不逐请求全量重建）。
 */
@Fast
public class TestAllViewMemberConvergence {

	/** 多余成员（缩容方向）：会话含已摘除服务器——不收敛，视同 changeSession 重建缩容。 */
	@Test
	public void testExtraMemberNotConverged() {
		assertFalse(FileSessionManager.allViewMembersConverged(Set.of("a", "b"), Set.of("a")),
				"∈会话∉注册表的多余成员：必须重建缩容");
		assertFalse(FileSessionManager.allViewMembersConverged(Set.of("a", "b"), Set.of()),
				"注册表清空（全下线）：会话成员全部成多余——重建导向显式失败");
	}

	/** 缺失成员（补员方向）：注册表新增而会话未含——收敛复用，不逐请求全量重建。 */
	@Test
	public void testMissingMemberConverged() {
		assertTrue(FileSessionManager.allViewMembersConverged(Set.of("a"), Set.of("a", "b")),
				"∈注册表∉会话的缺失成员：由 operate 补员承担，不重建（避免故障期重建抖动）");
		assertTrue(FileSessionManager.allViewMembersConverged(Set.of("a", "b"), Set.of("b", "a")),
				"成员与注册表一致（顺序无关）：收敛");
	}

	/** 空成员集恒不收敛：0 成员会话不可复用（重建路径的成员校验对 0 成员显式失败）。 */
	@Test
	public void testEmptyMembersNeverConverged() {
		assertFalse(FileSessionManager.allViewMembersConverged(Set.of(), Set.of("a")),
				"0 成员会话：不可复用");
		assertFalse(FileSessionManager.allViewMembersConverged(Set.of(), Set.of()),
				"0 成员会话与空注册表：同样不可复用（导向重建→显式失败）");
	}
}
