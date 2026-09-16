package UnitTest.Zeze.Transaction;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import Zeze.Application;
import Zeze.Config;
import Zeze.Transaction.Record;
import Zeze.Transaction.TableX;
import harness.Fast;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * FND7-55 回归：stop窗口内Reduce降级路径（reduceShare/reduceInvalid/reduceInvalidAllLocalOnly）
 * 对null Checkpoint解引用——TableX.flushWhenReduce把getZeze().getCheckpoint()无判空传给
 * RelativeRecordSet.flushWhenReduce(@NotNull Checkpoint)，终检点已过后到达的Reduce直接NPE，
 * "降级前先落库"（durability-before-downgrade）承诺被打破且异常不可辨。
 * 修复：checkpoint==null时抛IllegalStateException——Reduce由finally以默认
 * StateReduceException fail-safe应答（对端acquire失败重试），本地不再静默NPE、
 * 不把未落库的降级当成功应答（那会让GCM把权限授予他人后跨进程丢失更新）。
 * 说明：FND7-54的停机拒绝生效后，通过公开API在checkpoint==null时已无法产生新的脏记录，
 * 到达此守卫只剩极窄的残余竞态——用反射直接调用私有flushWhenReduce钉死守卫行为
 * （传null Record：守卫在触碰r之前判定，修复前此处即NPE解引用）。
 */
@Fast
public class TestFnd755FlushWhenReduceNullCheckpoint {
	// 独立serverId+url：@Fast类并行时避免本地RocksCache目录互撞（对齐TestCheckpointRunThreadSentinel）。
	private static final int SERVER_ID = 7550;

	private Application app;
	private TableX<?, ?> table;

	@BeforeEach
	public void setUp() throws Exception {
		var conf = new Config();
		conf.setServiceManager("disable");
		conf.setServerId(SERVER_ID);
		conf.setDefaultTableConf(new Config.TableConf()); // 裸Config不会补默认值
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseType(Config.DbType.Memory);
		dbConf.setDatabaseUrl("fnd7_55_reduce_flush_" + SERVER_ID);
		conf.getDatabaseConfMap().put("", dbConf);
		app = new Application("TestFnd755FlushWhenReduceNullCheckpoint", conf);
		app.start();
		table = app.getTables().values().stream()
				.filter(t -> t instanceof TableX)
				.map(t -> (TableX<?, ?>)t)
				.findFirst()
				.orElseThrow(() -> new IllegalStateException("no TableX registered"));
	}

	@AfterEach
	public void tearDown() throws Exception {
		if (app.getStartState() != Application.StartState.eStopped)
			app.stop(); // 已停实例幂等
	}

	@Test
	public void testExplicitIllegalStateInsteadOfNpeWhenCheckpointStopped() throws Exception {
		app.stop(); // 终检点完成，checkpoint置null

		var method = TableX.class.getDeclaredMethod("flushWhenReduce", Record.class);
		method.setAccessible(true);
		var ex = assertThrows(InvocationTargetException.class, () -> method.invoke(table, (Object)null),
				"checkpoint已终结时降级flush必须显式拒绝");
		var cause = ex.getCause();
		assertInstanceOf(IllegalStateException.class, cause,
				"必须是显式IllegalStateException（FND7-55），不得是NullPointerException解引用");
		assertTrue(cause.getMessage().contains("checkpoint stopped"), "错误信息需指明停机窗口语义");
	}
}
