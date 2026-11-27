package Zeze.Game;

import java.util.Objects;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import Zeze.AppBase;
import Zeze.Arch.ProviderImplement;
import Zeze.Arch.ProviderService;
import Zeze.Builtin.Provider.LinkBroken;
import Zeze.Net.AsyncSocket;
import Zeze.Transaction.Procedure;
import Zeze.Util.Task;
import org.jspecify.annotations.Nullable;

/**
 * Game 版带在线表的 Provider 实现模板：管理默认 Online 与多个 OnlineSet，处理 LinkBroken 下线。
 */
public class ProviderWithOnline extends ProviderImplement {
	protected @Nullable Online online; // 默认的Online. 需要外面调用create方法创建并初始化。App.Start.
	private @Nullable ProviderLoadWithOnline load;

	// 运行期高频无锁读、stop() 持锁清空，用 ConcurrentHashMap 保证可见性。
	protected final ConcurrentHashMap<String, Online> onlineSetMap = new ConcurrentHashMap<>(); // 所有创建过的Online,默认Online的key是空字符串. 需要外面调用默认Online.createOnlineSet创建

	public @Nullable Online getOnline() {
		return online;
	}

	@Override
	public @Nullable ProviderLoadWithOnline getLoad() {
		return load;
	}

	public @Nullable Online getOnline(@Nullable String name) {
		return name != null && !name.isEmpty() ? onlineSetMap.get(name) : online;
	}

	public void foreachOnline(Consumer<Online> consumer) {
		onlineSetMap.values().forEach(consumer);
	}

	@Override
	protected long ProcessLinkBroken(LinkBroken p) throws Exception {
		if (!AsyncSocket.ENABLE_PROTOCOL_LOG)
			//noinspection DataFlowIssue
			logger.info("LinkBroken[{}]: {}", p.getSender().getSessionId(), AsyncSocket.toStr(p.Argument));
		// 目前仅需设置online状态。
		var online = this.online;
		// context 在角色模式下为 roleId，混合装配下账号模式为非数字 clientId，解析失败则跳过。
		var roleId = parseRoleId(p.Argument.getUserState().getContext());
		if (null != roleId && null != online) {
			var onlineSet = online.getOnline(p.Argument.getUserState().getOnlineSetName());
			if (null != onlineSet)
				return onlineSet.linkBroken(p.Argument.getAccount(), roleId,
						ProviderService.getLinkName(Objects.requireNonNull(p.getSender())), p.Argument.getLinkSid());
		}
		return Procedure.Success;
	}

	/**
	 * 解析UserState.context中的roleId。
	 * 空串或非数字串（如账号在线模式的clientId）返回null，表示非角色会话，没有roleId。
	 */
	public static @Nullable Long parseRoleId(String context) {
		if (context.isEmpty())
			return null;
		try {
			return Long.parseLong(context);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	// 创建默认的Online和指定name的若干Online,重复创建会抛异常
	public void create(AppBase app, String... names) throws Exception {
		lock();
		try {
			if (online != null)
				throw new IllegalStateException("duplicate default");
			online = Online.create(app);
			online.Initialize(app);
			onlineSetMap.put("", online);
			for (var name : names) {
				if (onlineSetMap.containsKey(name))
					throw new IllegalStateException("duplicate name='" + name + '\'');
				onlineSetMap.put(name, online.createOnlineSet(app, name));
			}
			// load报告仅定义在默认online实例中，OnlineSet不报告load，
			// 【以后考虑改造定义方式】
			// 即，OnlineSet的Load不独立报告，会单独定义但被综合以后，由默认Load统一报告。
			load = new ProviderLoadWithOnline(online);
			//noinspection DataFlowIssue
			var config = app.getZeze().getConfig();
			load.getOverload().register(Task.getThreadPool(), config);
		} finally {
			unlock();
		}
	}

	public void start() {
		lock();
		try {
			//noinspection DataFlowIssue
			load.start();
			//noinspection DataFlowIssue
			online.start();
		} finally {
			unlock();
		}
	}

	@Override
	public void stop() throws Exception {
		lock();
		try {
			if (online != null) {
				online.stop();
				onlineSetMap.clear();
				online = null;
			}
			if (load != null)
				load.stop();
		} finally {
			unlock();
		}
	}
}
