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

/**
 * Server 与 GlobalCacheManager 失联时的记录锁安全守护：周期保活、超时触发本地锁释放（Releaser），
 * 并在超过释放预算前走致命保存退出，保证先于 GlobalDaemonTimeout 放弃锁权（支持线程内与外部进程两种模式）。
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

							// 外部Daemon失联时，本地仍须按时启动释放；优先级高于保活，
							// 避免 keepAlive 异常（如 Raft pending 满）阻断 startRelease。
							if (idle > config.serverDaemonTimeout && !Reflect.inDebugMode) {
								if (rr != GlobalAgentBase.CheckReleaseResult.Releasing) {
									logger.warn("ProcessDaemon.local fallback startRelease, daemon unreachable. "
											+ "idle={} > serverDaemonTimeout={}", idle, config.serverDaemonTimeout);
									agent.startRelease(zeze, null);
								}
							}

							if (idle > config.serverKeepAliveIdleTimeout) {
								try {
									agent.keepAlive();
								} catch (Throwable ex) {
									logger.error("ProcessDaemon.keepAlive globalIndex={}", i, ex);
								}
							}
						} catch (Throwable ex) {
							logger.error("ProcessDaemon.check globalIndex={}", i, ex);
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
				var now = System.currentTimeMillis();
				for (int i = 0; i < agents.length; i++) {
					try {
						var agent = agents[i];
						var config = agent.getConfig();
						var rr = agent.checkReleaseTimeout(now, config.serverReleaseTimeout);
						if (rr == GlobalAgentBase.CheckReleaseResult.Timeout) {
							logger.fatal("global release timeout. index={}", i);

							Application.haltAfterCheckpoint(zeze, 123123);
						}

						var idle = now - agent.getActiveTime();
						if (idle > config.serverDaemonTimeout && !Reflect.inDebugMode) {
							if (rr != GlobalAgentBase.CheckReleaseResult.Releasing) {
								logger.warn("startRelease ServerDaemonTimeout={}", config.serverDaemonTimeout);
								agent.startRelease(zeze, null);
							}
						}

						if (idle > config.serverKeepAliveIdleTimeout) {
							try {
								agent.keepAlive();
							} catch (Throwable ex) {
								logger.error("AchillesHeelDaemon keepAlive exception. index={}", i, ex);
							}
						}
					} catch (Throwable ex) {
						// 单 agent 异常隔离并继续巡检其余 agent；释放超时仍由上面的 fatal 出口处理。
						logger.error("AchillesHeelDaemon loop exception. index={}", i, ex);
					}
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
