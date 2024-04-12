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
import Zeze.Util.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Daemon {
	public static final String propertyNamePort = "Zeze.ProcessDaemon.Port";
	public static final String propertyNameClearInUse = "Zeze.Database.ClearInUse";
	private static final Logger logger = LogManager.getLogger(Daemon.class);

	// Key Is ServerId。每个Server对应一个Monitor。
	// 正常使用是一个Daemon对应一个Server。
	// 写成支持多个Server是为了跑Simulate测试。
	private static final LongConcurrentHashMap<Monitor> monitors = new LongConcurrentHashMap<>();
	private static DatagramSocket udpSocket;
	private static Process subprocess;

	private static final LongConcurrentHashMap<PendingPacket> pendings = new LongConcurrentHashMap<>();
	private static final FastLock pendingsLock = new FastLock();
	private static volatile Future<?> timer;

	public static long getLongProperty(String name, long def) {
		var p = System.getProperty(name);
		if (null == p || p.isBlank())
			return def;
		return Long.parseLong(p);
	}

	public static void main(String[] args) throws Exception {
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
				if (exitCode == 0)
					break;
				joinMonitors();
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
				try {
					var cmd = receiveCommand(udpSocket);
					switch (cmd.command()) {
					case Register.Command:
						var reg = (Register)cmd;
						var code = 0;
						if (monitors.containsKey(reg.serverId))
							code = 1;
						else {
							var monitor = new Monitor(reg);
							monitors.put(reg.serverId, monitor);
							monitor.start();
						}
						sendCommand(udpSocket, cmd.peer, new CommonResult(reg.reliableSerialNo, code));
						logger.info("Register! Server={} code={}", reg.serverId, code);
						break;

					case GlobalOn.Command:
						var on = (GlobalOn)cmd;
						code = 0;
						var monitor = monitors.get(on.serverId);
						if (monitor != null) {
							monitor.setConfig(on.globalIndex, on.globalConfig);
							logger.info("GlobalOn! Server={} ServerDaemonTimeout={} ServerReleaseTimeout={}",
									on.serverId, on.globalConfig.serverDaemonTimeout, on.globalConfig.serverReleaseTimeout);
						} else {
							logger.warn("GlobalOn! not found serverId={} ServerDaemonTimeout={} ServerReleaseTimeout={}",
									on.serverId, on.globalConfig.serverDaemonTimeout, on.globalConfig.serverReleaseTimeout);
							code = 1;
						}
						sendCommand(udpSocket, cmd.peer, new CommonResult(on.reliableSerialNo, code));
						break;

					case DeadlockReport.Command:
						logger.warn("deadlock report");
						destroySubprocess();
						break;
					}
				} catch (SocketTimeoutException ex) {
					// skip
				}
				if (subprocess.waitFor(0, TimeUnit.MILLISECONDS))
					return subprocess.exitValue();
			} catch (Throwable ex) { // print stacktrace.
				logger.fatal("Daemon.mainRun", ex);
				fatalExit();
				return -1; // never run here
			}
		}
	}

	private static void fatalExit() {
		subprocess.destroy();
		LogManager.shutdown();
		Runtime.getRuntime().halt(-1);
	}

	private static void joinMonitors() throws InterruptedException {
		for (var monitor : monitors)
			monitor.stopAndJoin();
		monitors.clear();
	}

	private static void destroySubprocess() throws InterruptedException {
		// run jstack
		try {
			var pid = String.valueOf(subprocess.pid());
			var cmd = new String[]{"jstack", "-e", "-l", pid};
			var process = Runtime.getRuntime().exec(cmd);
			Files.copy(new BufferedInputStream(process.getInputStream()), Path.of("jstack." + pid));
			process.destroy();
		} catch (Exception ex) {
			logger.error("", ex);
		}
		subprocess.destroy();
		subprocess = null;
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
						timer = Task.scheduleUnsafe(1000, 1000, () -> {
							var now = System.currentTimeMillis();
							for (var pending : pendings) {
								if (now - pending.sendTime > 1000) {
									pending.sendTime = now;
									pending.socket.send(pending.packet);
								}
							}
						});
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
			raf = new RandomAccessFile(new File(fileName), "rw");
			channel = raf.getChannel();
			mmap = channel.map(FileChannel.MapMode.READ_WRITE, 0, channel.size());
		}

		public AchillesHeelConfig getConfig(int index) {
			return globalConfigs.get(index);
		}

		public void setConfig(int index, AchillesHeelConfig config) {
			globalConfigs.set(index, config);
		}

		private ByteBuffer copyMMap() throws IOException {
			channelLock.lock();
			try {
				// Channel.lock 对同一个进程不能并发。
				var lock = channel.lock();
				try {
					var copy = new byte[globalConfigs.length() * 8];
					mmap.position(0);
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
