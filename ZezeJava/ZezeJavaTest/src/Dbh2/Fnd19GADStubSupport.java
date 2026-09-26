package Dbh2;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import Zeze.Component.Threading;
import Zeze.Config;
import Zeze.Net.ProtocolHandle;
import Zeze.Net.Rpc;
import Zeze.Services.ServiceManager.AbstractAgent;
import Zeze.Services.ServiceManager.AutoKey;
import Zeze.Services.ServiceManager.BAllocateIdArgument;
import Zeze.Services.ServiceManager.BAllocateIdResult;
import Zeze.Services.ServiceManager.BEditService;
import Zeze.Services.ServiceManager.BServerLoad;
import Zeze.Services.ServiceManager.BSubscribeArgument;
import Zeze.Services.ServiceManager.BUnSubscribeArgument;
import org.jetbrains.annotations.NotNull;

/**
 * FND19 GA-D0x测试共享桩：Dbh2AgentManager构造所需的最小AbstractAgent，
 * 与远程提交模式的最小配置文件（形态对齐UnitTest.Zeze.Services.NullTid128Agent先例）。
 */
final class Fnd19GADStubSupport {
	private Fnd19GADStubSupport() {
	}

	/** 不联网的AbstractAgent桩：Dbh2AgentManager构造只调用getAutoKey（本地map操作，可达）。 */
	static final class NullServiceAgent extends AbstractAgent {
		@Override
		protected void allocate(@NotNull AutoKey autoKey, int pool) {
			throw new UnsupportedOperationException();
		}

		@Override
		protected boolean allocateAsync(@NotNull String globalName, int allocCount,
										@NotNull ProtocolHandle<Rpc<BAllocateIdArgument, BAllocateIdResult>> callback) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void start() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void waitReady() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void editService(@NotNull BEditService arg) {
			throw new UnsupportedOperationException();
		}

		@Override
		public @NotNull CompletableFuture<List<SubscribeState>> subscribeServicesAsync(@NotNull BSubscribeArgument info) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void unSubscribeService(@NotNull BUnSubscribeArgument arg) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean setServerLoad(@NotNull BServerLoad load) {
			throw new UnsupportedOperationException();
		}

		@Override
		public @NotNull Threading getThreading() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void close() {
			throw new UnsupportedOperationException();
		}
	}

	/**
	 * 远程提交模式（Dbh2LocalCommit=false + CommitServerAddress直连）的最小配置文件。
	 * 该模式下Dbh2AgentManager构造不创建本地Commit/CommitRocks（省去CommitRocksHome系统属性
	 * 与磁盘副作用，@Fast车道无全局状态竞争）；本组测试不触碰提交路径，地址用不可达值即可。
	 */
	static Path writeRemoteCommitConfig(Path tempDir) throws Exception {
		var xml = tempDir.resolve("fnd19gad-remote-commit.xml");
		Files.writeString(xml, """
				<?xml version="1.0" encoding="utf-8"?>
				<zeze Dbh2LocalCommit="false">
					<CustomizeConf Name="Dbh2Config" CommitServerAddress="127.0.0.1:1"/>
				</zeze>
				""");
		return xml;
	}
}
