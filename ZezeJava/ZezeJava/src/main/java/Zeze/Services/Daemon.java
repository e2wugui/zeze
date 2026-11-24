package Zeze.Services;

import java.util.Objects;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReferenceArray;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.Serializable;
import Zeze.Util.FastLock;
import Zeze.Util.LongConcurrentHashMap;
import Zeze.Util.ShutdownHook;
import Zeze.Util.TaskSpec;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

/**
 * 看门狗守护进程：监管一个子进程（游戏服务器），经UDP接收子进程注册，按mmap中的
 * 活跃时间戳超时发送Release或销毁子进程并重启；收到死锁报告立即销毁重启。
 */
public class Daemon {
	public static final String propertyNamePort = "Zeze.ProcessDaemon.Port";
	public static final String propertyNameClearInUse = "Zeze.Database.ClearInUse";
	private static final Logger logger = LogManager.getLogger(Daemon.class);

	// 按 serverId 记录 Monitor，支持一个 Daemon 监管多个 Server。
	private static final LongConcurrentHashMap<Monitor> monitors = new LongConcurrentHashMap<>();
	private static @Nullable DatagramSocket udpSocket;
	private static volatile @Nullable Process subprocess;

	private static final LongConcurrentHashMap<PendingPacket> pendings = new LongConcurrentHashMap<>();
	private static final FastLock pendingsLock = new FastLock();
	private static volatile @Nullable Future<?> timer;

	public static long getLongProperty(String name, long def) {
		var p = System.getProperty(name);
		return p != null && !p.isBlank() ? Long.parseLong(p) : def;
	}

	public static void main(String[] args) throws Exception {
		if (args.length < 1) {
			System.err.println("usage: Daemon <java-executable> [jvm-args ...] <main-class> [args...]");
			return;
		}
		udpSocket = new DatagramSocket(0, InetAddress.getLoopbackAddress());
		udpSocket.setSoTimeout(200);
		var minAliveTime = getLongProperty("MinAliveTime", 30 * 60 * 1000);

		try {
			var restart = false;
			while (true) {
				var command = new ArrayList<String>();
				Collections.addAll(command, args);
				if (restart)
					command.add(1, "-D" + propertyNameClearInUse + "=true");
				command.add(1, "-D" + propertyNamePort + "=" + udpSocket.getLocalPort());

				var pb = new ProcessBuilder(command);
				pb.inheritIO();

				subprocess = pb.start();
				var startTime = System.currentTimeMillis();
				var exitCode = mainRun();
				// Monitor 是非 daemon 线程，正常退出也须停止并回收 mmap 资源。
				joinMonitors();
				if (exitCode == 0)
					break;
				logger.warn("Subprocess Restart! ExitCode={}", exitCode);
				if (System.currentTimeMillis() - startTime < minAliveTime) {
					logger.fatal("subprocess alive too short: {}", minAliveTime);
					break;
				}
				restart = true;
			}
		} catch (Throwable ex) {
			logger.error("Daemon.main", ex);
		} finally {
			// 守护进程退出前限时等待子进程，超时强杀。
			var subprocess = Daemon.subprocess;
			if (subprocess != null) {
				subprocess.destroy();
				reapDiagnosticProcess(subprocess);
			}
		}
	}

	private static int mainRun() {
		while (true) {
			try {
				// 轮询：等待Global配置以及等待子进程退出。
				Command cmd = null;
				try {
					cmd = receiveCommand(Objects.requireNonNull(udpSocket));
				} catch (SocketTimeoutException ex) {
					// 超时后检查子进程状态。
				} catch (Throwable ex) {
					// 坏报文仅丢弃，避免输入错误终止守护进程。
					logger.error("Daemon.receiveCommand bad packet", ex);
				}
				if (cmd != null) {
					// 非注册命令仅接受已注册对端；Register 无凭据，此校验不能防止抢先注册。
					if (cmd.command() != Register.Command && !isRegisteredPeer(Objects.requireNonNull(cmd.peer))) {
						logger.error("Command {} rejected: unregistered peer={}", cmd.command(), cmd.peer);
					} else
					switch (cmd.command()) {
					case Register.Command:
						var reg = (Register)cmd;
						var code = 0;
						if (!isValidRegister(reg)) {
							logger.error("Register rejected: serverId={} globalCount={} mmap={}",
									reg.serverId, reg.globalCount, reg.mmapFileName);
							code = 2;
						} else if (monitors.containsKey(reg.serverId))
							code = 1;
						else {
							try {
								var monitor = new Monitor(reg);
								monitors.put(reg.serverId, monitor);
								monitor.start();
							} catch (Throwable ex) {
								// mmap 可能在校验后失效，拒绝注册并保持守护循环。
								logger.error("Register monitor failed", ex);
								code = 2;
							}
						}
						sendCommand(udpSocket, Objects.requireNonNull(cmd.peer), new CommonResult(reg.reliableSerialNo, code));
						logger.info("Register! Server={} code={}", reg.serverId, code);
						break;

					case GlobalOn.Command:
						var on = (GlobalOn)cmd;
						code = 0;
						var monitor = monitors.get(on.serverId);
						if (monitor != null && !monitor.peerSocketAddress.equals(on.peer)) {
							// 配置须来自对应 serverId 的注册对端，防止跨 Server 改写。
							logger.error("GlobalOn rejected: serverId={} peer={} registered={}",
									on.serverId, on.peer, monitor.peerSocketAddress);
							code = 1;
						} else if (monitor != null) {
							if (on.globalIndex < 0 || on.globalIndex >= monitor.globalConfigs.length()) {
								logger.error("GlobalOn bad globalIndex={}, count={}",
										on.globalIndex, monitor.globalConfigs.length());
								code = 2;
							} else {
								monitor.setConfig(on.globalIndex, on.globalConfig);
								logger.info("GlobalOn! Server={} ServerDaemonTimeout={} ServerReleaseTimeout={}",
										on.serverId, on.globalConfig.serverDaemonTimeout, on.globalConfig.serverReleaseTimeout);
							}
						} else {
							logger.warn("GlobalOn! not found serverId={} ServerDaemonTimeout={} ServerReleaseTimeout={}",
									on.serverId, on.globalConfig.serverDaemonTimeout, on.globalConfig.serverReleaseTimeout);
							code = 1;
						}
						sendCommand(udpSocket, Objects.requireNonNull(cmd.peer), new CommonResult(on.reliableSerialNo, code));
						break;

					case DeadlockReport.Command:
						if (!isRegisteredPeer(Objects.requireNonNull(cmd.peer))) {
							logger.error("DeadlockReport rejected: peer={}", cmd.peer);
							break;
						}
						logger.warn("deadlock report");
						destroySubprocess();
						break;
					}
				}
				// Monitor 可能已销毁并清空子进程引用，返回非零以触发重启。
				var subprocess = Daemon.subprocess;
				if (subprocess == null || subprocess.waitFor(0, TimeUnit.MILLISECONDS))
					return subprocess != null ? subprocess.exitValue() : 1;
			} catch (Throwable ex) {
				logger.fatal("Daemon.mainRun", ex);
				fatalExit();
				return -1;
			}
		}
	}

	// 注册文件须为临时目录下 zeze*.mmap，且尺寸为 globalCount * 8；退出时会删除。
	// 限制 globalCount 防止过量映射，路径/文件校验异常均拒绝注册。
	private static boolean isValidRegister(Register reg) {
		try {
			if (reg.globalCount <= 0 || reg.globalCount > 1024)
				return false;
			var fileName = Path.of(Objects.requireNonNull(reg.mmapFileName));
			if (!Files.isRegularFile(fileName))
				return false;
			var parent = fileName.getParent();
			if (parent == null || !parent.equals(Path.of(System.getProperty("java.io.tmpdir"))))
				return false;
			var name = fileName.getFileName().toString();
			if (!name.startsWith("zeze") || !name.endsWith(".mmap"))
				return false;
			return Files.size(fileName) == (long)reg.globalCount * 8;
		} catch (RuntimeException | IOException e) {
			return false;
		}
	}

	private static boolean isRegisteredPeer(SocketAddress peer) {
		for (var monitor : monitors) {
			if (monitor.peerSocketAddress.equals(peer))
				return true;
		}
		return false;
	}

	private static void fatalExit() {
		var subprocess = Daemon.subprocess;
		if (subprocess != null) {
			subprocess.destroy();
			reapDiagnosticProcess(subprocess);
		}
		LogManager.shutdown();
		Runtime.getRuntime().halt(-1);
	}

	private static void joinMonitors() throws InterruptedException {
		for (var monitor : monitors)
			monitor.stopAndJoin();
		monitors.clear();
	}

	// Daemon.class 仅仲裁销毁，进程确认终止后才清空引用，防止提前重启。
	// 诊断、销毁和 join 须在锁外执行，避免 join 等待正在争用此锁的 Monitor。
	private static void destroySubprocess() throws InterruptedException {
		destroySubprocess(false);
	}

	private static void destroySubprocess(boolean authorityExpired) throws InterruptedException {
		Process p;
		boolean alreadyDestroying;
		synchronized (Daemon.class) {
			p = subprocess;
			if (p == null)
				return;
			alreadyDestroying = destroyingSubprocess;
			destroyingSubprocess = true;
		}
		if (alreadyDestroying) {
			// 普通销毁可能仍在采集诊断或等待优雅退出。权限到期必须立即强杀，
			// 原销毁调用继续负责确认退出和清理子进程引用。
			if (authorityExpired)
				p.destroyForcibly();
			return;
		}
		// 权限过期后直接强杀，诊断和优雅退出不得延长保存窗口。
		if (!authorityExpired) try {
			var pid = String.valueOf(p.pid());
			var cmd = new String[]{"jstack", "-e", "-l", pid};
			captureDiagnostic(cmd, Path.of("jstack." + pid), 30_000);
		} catch (Exception ex) {
			logger.error("", ex);
		}
		try {
			if (authorityExpired)
				p.destroyForcibly();
			else
				p.destroy();
			if (!p.waitFor(30, TimeUnit.SECONDS)) {
				p.destroyForcibly();
				if (!p.waitFor(30, TimeUnit.SECONDS))
					throw new IllegalStateException("subprocess did not terminate: " + p.pid());
			}
			synchronized (Daemon.class) {
				if (subprocess == p)
					subprocess = null;
			}
			// 由主循环统一停止并等待本代 Monitor。销毁可能由 Monitor 发起，
			// 此处等待其他 Monitor 会与主循环并发清理同一批资源。
		} finally {
			synchronized (Daemon.class) {
				destroyingSubprocess = false;
			}
		}
	}

	private static boolean destroyingSubprocess; // Daemon.class守护

	static void captureDiagnostic(String[] command, Path output, long timeoutMillis) throws IOException {
		// 诊断输出直接写文件，避免等待管道 EOF 阻塞超时收尾。
		var process = new ProcessBuilder(command).redirectErrorStream(true)
				.redirectOutput(output.toFile()).start();
		reapDiagnosticProcess(process, timeoutMillis);
	}

	/** 限时等待诊断进程，超时或中断时强杀，保留中断标志。 */
	private static void reapDiagnosticProcess(Process process) {
		reapDiagnosticProcess(process, 30_000);
	}

	private static void reapDiagnosticProcess(Process process, long timeoutMillis) {
		try {
			if (!process.waitFor(timeoutMillis, TimeUnit.MILLISECONDS))
				process.destroyForcibly();
		} catch (InterruptedException e) {
			process.destroyForcibly();
			Thread.currentThread().interrupt();
		}
	}

	private static final class PendingPacket {
		public final DatagramSocket socket;
		public final DatagramPacket packet;
		public long sendTime = System.currentTimeMillis();

		public PendingPacket(DatagramSocket socket, DatagramPacket packet) {
			this.socket = socket;
			this.packet = packet;
		}
	}

	public static void sendCommand(DatagramSocket socket, SocketAddress peer, Command cmd) throws IOException {
		var bb = ByteBuffer.Allocate(5);
		bb.WriteInt(cmd.command());
		cmd.encode(bb);
		var p = new DatagramPacket(bb.Bytes, 0, bb.WriteIndex, peer);
		if (cmd.isRequest()) {
			if (pendings.putIfAbsent(cmd.reliableSerialNo, new PendingPacket(socket, p)) != null)
				throw new IllegalStateException("Duplicate ReliableSerialNo=" + cmd.reliableSerialNo);

			if (timer == null) {
				pendingsLock.lock();
				try {
					if (timer == null) {
						timer = TaskSpec.ofAction(() -> {
							var now = System.currentTimeMillis();
							for (var it = pendings.entryIterator(); it.moveToNext(); ) {
								var pending = it.value();
								if (now - pending.sendTime > 1000) {
									pending.sendTime = now;
									try {
										pending.socket.send(pending.packet);
									} catch (IOException e) {
									// 发送失败后移除待重发项，避免对已关闭 socket 无限重试。
										pendings.remove(it.key());
									}
								}
							}
						}).schedulePeriodNow(1000, 1000);
						//noinspection DataFlowIssue
						ShutdownHook.add(() -> timer.cancel(false));
					}
				} finally {
					pendingsLock.unlock();
				}
			}
		}
		socket.send(p);
	}

	public static Command receiveCommand(DatagramSocket socket) throws IOException {
		var buf = new byte[1024];
		var p = new DatagramPacket(buf, buf.length);
		socket.receive(p);
		var bb = ByteBuffer.Wrap(buf, p.getLength());
		var c = bb.ReadInt();
		Command cmd;
		//noinspection EnhancedSwitchMigration
		switch (c) {
		case Register.Command:
			cmd = new Register(bb, p.getSocketAddress());
			break;
		case CommonResult.Command:
			cmd = new CommonResult(bb, p.getSocketAddress());
			break;
		case GlobalOn.Command:
			cmd = new GlobalOn(bb, p.getSocketAddress());
			break;
		case Release.Command:
			cmd = new Release(bb, p.getSocketAddress());
			break;
		case DeadlockReport.Command:
			cmd = new DeadlockReport(bb, p.getSocketAddress());
			break;
		default:
			throw new UnsupportedOperationException("Unknown Command =" + c);
		}
		if (cmd instanceof CommonResult && cmd.reliableSerialNo != 0) {
			var pending = pendings.get(cmd.reliableSerialNo);
			if (pending != null && pending.socket == socket
					&& pending.packet.getSocketAddress().equals(cmd.peer))
				pendings.remove(cmd.reliableSerialNo, pending);
		}
		return cmd;
	}

	private static class Monitor extends Thread {
		private final SocketAddress peerSocketAddress;
		private final AtomicReferenceArray<@Nullable AchillesHeelConfig> globalConfigs;
		private final String fileName;
		private final RandomAccessFile raf;
		private final FileChannel channel;
		private final FastLock channelLock = new FastLock();
		private final MappedByteBuffer mmap;
		private volatile boolean running = true;

		public Monitor(Register reg) throws Exception {
			peerSocketAddress = Objects.requireNonNull(reg.peer);
			globalConfigs = new AtomicReferenceArray<>(reg.globalCount);
			fileName = Objects.requireNonNull(reg.mmapFileName);
			var tmpRaf = new RandomAccessFile(new File(Objects.requireNonNull(fileName)), "rw");
			try {
				var tmpChannel = tmpRaf.getChannel();
				// 校验与映射间文件可能变化，构造时须再次核对尺寸。
				var size = tmpChannel.size();
				if (size != (long)reg.globalCount * 8)
					throw new IOException("mmap size mismatch: " + size + " != " + ((long)reg.globalCount * 8));
				mmap = tmpChannel.map(FileChannel.MapMode.READ_WRITE, 0, size);
				raf = tmpRaf;
				channel = tmpChannel;
			} catch (Exception e) {
				try {
					tmpRaf.close(); // 连带关闭channel
				} catch (Exception ignored) {
				}
				throw e;
			}
		}

		public @Nullable AchillesHeelConfig getConfig(int index) {
			return globalConfigs.get(index);
		}

		public void setConfig(int index, AchillesHeelConfig config) {
			globalConfigs.set(index, config);
		}

		// 映射容量不足时返回 null，跳过本轮并重试。
		private @Nullable ByteBuffer copyMMap() throws IOException {
			channelLock.lock();
			try {
				// Channel.lock 对同一个进程不能并发。
				var lock = channel.lock();
				try {
					var copy = new byte[globalConfigs.length() * 8];
					mmap.position(0);
					if (mmap.remaining() < copy.length) {
						logger.error("Monitor.copyMMap truncated: remaining={}, expect={}",
								mmap.remaining(), copy.length);
						return null;
					}
					mmap.get(copy, 0, copy.length);
					return ByteBuffer.Wrap(copy);
				} finally {
					lock.release();
				}
			} finally {
				channelLock.unlock();
			}
		}

		@Override
		public void run() {
			try {
				while (running) {
					var bb = copyMMap();
					if (bb == null) {
						//noinspection BusyWait
						Thread.sleep(1000);
						continue;
					}
					var now = System.currentTimeMillis();
					for (int i = 0; i < globalConfigs.length(); ++i) {
						var activeTime = bb.ReadLong8();
						var config = getConfig(i);
						if (config == null)
							continue;
						if (activeTime < 0)
							// 负时间戳表示已注销；终检点和关库可能较慢，不再触发 Release 或强杀。
							continue;

						var idle = now - activeTime;
						if (idle > config.serverReleaseTimeout) {
							logger.info("destroySubprocess {} - {} > {}", now, activeTime, config.serverReleaseTimeout);
							destroySubprocess(true);
						} else if (idle > config.serverDaemonTimeout) {
							logger.info("sendCommand Release-{} {} - {} > {}", i, now, activeTime, config.serverDaemonTimeout);
							// UDP 可能丢包，逐轮重发 Release，Server 须幂等处理。
							try {
								sendCommand(Objects.requireNonNull(udpSocket), peerSocketAddress, new Release(i));
							} catch (IOException e) {
								// Release 会在下一轮重发，单次发送失败不终止监控。
								logger.warn("sendCommand Release-{} failed, skip this round.", i, e);
							}
						}
					}
					// 每轮扫描只等待一次，避免周期随 GCM 数量增长。
					//noinspection BusyWait
					Thread.sleep(1000);
				}
			} catch (Throwable ex) {
				logger.fatal("Monitor.run", ex);
				fatalExit();
			}
		}

		public void stopAndJoin() throws InterruptedException {
			running = false;
			if (Thread.currentThread() != this) // 销毁路径可能由当前 Monitor 发起，不能 join 自己。
				join();
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
				Files.delete(Path.of(fileName));
			} catch (Exception ignored) {
			}
		}
	}

	public static abstract class Command implements Serializable {
		private static final AtomicLong seed = new AtomicLong();

		public @Nullable SocketAddress peer;
		public long reliableSerialNo;
		private boolean isRequest;

		public abstract int command();

		public boolean isRequest() {
			return isRequest;
		}

		public void setReliableSerialNo() {
			do
				reliableSerialNo = seed.incrementAndGet();
			while (reliableSerialNo == 0);
			isRequest = true;
		}

		@Override
		public void encode(ByteBuffer bb) {
			bb.WriteLong(reliableSerialNo);
		}

		@Override
		public void decode(IByteBuffer bb) {
			reliableSerialNo = bb.ReadLong();
		}
	}

	// 仅传递监控所需的超时参数。
	public static class AchillesHeelConfig implements Serializable {
		public int serverDaemonTimeout;
		public int serverReleaseTimeout;

		@Override
		public void encode(ByteBuffer bb) {
			bb.WriteInt(serverDaemonTimeout);
			bb.WriteInt(serverReleaseTimeout);
		}

		@Override
		public void decode(IByteBuffer bb) {
			serverDaemonTimeout = bb.ReadInt();
			serverReleaseTimeout = bb.ReadInt();
		}
	}

	public static class Register extends Command {
		public static final int Command = 0;

		public int serverId;
		public int globalCount;
		public @Nullable String mmapFileName;

		public Register(int serverId, int c, String name) {
			this.serverId = serverId;
			globalCount = c;
			mmapFileName = name;
			setReliableSerialNo();
		}

		public Register(ByteBuffer bb, SocketAddress peer) {
			this.decode(bb);
			this.peer = peer;
		}

		@Override
		public int command() {
			return Command;
		}

		@Override
		public void encode(ByteBuffer bb) {
			super.encode(bb);
			bb.WriteInt(serverId);
			bb.WriteInt(globalCount);
			bb.WriteString(mmapFileName);
		}

		@Override
		public void decode(IByteBuffer bb) {
			super.decode(bb);
			serverId = bb.ReadInt();
			globalCount = bb.ReadInt();
			mmapFileName = bb.ReadString();
		}
	}

	public static class GlobalOn extends Command {
		public static final int Command = 1;

		public int serverId;
		public int globalIndex;
		public final AchillesHeelConfig globalConfig = new AchillesHeelConfig();

		public GlobalOn(int serverId, int index, int server, int release) {
			this.serverId = serverId;
			globalIndex = index;
			globalConfig.serverDaemonTimeout = server;
			globalConfig.serverReleaseTimeout = release;
			setReliableSerialNo();
		}

		public GlobalOn(ByteBuffer bb, SocketAddress peer) {
			this.decode(bb);
			this.peer = peer;
		}

		@Override
		public int command() {
			return Command;
		}

		@Override
		public void encode(ByteBuffer bb) {
			super.encode(bb);
			bb.WriteInt(serverId);
			bb.WriteInt(globalIndex);
			globalConfig.encode(bb);
		}

		@Override
		public void decode(IByteBuffer bb) {
			super.decode(bb);
			serverId = bb.ReadInt();
			globalIndex = bb.ReadInt();
			globalConfig.decode(bb);
		}
	}

	public static class CommonResult extends Command {
		public static final int Command = 2;

		public int code;

		public CommonResult(long serial, int code) {
			reliableSerialNo = serial;
			this.code = code;
		}

		public CommonResult(ByteBuffer bb, SocketAddress peer) {
			this.decode(bb);
			this.peer = peer;
		}

		@Override
		public int command() {
			return Command;
		}

		@Override
		public void encode(ByteBuffer bb) {
			super.encode(bb);
			bb.WriteInt(code);
		}

		@Override
		public void decode(IByteBuffer bb) {
			super.decode(bb);
			code = bb.ReadInt();
		}
	}

	public static class Release extends Command {
		public static final int Command = 3;

		public int globalIndex;

		public Release(int index) {
			globalIndex = index;
		}

		public Release(ByteBuffer bb, SocketAddress peer) {
			this.decode(bb);
			this.peer = peer;
		}

		@Override
		public int command() {
			return Command;
		}

		@Override
		public void encode(ByteBuffer bb) {
			super.encode(bb);
			bb.WriteInt(globalIndex);
		}

		@Override
		public void decode(IByteBuffer bb) {
			super.decode(bb);
			globalIndex = bb.ReadInt();
		}
	}

	public static class DeadlockReport extends Command {
		public static final int Command = 4;

		public DeadlockReport() {
		}

		public DeadlockReport(ByteBuffer bb, SocketAddress peer) {
			this.decode(bb);
			this.peer = peer;
		}

		@Override
		public int command() {
			return Command;
		}

		@Override
		public void encode(ByteBuffer bb) {
			super.encode(bb);
		}

		@Override
		public void decode(IByteBuffer bb) {
			super.decode(bb);
		}
	}
}
