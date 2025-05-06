package Game.Rank;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import Game.App;
import Zeze.Arch.ProviderUserSession;
import Zeze.Arch.RedirectAll;
import Zeze.Arch.RedirectAllFuture;
import Zeze.Arch.RedirectFuture;
import Zeze.Arch.RedirectHash;
import Zeze.Arch.RedirectKey;
import Zeze.Arch.RedirectResult;
import Zeze.Arch.RedirectToServer;
import Zeze.Hot.HotService;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.Serializable;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.Procedure;
import Zeze.Util.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 基本排行榜，实现了按long value从大到小进榜。
 * 增加排行被类型。在 solution.xml::beankey::BConcurrentKey中增加类型定义。
 * 然后在数据变化时调用 RunUpdateRank 方法更行排行榜。
 */
@SuppressWarnings({"MethodMayBeStatic", "RedundantSuppression"})
public class ModuleRank extends AbstractModule implements IModuleRank {
	private static final Logger logger = LogManager.getLogger(ModuleRank.class);
	public final void Start(App app) {
	}

	public final void Stop(App app) {
	}

	@Override
	public void start() throws Exception {
		Start(App);
	}

	@Override
	public void stop() throws Exception {
		Stop(App);
	}

	@Override
	public void upgrade(HotService old) throws Exception {

	}

	/******************************** ModuleRedirect 测试 *****************************************/
	// 单发给某个serverId执行(找不到或没连接会抛异常),可以是本服. 返回类型可以是void或RedirectFuture<自定义结果类型或Long(resultCode)>
	@Override
	@RedirectToServer(version = 1)
	public RedirectFuture<TestToServerResult> TestToServer(int serverId, int in) { // 首个参数serverId是固定必要的特殊参数,后面是自定义输入参数
		var result = new TestToServerResult();
		result.setOut(in);
		result.setServerId(App.Zeze.getConfig().getServerId());
		return RedirectFuture.finish(result); // 同步完成则先finish再返回,异步则可在返回后在其它位置调用finish完成
	}

	// 第一个参数hash是固定的特殊参数
	@Override
	@RedirectHash(version = 2) // 单发给某个hash值指定的server执行,可能是本服,找不到hash节点也会在本服执行. 返回类型同ToServer
	public RedirectFuture<TestHashResult> TestHash(int hash, int in) { // 首个参数hash是固定必要的特殊参数,后面是自定义输入参数
		var f = new RedirectFuture<TestHashResult>();
		Task.run(App.Zeze.newProcedure(() -> {
			TestHashResult result = new TestHashResult();
			result.setHash(hash);
			result.setOut(in);
			result.setServerId(App.Zeze.getConfig().getServerId());
			f.setResult(result); // 异步完成
			return Procedure.Success;
		}, "TestHashAsync"), null, null, DispatchMode.Normal);
		return f;
	}

	@Override
	@RedirectAll(version = 3) // 广播请求并获取所有回复结果
	public RedirectAllFuture<TestToAllResult> TestToAll(int hash, int in) throws Exception { // 首个参数hash在发起方是hash总数,处理方是当前hash,后面是自定义参数列表
		System.out.println("TestToAll hash=" + hash + ", in=" + in);
		switch (hash) {
		case 0: // local sync
		case 1: // remote sync
			var result = new TestToAllResult();
			result.out = in;
			return RedirectAllFuture.result(result);
		case 2: // local exception
		case 3: // remote exception
			throw new Exception("not bug, only for test");
		case 4: // local async
		case 5: // remote async
			var future = RedirectAllFuture.<TestToAllResult>async(); // 启用异步方式,之后在future.asyncResult()时回复结果
			Task.run(App.Zeze.newProcedure(() -> {
				var result1 = new TestToAllResult();
				result1.out = in;
				future.asyncResult(result1);
				return Procedure.Success;
			}, "TestToAllAsync"), null, null, DispatchMode.Normal);
			return future;
		}
		throw new UnsupportedOperationException();
	}

	@RedirectToServer // 返回结果可以是Long类型,表示只有resultCode值
	public RedirectFuture<Long> TestToServerLongResult(int serverId) { // 可以没有自定义输入参数,但必须至少有serverId参数
		return RedirectFuture.finish(Procedure.Success);
	}

	@RedirectHash
	public RedirectFuture<Long> TestHashLongResult(int hash) { // 可以没有自定义输入参数,但必须至少有hash参数
		return RedirectFuture.finish(Procedure.Success);
	}

	@RedirectToServer
	public RedirectFuture<String> TestToServerStringResult(int serverId) {
		return RedirectFuture.finish("ok");
	}

	@RedirectToServer
	public RedirectFuture<Binary> TestToServerBinaryResult(int serverId) {
		return RedirectFuture.finish(new Binary("ok"));
	}

	@RedirectToServer(timeout = 1000) // 返回结果可以是Bean类型,其中如果有setResultCode(long)方法则会自动设置成resultCode
	public RedirectFuture<BBeanResult> TestToServerBeanResult(int serverId) { // 可以没有自定义输入参数,但必须至少有serverId参数
		return RedirectFuture.finish(new BBeanResult());
	}

	public static class GenericResult<T extends Serializable> {
		public long resultCode;
		public T ser;
		public java.io.Serializable obj;
	}

	@RedirectHash(timeout = 2000)
	public RedirectFuture<GenericResult<BBeanResult>> TestHashGenericResult(int serverId, @RedirectKey Long arg) {
		return RedirectFuture.finish(new GenericResult<>());
	}

	@RedirectToServer
	public void TestToServerNoResult(int serverId, @RedirectKey List<Long> longList) {
	}

	@RedirectHash
	public void TestHashNoResult(@RedirectKey int hash, BBeanResult rankList) {
	}

	@RedirectAll(timeout = 3000)
	public void TestAllNoResult(int hash, java.io.Serializable obj) {
	}

	@RedirectAll
	public RedirectAllFuture<RedirectResult> TestSimpleResult(int hash) {
		return RedirectAllFuture.result(new RedirectResult());
	}

	public static final class InnerBean implements Serializable {
		public int a;

		@Override
		public void encode(ByteBuffer bb) {
			bb.WriteInt(a);
		}

		@Override
		public void decode(IByteBuffer bb) {
			a = bb.ReadInt();
		}
	}

	@RedirectToServer
	public void TestInnerClass(int serverId, InnerBean inner, @RedirectKey long key) {
	}

	@RedirectToServer(oneByOne = false)
	public void TestBeanList(int serverId, List<InnerBean> inner, List<Long> longList, Map<Long, InnerBean> map) {
	}

    @Override
    protected long ProcessCGetRankList(Game.Rank.CGetRankList p) {
        return Zeze.Transaction.Procedure.NotImplement;
    }

	// ZEZE_FILE_CHUNK {{{ GEN MODULE @formatter:off
    public ModuleRank(Game.App app) {
        super(app);
    }
	// ZEZE_FILE_CHUNK }}} GEN MODULE @formatter:on
}
