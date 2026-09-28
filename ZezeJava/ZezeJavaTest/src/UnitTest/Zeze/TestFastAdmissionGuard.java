package UnitTest.Zeze;

import harness.Fast;
import harness.FastServerIds;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @Fast 准入自检：FastServerIds 号段表区间两两不重叠且 growth 为正（规约见
 * AGENTS.md）。绕过分配处私写 serverId 字面量不在本自检视野内，其防线是
 * Application.start 的 FileMutex fail-fast（同段并发必炸）。
 */
@Fast
public class TestFastAdmissionGuard {

	@Test
	public void serverIdSegmentsPairwiseDisjoint() {
		var segments = FastServerIds.segments();
		var violations = new ArrayList<String>();
		for (var i = 0; i < segments.size(); i++) {
			var a = segments.get(i);
			if (a.growth() <= 0)
				violations.add(a + "：growth 必须为正");
			for (var j = i + 1; j < segments.size(); j++) {
				var b = segments.get(j);
				if (a.base() < b.base() + b.growth() && b.base() < a.base() + a.growth())
					violations.add(a + " 与 " + b + "：号段重叠");
			}
		}
		assertTrue(violations.isEmpty(),
				"FastServerIds 号段冲突（有库 App serverId 须全局唯一，规约见 AGENTS.md）：\n"
						+ String.join("\n", violations));
	}

	@Test
	public void memoryUrlsPairwiseDistinct() {
		var seen = new HashSet<String>();
		var dups = new ArrayList<String>();
		for (var url : FastServerIds.memoryUrls())
			if (!seen.add(url))
				dups.add(url);
		assertTrue(dups.isEmpty(),
				"FastServerIds DatabaseMemory url 重复（同名 url 同 JVM 静默共享存储串数据）：\n"
						+ String.join("\n", dups));
	}
}
