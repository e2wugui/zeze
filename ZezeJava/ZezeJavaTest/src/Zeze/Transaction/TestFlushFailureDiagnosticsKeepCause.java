package Zeze.Trans;

import java.util.Iterator;
import java.util.Set;

import Zeze.Application;
import Zeze.Config;
import Zeze.Transaction.Checkpoint;
import Zeze.Transaction.Record;
import harness.Fast;
import harness.FastServerIds;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Checkpoint.flushInternal 失败重抛前用 extractTableNames 组装诊断信息（表名清单），
 * 组装过程（再次遍历记录集）若抛出二次异常，会顶掉真正的 flush 失败原因——
 * 双重故障下原始 cause 丢失。
 *
 * 修复：诊断组装整体兜底，失败退化为占位，原始异常保持为 cause。
 * 用两轮各抛不同异常的 rigged 记录集精确构造双重故障。
 */
@Fast
public class TestFlushFailureDiagnosticsKeepCause {
	// 独立serverId隔离本地zeze_cache目录与其他@Fast测试（16373，growth=1；上限16383）。
	private static final int SERVER_ID = FastServerIds.TEST_FLUSH_DIAGNOSTICS_KEEP_CAUSE;

	private static Application newApp() throws Exception {
		var conf = new Config();
		conf.setServiceManager("disable");
		conf.setServerId(SERVER_ID);
		conf.setDefaultTableConf(new Config.TableConf());
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseUrl("flush_diag_cause_" + conf.getServerId());
		conf.getDatabaseConfMap().putIfAbsent("", dbConf);
		return new Application("TestFlushFailureDiagnosticsKeepCause", conf);
	}

	/** 第一轮迭代（flush主循环）抛flush-source；第二轮（诊断重迭代）抛二次异常。 */
	private static Iterable<Record> failTwice() {
		return () -> new Iterator<>() {
			private int round = 0;
			private boolean thrown;

			@Override
			public boolean hasNext() {
				return !thrown;
			}

			@Override
			public Record next() {
				thrown = true;
				if (++round == 1)
					throw new IllegalStateException("flush-source");
				throw new NullPointerException("diagnostic-rethrow");
			}
		};
	}

	@Test
	public void testDiagnosticsFailureDoesNotMaskFlushCause() throws Exception {
		var app = newApp();
		app.start();
		try {
			Checkpoint checkpoint = app.getCheckpoint();
			var ex = Assertions.assertThrows(RuntimeException.class,
					() -> checkpoint.flush(failTwice(), Set.of(), null));
			var cause = ex.getCause();
			Assertions.assertNotNull(cause, "重抛的失败必须保留原始cause");
			Assertions.assertInstanceOf(IllegalStateException.class, cause,
					"cause必须是flush主循环的原始异常，而非诊断组装的二次异常");
			Assertions.assertEquals("flush-source", cause.getMessage());
		} finally {
			app.stop();
		}
	}
}
