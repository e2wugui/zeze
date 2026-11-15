package Zeze.Transaction;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import Zeze.Application;
import Zeze.Serialize.ByteBuffer;
import Zeze.Services.Daemon;
import Zeze.Util.FastLock;
import Zeze.Util.Reflect;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

/*
 * 【问题】 Server失联，Global回收记录锁怎么处理？
 * Server与Global之间记录锁管理机制。这里锁有三个状态，Modify,Share,Invalid。
 * 下面分析Server-Global之间所有的交互。
 * <p>
 * 0. Acquire Rpc
 * Server向Global申请记录锁。Release也是通过这个操作处理，Release的锁状态是Invalid。
 * <p>
 * 1. NormalClose Rpc
 * Server正常退出时发送，Global主动释放所有分配到该Server的记录锁。
 * <p>
 * 2. Login Rpc
 * Server初次与Global建立连接时发送。Global会释放该Server上已经分配的记录锁。
 * 一般是Server宕机又重启了。
 * <p>
 * 3. ReLogin Rpc
 * Server与Global之间的连接短暂断开又重新连上时发送。Global简单的把新连接绑定上，不会释放已经分配的锁。
 * <p>
 * 4. KeepAlive Rpc
 * Server空闲时发送给Global。
 * ActiveTime = Acquire或者KeepAlive的活动时间。
 * Global为每个Server都维护ActiveTime。在收到Acquire或者KeepAlive设为now。
 * Server为每个Global都维护ActiveTime。在收到Acquire.Response或者KeepAlive.Response时设为now。
 * Server每秒检查ActiveTime，发现 now - ActiveTime > ServerIdleTimeout 时发送KeepAlive。
 * <p>
 * 5. Global发现Server断开连接
 * 不做任何处理。短暂断开允许重连。锁释放由Global-AchillesHeel-Daemon处理。
 * <p>
 * 6. Global-AchillesHeel-Daemon
 * 每5秒扫描一遍所有Server，发现 now - Server.ActiveTime > GlobalDaemonTimeout，释放该Server所有锁。【Important!】
 * a) 5秒慢检查;如果Server很多，避免轮询消耗太多cpu。慢检查会造成实际回收时间超出超时设置，但不会造成锁状态问题。
 * b) GlobalDaemonTimeout，最终超时。Server必须在这之前释放自己持有的锁或者退出进程；
 * <p>
 * 7. Server-AchillesHeel-Daemon
 * Server每秒扫描一遍Global，发现 now - Global.ActiveTime > ServerDaemonTimeout，启动本地释放锁线程。
 * a) ServerDaemonTimeout需要大于KeepAlive的空闲间隔 + 尝试重连的时间。
 * b) 本地释放锁在独立线程执行；守护线程巡检进度，并在原释放预算内预留最终保存窗口，截止仍未完成则退出。【Important！】
 * c) 守护线程一开始创建，做最简单的事情，确保需要的时候，最终的自杀能成功。【Important！】
 * <p>
 * 8. Timeout
 * a) ServerKeepAliveIdleTimeout < ServerDaemonTimeout;
 * b) ServerDaemonTimeout + ServerReleaseTimeout < GlobalDaemonTimeout; 必须满足而且不能太接近【Important！】
 * c) 其他Timeout：Acquire.Timeout, Reduce.Timeout, KeepAlive.Timeout, Server.FastErrorPeriod, Global.ForbidPeriod
 * <p>
 * 9. Timeout Config
 * a) 在Global配置三个参数：MaxNetPing=1000, ServerProcessTime=500, ServerReleaseTimeout=10*1000,
 * b) 其他Timeout配置全部从上面三个参数推导。
 * c) Gs不独立配置，Login的时候从Global得到配置。避免由于配置不一致导致问题。
 * d) Global多个实例允许不一样的配置，异构网络里面可能需要。简单起见，最好统一配置。
 * e) ServerReleaseTimeout 默认60秒，这个和Server.Cache.Capacity相关，而且会与应用事务竞争，可能需要长一些。
 * <p>
 * 10. Timeout Compute
 * *) Reconnect.Timer = 1000;
 * a) ServerKeepAliveIdleTimeout = MaxNetPing;
 * b) ServerDaemonTimeout = Reconnect.Timer * 8; // 期间允许8次重连尝试
 * c) ServerReleaseTimeout = 60 * 1000; // From Global，可配置
 * d) GlobalDaemonTimeout = ServerDaemonTimeout + ServerReleaseTimeout + MaxNetPing * 2 + 1000;
 * e) Reduce.Timeout = MaxNetPing + ServerProcessTime;
 * f) Acquire.Timeout = Reduce.Timeout + MaxNetPing
 * g) KeepAlive.Timeout = MaxNetPing;
 * h) Server.FastErrorPeriod = ServerDaemonTimeout / 2; // Global请求失败一次即进入这个超时，期间所有的Acquire都本地马上失败。
 * i) Global.ForbidPeriod = ServerDaemonTimeout / 2; // Reduce失败一次即进入这个超时，期间所有的Reduce马上失败。
 * j) ServerKillTimeout = ServerDaemonTimeout + ServerReleaseTimeout - 1000；通过GlobalOn原第二个超时字段下发外部Daemon。
 * k) 本地最终保存截止 = ActiveTime + ServerKillTimeout - 1000；Release提前预留min(30000, ServerReleaseTimeout / 2)毫秒。
 *    截止在Releaser创建时捕获，迟到心跳不能延长；fatal出口由Application启动独立截止守护后保存。
 * <p>
 * 11. 设计决策（相对基础思路的偏差）
 * a) Server发现Global断开连接时不马上释放本地资源，由AchillesHeelDaemon处理。
 * b) Global.Cleanup 手动释放锁禁用。
 * <p>
 * 12. Implement
 * 查看相关实现代码: 打开 Zeze.Services.AchillesHeelConfig，在每个配置上查看引用。
 * 下面是配置的使用情况统计。No表示没有用到。
 * ==============================================
 * Server-Implement         RaftAgent NormalAgent
 * ----------------------------------------------
 * *) Reconnect.Timer            No       Yes
 * a) ServerKeepAliveIdleTimeout Yes      Yes  - In Same Place
 * b) ServerDaemonTimeout        Yes      Yes  - In Same Place
 * c) ServerReleaseTimeout       Yes      Yes  - In Same Place
 * f) Acquire.Timeout            No       Yes
 * g) KeepAlive.Timeout          No       Yes
 * h) Server.FastErrorPeriod     No       Yes
 * ==============================================
 * Global-Implement       WithRaft   Normal  Async
 * ----------------------------------------------
 * d) GlobalDaemonTimeout             Yes   Yes
 * e) Reduce.Timeout                  Yes   Yes
 * i) Global.ForbidPeriod             Yes   Yes
 * <p>
 * *. 基础思路参见 zeze/GlobalCacheManager/Cleanup.txt，在此之上增加了KeepAlive。
 */
public class AchillesHeelDaemon {
	private static final Logger logger = LogManager.getLogger(AchillesHeelDaemon.class);

	private final Application zeze;
	private final GlobalAgentBase [] agents;
	private final @Nullable ThreadDaemon td;
	private final @Nullable ProcessDaemon pd;

	public AchillesHeelDaemon(Application zeze, GlobalAgentBase [] agents) throws Exception {
		this.zeze = zeze;
		this.agents = agents.clone();
		var peerPort = System.getProperty(Daemon.propertyNamePort);
		if (peerPort != null) {
			pd = new ProcessDaemon(Integer.parseInt(peerPort));
			td = null;
		} else {
			pd = null;
			td = new ThreadDaemon();
		}
	}

	public void start() {
		if (td != null)
			td.start();
		if (pd != null)
			pd.start();
	}

	public void stopAndJoin() {
		if (td != null)
			td.stopAndJoin();
		if (pd != null)
			pd.stopAndJoin();
	}

	public void deadlockReport() {
		if (pd != null) {
			try {
				Daemon.sendCommand(pd.udpSocket, pd.daemonSocketAddress, new Daemon.DeadlockReport());
			} catch (IOException e) {
				logger.error("", e);
			}
		}
	}

	public void onInitialize(GlobalAgentBase agent) {
		if (pd != null) {
			try {
				var config = agent.getConfig();
				Daemon.sendCommand(pd.udpSocket, pd.daemonSocketAddress,
						new Daemon.GlobalOn(zeze.getConfig().getServerId(), agent.globalCacheManagerHashIndex,
								config.serverDaemonTimeout, config.serverKillTimeout));
			} catch (IOException e) {
				logger.error("", e);
			}
		}
	}

	public void setProcessDaemonActiveTime(GlobalAgentBase agent, long value) {
		if (pd != null)
			pd.setActiveTime(agent, value);
	}

	private final class ProcessDaemon extends Thread {
		private final DatagramSocket udpSocket = new DatagramSocket(0, InetAddress.getLoopbackAddress());
		private final SocketAddress daemonSocketAddress;
		private final String fileName;
		private final RandomAccessFile raf;
		private final FileChannel channel;
		private final FastLock channelLock = new FastLock();
		private final MappedByteBuffer mmap;
		private final long[] lastReportTime = new long[agents.length];
		private volatile boolean running = true;
		// 由channelLock保护；注销后不能再发布心跳。
		private boolean shutdownUnregistered;

		public ProcessDaemon(int peer) throws Exception {
			daemonSocketAddress = new InetSocketAddress("127.0.0.1", peer);
			var file = Files.createTempFile("zeze", ".mmap").toFile();
			file.deleteOnExit();
			fileName = file.getAbsolutePath();
			raf = new RandomAccessFile(fileName, "rw");
			raf.setLength(8L * agents.length);
			channel = raf.getChannel();
			mmap = channel.map(FileChannel.MapMode.READ_WRITE, 0, channel.size());
			udpSocket.setSoTimeout(200);
			Daemon.sendCommand(udpSocket, daemonSocketAddress,
					new Daemon.Register(zeze.getConfig().getServerId(), agents.length, fileName));
		}

		public void setActiveTime(GlobalAgentBase agent, long value) {
			channelLock.lock();
			try {
				if (shutdownUnregistered)
					return;
				var index = agent.globalCacheManagerHashIndex;
				if (value - lastReportTime[index] < 1000)
					return;
				var bb = ByteBuffer.Allocate(8);
				bb.WriteLong8(value);
				try (var ignored = channel.lock()) {
					mmap.position(index * 8);
					mmap.put(bb.Bytes, 0, 8);
					lastReportTime[index] = value;
				} catch (Throwable ex) {
					logger.error("setActiveTime", ex);
				}
			} finally {
				channelLock.unlock();
			}
		}

		// -1使外部Monitor停止监管此槽，避免正常关停保存被idle超时打断。
		private void markShutdownUnregistered() {
			var bb = ByteBuffer.Allocate(8);
			bb.WriteLong8(-1);
			channelLock.lock();
			try {
				shutdownUnregistered = true;
				try (var ignored = channel.lock()) {
					for (int i = 0; i < agents.length; i++) {
						mmap.position(i * 8);
						mmap.put(bb.Bytes, 0, 8);
					}
				} catch (Throwable ex) {
					logger.error("markShutdownUnregistered", ex);
				}
			} finally {
				channelLock.unlock();
			}
		}

		@Override
		public void run() {
			try {
				while (running) {
					Daemon.Command cmd;
					try {
						cmd = Daemon.receiveCommand(udpSocket);
					} catch (SocketTimeoutException ex) {
						cmd = null;
					} catch (Throwable ex) {
						// 坏报文不能触发外层的致命退出。
						logger.error("ProcessDaemon.receiveCommand bad packet", ex);
						cmd = null;
					}
					if (cmd != null) {
						//noinspection SwitchStatementWithTooFewBranches
						switch (cmd.command()) {
						case Daemon.Release.Command:
							var r = (Daemon.Release)cmd;
							logger.info("receiveCommand {}", r.globalIndex);

							if (r.globalIndex < 0 || r.globalIndex >= agents.length) {
								logger.error("ProcessDaemon.receiveCommand Release bad globalIndex={}, agents.length={}",
										r.globalIndex, agents.length);
								break;
							}

							if (Reflect.inDebugMode) {
								logger.warn("ProcessDaemon Release command skipped in debug mode, globalIndex={}",
										r.globalIndex);
								break;
							}
							try {
								var agent = agents[r.globalIndex];
								var config = agent.getConfig();
								var rr = agent.checkReleaseTimeout(System.currentTimeMillis(), config.serverReleaseTimeout);
								if (rr == GlobalAgentBase.CheckReleaseResult.Timeout) {
									haltOnReleaseTimeout(r.globalIndex);
								}
								if (rr != GlobalAgentBase.CheckReleaseResult.Releasing) {
									logger.warn("ProcessDaemon.startRelease ServerDaemonTimeout={}", config.serverDaemonTimeout);
									agent.startRelease(zeze, null);
								}
							} catch (Throwable ex) {
								logger.error("ProcessDaemon.handle Release globalIndex={}", r.globalIndex, ex);
							}
							break;
						}
					}

					var now = System.currentTimeMillis();
					for (int i = 0; i < agents.length; i++) {
						try {
							var agent = agents[i];
							var config = agent.getConfig();

							// 此调用也收尾已完成的Release，不能仅在收到Release命令时执行。
							var rr = agent.checkReleaseTimeout(now, config.serverReleaseTimeout);
							if (rr == GlobalAgentBase.CheckReleaseResult.Timeout)
								haltOnReleaseTimeout(i);
							var idle = now - agent.getActiveTime();
							if (idle > config.serverKeepAliveIdleTimeout) {
								agent.keepAlive();
							}

							// 外部Daemon失联时，本地仍须按时启动释放。
							if (idle > config.serverDaemonTimeout && !Reflect.inDebugMode) {
								if (rr != GlobalAgentBase.CheckReleaseResult.Releasing) {
									logger.warn("ProcessDaemon.local fallback startRelease, daemon unreachable. "
											+ "idle={} > serverDaemonTimeout={}", idle, config.serverDaemonTimeout);
									agent.startRelease(zeze, null);
								}
							}
						} catch (Throwable ex) {
							logger.error("ProcessDaemon.keepAlive globalIndex={}", i, ex);
						}
					}
				}
			} catch (Throwable ex) {
				logger.fatal("ProcessDaemon.AchillesHeelDaemon ", ex);
				haltAfterCheckpoint(321321);
			}
		}

		private void haltAfterCheckpoint(int exitCode) {
			Application.haltAfterCheckpoint(zeze, exitCode);
		}

		private void haltOnReleaseTimeout(int globalIndex) {
			logger.fatal("ProcessDaemon.AchillesHeelDaemon global release timeout. index={}", globalIndex);
			haltAfterCheckpoint(123123);
		}

		public void stopAndJoin() {
			markShutdownUnregistered();
			running = false;
			try {
				join();
			} catch (Exception e) {
				logger.error("ProcessDaemon.join", e);
			}
			try {
				channel.close();
			} catch (Exception e) {
				logger.error("Channel.close", e);
			}
			try {
				raf.close();
			} catch (Exception e) {
				logger.error("File.close", e);
			}
			try {
				udpSocket.close();
			} catch (Exception e) {
				logger.error("UdpSocket.close", e);
			}
			try {
				Files.delete(Path.of(fileName));
			} catch (Exception ignored) {
			}
		}
	}

	private final class ThreadDaemon extends Thread {
		private volatile boolean running = true;

		public ThreadDaemon() {
			super("AchillesHeelDaemon");
			setDaemon(true);
		}

		@Override
		public void run() {
			while (running) {
				try {
					var now = System.currentTimeMillis();
					for (int i = 0; i < agents.length; i++) {
						var agent = agents[i];
						var config = agent.getConfig();
						var rr = agent.checkReleaseTimeout(now, config.serverReleaseTimeout);
						if (rr == GlobalAgentBase.CheckReleaseResult.Timeout) {
							logger.fatal("global release timeout. index={}", i);

							Application.haltAfterCheckpoint(zeze, 123123);
						}

						var idle = now - agent.getActiveTime();
						if (idle > config.serverKeepAliveIdleTimeout) {
							agent.keepAlive();
						}

						if (idle > config.serverDaemonTimeout && !Reflect.inDebugMode) {
							if (rr != GlobalAgentBase.CheckReleaseResult.Releasing) {
								logger.warn("startRelease ServerDaemonTimeout={}", config.serverDaemonTimeout);
								agent.startRelease(zeze, null);
							}
						}
					}
				} catch (Throwable ex) {
					// 单轮异常继续巡检；释放超时仍由上面的fatal出口处理。
					logger.error("AchillesHeelDaemon loop exception", ex);
				}
				try {
					//noinspection BusyWait
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					logger.warn("sleep", e);
				}
			}
		}

		public void stopAndJoin() {
			running = false;
			try {
				join();
			} catch (InterruptedException e) {
				logger.error("ThreadDaemon.join", e);
			}
		}
	}
}
