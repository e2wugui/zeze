package Zeze.Dbh2.Master;

import java.io.File;
import Zeze.Builtin.Dbh2.Master.BClearInUse;
import Zeze.Builtin.Dbh2.Master.BGetDataWithVersion;
import Zeze.Builtin.Dbh2.Master.BSaveDataWithSameVersion;
import Zeze.Builtin.Dbh2.Master.BSetInUse;
import Zeze.Builtin.Dbh2.Master.ClearInUse;
import Zeze.Builtin.Dbh2.Master.GetDataWithVersion;
import Zeze.Builtin.Dbh2.Master.SaveDataWithSameVersion;
import Zeze.Builtin.Dbh2.Master.SetInUse;
import Zeze.IModule;
import Zeze.Net.Binary;
import Zeze.Raft.LogSequence;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND19 GA-C06回归：SetInUse/ClearInUse/SaveDataWithSameVersion的eSuccess必须在
 * trans.commit()之后置位（预置在前时commit抛出会让finally向客户端发送假成功，
 * 破坏CAS与单实例护栏契约）。
 * commit失败分支（写冲突/IO错误）无法在不损坏rocks的前提下注入，这里钉住
 * 重排后的成功语义不变：成功路径返回eSuccess、CAS版本推进、实例表进出正确。
 * （放本包以直接调用protected的Process*Request。）
 */
@Fast
public class TestMasterGlobalDataSuccessAfterCommit {

	@Test
	public void testSaveDataWithSameVersion() throws Exception {
		var home = "testFnd19GA06SaveData";
		LogSequence.deleteDirectory(new File(home));
		var master = new Master(home, new Zeze.Config());
		try {
			var key = new Binary(new byte[]{1});

			// 首次插入：version以参数值落盘。
			var insert = new SaveDataWithSameVersion();
			insert.Argument.setKey(key);
			insert.Argument.setData(new Binary(new byte[]{1}));
			insert.Argument.setVersion(0);
			Assertions.assertEquals(0, master.ProcessSaveDataWithSameVersionRequest(insert));
			Assertions.assertEquals(BSaveDataWithSameVersion.eSuccess, IModule.getErrorCode(insert.getResultCode()));
			Assertions.assertEquals(0, insert.Result.getVersion());

			// CAS成功：版本+1。
			var update = new SaveDataWithSameVersion();
			update.Argument.setKey(key);
			update.Argument.setData(new Binary(new byte[]{2}));
			update.Argument.setVersion(0);
			Assertions.assertEquals(0, master.ProcessSaveDataWithSameVersionRequest(update));
			Assertions.assertEquals(BSaveDataWithSameVersion.eSuccess, IModule.getErrorCode(update.getResultCode()));
			Assertions.assertEquals(1, update.Result.getVersion());

			// 过期版本被拒绝：数据不变。
			// （直接调用不经派发层，handler返回值不会写入resultCode，断言用返回值；
			// 成功路径的eSuccess是显式预置，resultCode断言有效。）
			var stale = new SaveDataWithSameVersion();
			stale.Argument.setKey(key);
			stale.Argument.setData(new Binary(new byte[]{3}));
			stale.Argument.setVersion(0);
			var staleRc = master.ProcessSaveDataWithSameVersionRequest(stale);
			Assertions.assertEquals(BSaveDataWithSameVersion.eVersionMismatch, IModule.getErrorCode(staleRc));

			// 数据未被stale覆盖。
			var get = new GetDataWithVersion();
			get.Argument.setKey(key);
			Assertions.assertEquals(0, master.ProcessGetDataWithVersionRequest(get));
			Assertions.assertEquals(1, get.Result.getVersion());
			Assertions.assertEquals(1, get.Result.getData().size());
		} finally {
			master.close();
			LogSequence.deleteDirectory(new File(home));
		}
	}

	@Test
	public void testSetAndClearInUse() throws Exception {
		var home = "testFnd19GA06SetInUse";
		LogSequence.deleteDirectory(new File(home));
		var master = new Master(home, new Zeze.Config());
		try {
			var set = new SetInUse();
			set.Argument.setLocalId(7);
			set.Argument.setGlobal("g");
			Assertions.assertEquals(0, master.ProcessSetInUseRequest(set));
			Assertions.assertEquals(BSetInUse.eSuccess, IModule.getErrorCode(set.getResultCode()));

			// 重复localId被拒（单实例护栏）。返回值断言（同上，不经派发层）。
			var dup = new SetInUse();
			dup.Argument.setLocalId(7);
			dup.Argument.setGlobal("g");
			var dupRc = master.ProcessSetInUseRequest(dup);
			Assertions.assertEquals(BSetInUse.eInstanceAlreadyExists, IModule.getErrorCode(dupRc));

			// clear后可重新set。
			var clear = new ClearInUse();
			clear.Argument.setLocalId(7);
			clear.Argument.setGlobal("g");
			Assertions.assertEquals(0, master.ProcessClearInUseRequest(clear));
			Assertions.assertEquals(BClearInUse.eSuccess, IModule.getErrorCode(clear.getResultCode()));

			var again = new SetInUse();
			again.Argument.setLocalId(7);
			again.Argument.setGlobal("g");
			Assertions.assertEquals(0, master.ProcessSetInUseRequest(again));
			Assertions.assertEquals(BSetInUse.eSuccess, IModule.getErrorCode(again.getResultCode()));
		} finally {
			master.close();
			LogSequence.deleteDirectory(new File(home));
		}
	}
}
