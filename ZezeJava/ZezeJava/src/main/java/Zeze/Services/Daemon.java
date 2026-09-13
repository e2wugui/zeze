package Zeze.Services;

import java.io.BufferedInputStream;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Daemon {
	public static final String propertyNamePort = "Zeze.ProcessDaemon.Port";
	public static final String propertyNameClearInUse = "Zeze.Database.ClearInUse";
	private static final @NotNull Logger logger = LogManager.getLogger(Daemon.class);

	// Key Is ServerId。每个Server对应一个Monitor。
	// 正常使用是一个Daemon对应一个Server。
	// 写成支持多个Server是为了跑Simulate测试。
	private static final LongConcurrentHashMap<Monitor> monitors = new LongConcurrentHashMap<>();
	private static DatagramSocket udpSocket;
	private static volatile @Nullable Process subprocess;

	private static final LongConcurrentHashMap<PendingPacket> pendings = new LongConcurrentHashMap<>();
	private static final FastLock pendingsLock = new FastLock();
	private static volatile @Nullable Future<?> timer;

	public static long getLongProperty(String name, long def) {
		var p = System.getProperty(name);
		return p != null && !p.isBlank() ? Long.parseLong(p) : def;
	}

	public static void main(String[] args) throws Exception {
		// 参数契约入口显式化（FND4-72）：args为空时下方command.add(1,...)越界，以意外
		// IndexOutOfBoundsException而非usage提示失败。无参启动直接给出用法。
		if (args.length < 1) {
			// FND5-38：args[0]是可执行程序（如java），主类在其后——曾印"<main-class>"，
			// 按usage启动必然IOException失败，守护从未生效。
			System.err.println("usage: Daemon <java-executable> [jvm-args ...] <main-class> [args...]");
			return;
		}
		// udp for subprocess register
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
				// 正常退出也必须停止Monitor：Monitor是非daemon线程，不join会令JVM挂死
				// （GlobalOn未到达时超时判定被跳过，Monitor纯空转，永不退出）；
				// stopAndJoin顺带关闭mmap临时文件句柄。
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
		} catch (Throwable ex) { // print stacktrace.
			logger.error("Daemon.main", ex);
		} finally {
			// 退出的时候，确保销毁服务进程。
			if (subprocess != null)
				subprocess.destroy();
		}
	}

	private static int mainRun() {
		while (true) {
			try {
				// 轮询：等待Global配置以及等待子进程退出。
				Command cmd = null;
				try {
					cmd = receiveCommand(udpSocket);
				} catch (SocketTimeoutException ex) {
					// skip
				} catch (Throwable ex) {
					// 收到未知/截断/损坏的UDP报文（本地任意进程可向该随机端口发送，FND5-37，
					// 对齐ProcessDaemon判例）：丢弃并继续，非信任输入不得触发外层catch的
					// fatalExit杀掉看门狗与被监管子进程。
					logger.error("Daemon.receiveCommand bad packet", ex);
				}
				if (cmd != null) {
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
								// Monitor打开mmap失败（校验后文件被删/被锁等）：拒绝本次注册，
								// 不让异常逃逸到外层catch的fatalExit。
								logger.error("Register monitor failed", ex);
								code = 2;
							}
						}
						sendCommand(udpSocket, cmd.peer, new CommonResult(reg.reliableSerialNo, code));
						logger.info("Register! Server={} code={}", reg.serverId, code);
						break;

					case GlobalOn.Command:
						var on = (GlobalOn)cmd;
						code = 0;
						var monitor = monitors.get(on.serverId);
						if (monitor != null) {
							if (on.globalIndex < 0 || on.globalIndex >= monitor.globalConfigs.length()) {
								// 非信任输入边界校验（FND5-37，对齐ProcessDaemon.Release判例）：
								// 越界索引丢弃，AIOOBE不得逃逸到外层catch的fatalExit。
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
						sendCommand(udpSocket, cmd.peer, new CommonResult(on.reliableSerialNo, code));
						break;

					case DeadlockReport.Command:
						// 弱校验（FND5-37复审）：DeadlockReport不带任何字段，子进程侧与Register
						// 走同一个udpSocket（对端地址相同），仅接受已注册Monitor对端的报告——
						// 任意本地进程伪造报文不得销毁被监管子进程并连带令守护整体退出。
						if (!isRegisteredPeer(cmd.peer)) {
							logger.error("DeadlockReport rejected: peer={}", cmd.peer);
							break;
						}
						logger.warn("deadlock report");
						destroySubprocess();
						break;
					}
				}
				// subprocess 可能已被Monitor（idle超时）或DeadlockReport路径销毁并置null，此时返回非0让main重启子进程。
				if (subprocess == null || subprocess.waitFor(0, TimeUnit.MILLISECONDS))
					return subprocess != null ? subprocess.exitValue() : 1;
			} catch (Throwable ex) { // print stacktrace.
				logger.fatal("Daemon.mainRun", ex);
				fatalExit();
				return -1; // never run here
			}
		}
	}

	// Register 校验（FND5-37）：mmap文件由子进程createTempFile("zeze",".mmap")创建，
	// 限定临时目录+前缀后缀白名单；copyMMap按globalCount*8布局读活跃时间戳，尺寸必须
	// 吻合；globalCount上界防伪造巨值OOM（实际=进程内GCM实例数，个位数）。不满足即拒绝，
	// 杜绝任意路径打开/任意尺寸映射及stopAndJoin的任意路径删除。
	// 整体catch（FND5-37复审）：Path.of对Windows非法路径字符抛InvalidPathException（合法
	// 编码的Register报文即可携带），不包则逃逸到mainRun外层catch的fatalExit——单报文
	// 仍可halt看门狗。非信任输入的解析异常一律按拒绝处理。
	private static boolean isValidRegister(@NotNull Register reg) {
		try {
			if (reg.globalCount <= 0 || reg.globalCount > 1024)
				return false;
			var fileName = Path.of(reg.mmapFileName);
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

	private static boolean isRegisteredPeer(@NotNull SocketAddress peer) {
		for (var monitor : monitors) {
			if (monitor.peerSocketAddress.equals(peer))
				return true;
		}
		return false;
	}

	private static void fatalExit() {
		if (subprocess != null)
			subprocess.destroy();
		LogManager.shutdown();
		Runtime.getRuntime().halt(-1);
	}

	private static void joinMonitors() throws InterruptedException {
		for (var monitor : monitors)
			monitor.stopAndJoin();
		monitors.clear();
	}

	// 锁职责=销毁仲裁：临界区内只做"读-置空"两步，保证同一时刻仅一个线程拿到Process
	// 去执行销毁（含占坑的读-置空串行化，多GCM同轮超时重复进入、DeadlockReport与Monitor
	// 跨线程并发时，后来者拿到null幂等返回）。
	// jstack采样/destroy/joinMonitors全部在锁外：曾经把join留在锁内，被join的Monitor
	// 若正阻塞在本锁的monitorenter上，形成"持锁者join等锁者"的循环死锁，看门狗整体
	// 冻结且不可自愈（FND4-67）。现在Monitor等锁者很快拿到锁、发现null返回，再由
	// stopAndJoin置running=false退出循环，join必然返回。
	private static void destroySubprocess() throws InterruptedException {
		Process p;
		synchronized (Daemon.class) {
			p = subprocess;
			subprocess = null;
		}
		if (p == null)
			return;
		// run jstack
		try {
			var pid = String.valueOf(p.pid());
			var cmd = new String[]{"jstack", "-e", "-l", pid};
			// 合并stderr避免缓冲区填满阻塞子进程；显式关闭输入流；限时等待退出，超时强杀。
			var process = new ProcessBuilder(cmd).redirectErrorStream(true).start();
			try (var input = new BufferedInputStream(process.getInputStream())) {
				Files.copy(input, Path.of("jstack." + pid));
			}
			if (!process.waitFor(30, TimeUnit.SECONDS))
				process.destroyForcibly();
		} catch (Exception ex) {
			logger.error("", ex);
		}
		p.destroy();
		joinMonitors();
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

			// auto start Timer
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
										// socket已关闭（如调用方收尾关闭sender）：重发永不可能成功，
										// 摘除该pending。否则本定时器每秒对死socket send抛异常，
										// 每秒刷一条error日志直到JVM退出。
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
		if (cmd.reliableSerialNo != 0)
			pendings.remove(cmd.reliableSerialNo);
		return cmd;
	}

	private static class Monitor extends Thread {
		private final SocketAddress peerSocketAddress;
		private final AtomicReferenceArray<AchillesHeelConfig> globalConfigs;
		private final String fileName;
		private final RandomAccessFile raf;
		private final FileChannel channel;
		private final FastLock channelLock = new FastLock();
		private final MappedByteBuffer mmap;
		private volatile boolean running = true;

		public Monitor(Register reg) throws Exception {
			peerSocketAddress = reg.peer;
			globalConfigs = new AtomicReferenceArray<>(reg.globalCount);
			fileName = reg.mmapFileName;
			var tmpRaf = new RandomAccessFile(new File(fileName), "rw");
			try {
				var tmpChannel = tmpRaf.getChannel();
				// 构造期复核尺寸（FND5-37复审）：isValidRegister校验与本构造之间存在TOCTOU窗口，
				// 文件被截断/替换后按当下channel.size()映射，容量!=globalCount*8会使copyMMap
				// 稳定抛BufferUnderflowException逃逸到run的fatalExit。此处不匹配直接拒绝注册
				// （由mainRun的catch转为code=2应答）。
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

		public AchillesHeelConfig getConfig(int index) {
			return globalConfigs.get(index);
		}

		public void setConfig(int index, AchillesHeelConfig config) {
			globalConfigs.set(index, config);
		}

		// 返回null表示本轮防御性丢弃（映射可读字节不足，运行期文件被外部篡改的兜底），
		// 调用方跳过本轮即可，下轮重试；不得让BufferUnderflowException逃逸到run的fatalExit。
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
							continue; // skip not ready global

						var idle = now - activeTime;
						if (idle > config.serverReleaseTimeout) {
							logger.info("destroySubprocess {} - {} > {}", now, activeTime, config.serverReleaseTimeout);
							destroySubprocess();
							// daemon main will restart subprocess!
						} else if (idle > config.serverDaemonTimeout) {
							logger.info("sendCommand Release-{} {} - {} > {}", i, now, activeTime, config.serverDaemonTimeout);
							// 在Server执行Release期间，命令可能重复发送。
							// 重复命令的处理由Server完成，
							// 这里重发也是需要的，刚好解决Udp不可靠性。
							sendCommand(udpSocket, peerSocketAddress, new Release(i));
						}
						//noinspection BusyWait
						Thread.sleep(1000);
					}
				}
			} catch (Throwable ex) { // print stacktrace.
				logger.fatal("Monitor.run", ex);
				fatalExit();
			}
		}

		public void stopAndJoin() throws InterruptedException {
			running = false;
			if (Thread.currentThread() != this) // join自己会永久等待；Monitor.run会经由destroySubprocess->joinMonitors间接调用到这里
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
				Files.delete(Path.of(fileName)); // try delete
			} catch (Exception ignored) {
			}
		}
	}

	public static abstract class Command implements Serializable {
		private static final AtomicLong seed = new AtomicLong();

		public SocketAddress peer;
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

	// 精简版本配置。仅传递Daemon需要的参数过来。
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
		public String mmapFileName;

		public Register(int serverId, int c, String name) {
			this.serverId = serverId;
			globalCount = c;
			mmapFileName = name;
			setReliableSerialNo(); // enable reliable
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
			setReliableSerialNo(); // enable reliable
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
