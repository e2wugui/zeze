package Zeze.Services;

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
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReferenceArray;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.Serializable;
import Zeze.Util.LongConcurrentHashMap;
import Zeze.Util.ShutdownHook;
import Zeze.Util.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Daemon {
	public static final String PropertyNamePort = "Zeze.ProcessDaemon.Port";
	private static final Logger logger = LogManager.getLogger(Daemon.class);

	// Key Is ServerId。每个Server对应一个Monitor。
	// 正常使用是一个Daemon对应一个Server。
	// 写成支持多个Server是为了跑Simulate测试。
	private static final LongConcurrentHashMap<Monitor> Monitors = new LongConcurrentHashMap<>();
	private static DatagramSocket UdpSocket;
	private static Process Subprocess;

	private static final LongConcurrentHashMap<PendingPacket> Pendings = new LongConcurrentHashMap<>();
	private static volatile Future<?> Timer;

	public static void main(String[] args) throws Exception {
		// udp for subprocess register
		UdpSocket = new DatagramSocket(0, InetAddress.getLoopbackAddress());
		UdpSocket.setSoTimeout(200);

		var command = new ArrayList<String>();
		command.add("java");
		command.add("-D" + PropertyNamePort + "=" + UdpSocket.getLocalPort());
		Collections.addAll(command, args);

		var pb = new ProcessBuilder(command);
		pb.inheritIO();

		try {
			while (true) {
				Subprocess = pb.start();
				var exitCode = mainRun();
				if (exitCode == 0)
					break;
				joinMonitors();
				logger.warn("Subprocess Restart! ExitCode={}", exitCode);
			}
		} catch (Throwable ex) {
			logger.error("Daemon.main", ex);
		} finally {
			// 退出的时候，确保销毁服务进程。
			if (Subprocess != null)
				Subprocess.destroy();
		}
	}

	private static int mainRun() {
		while (true) {
			try {
				// 轮询：等待Global配置以及等待子进程退出。
				try {
					var cmd = receiveCommand(UdpSocket);
					switch (cmd.command()) {
					case Register.Command:
						var reg = (Register)cmd;
						var code = 0;
						if (Monitors.containsKey(reg.ServerId))
							code = 1;
						else {
							var monitor = new Monitor(reg);
							Monitors.put(reg.ServerId, monitor);
							monitor.start();
						}
						sendCommand(UdpSocket, cmd.Peer, new CommonResult(reg.ReliableSerialNo, code));
						logger.info("Register! Server={} code={}", reg.ServerId, code);
						break;

					case GlobalOn.Command:
						var on = (GlobalOn)cmd;
						code = 0;
						var monitor = Monitors.get(on.ServerId);
						if (monitor != null) {
							monitor.setConfig(on.GlobalIndex, on.GlobalConfig);
							logger.info("GlobalOn! Server={} ServerDaemonTimeout={} ServerReleaseTimeout={}",
									on.ServerId, on.GlobalConfig.ServerDaemonTimeout, on.GlobalConfig.ServerReleaseTimeout);
						} else {
							logger.warn("GlobalOn! not found serverId={} ServerDaemonTimeout={} ServerReleaseTimeout={}",
									on.ServerId, on.GlobalConfig.ServerDaemonTimeout, on.GlobalConfig.ServerReleaseTimeout);
							code = 1;
						}
						sendCommand(UdpSocket, cmd.Peer, new CommonResult(on.ReliableSerialNo, code));
						break;
					}
				} catch (SocketTimeoutException ex) {
					// skip
				}
				if (Subprocess.waitFor(0, TimeUnit.MILLISECONDS))
					return Subprocess.exitValue();
			} catch (Throwable ex) {
				logger.fatal("Daemon.mainRun", ex);
				fatalExit();
				return -1; // never get here
			}
		}
	}

	private static void fatalExit() {
		Subprocess.destroy();
		LogManager.shutdown();
		Runtime.getRuntime().halt(-1);
	}

	private static void joinMonitors() throws InterruptedException {
		for (var monitor : Monitors)
			monitor.stopAndJoin();
		Monitors.clear();
	}

	private static void destroySubprocess() throws InterruptedException {
		Subprocess.destroy();
		joinMonitors();
	}

	private static final class PendingPacket {
		public final DatagramSocket Socket;
		public final DatagramPacket Packet;
		public long SendTime = System.currentTimeMillis();

		public PendingPacket(DatagramSocket socket, DatagramPacket packet) {
			Socket = socket;
			Packet = packet;
		}
	}

	public static void sendCommand(DatagramSocket socket, SocketAddress peer, Command cmd) throws IOException {
		var bb = ByteBuffer.Allocate(5);
		bb.WriteInt(cmd.command());
		cmd.Encode(bb);
		var p = new DatagramPacket(bb.Bytes, 0, bb.WriteIndex, peer);
		if (cmd.isRequest()) {
			if (Pendings.putIfAbsent(cmd.ReliableSerialNo, new PendingPacket(socket, p)) != null)
				throw new RuntimeException("Duplicate ReliableSerialNo=" + cmd.ReliableSerialNo);

			// auto start Timer
			if (Timer == null) {
				synchronized (Pendings) {
					if (Timer == null) {
						Timer = Task.schedule(1000, 1000, () -> {
							var now = System.currentTimeMillis();
							for (var pending : Pendings) {
								if (now - pending.SendTime > 1000) {
									pending.SendTime = now;
									pending.Socket.send(pending.Packet);
								}
							}
						});
						ShutdownHook.add(() -> Timer.cancel(false));
					}
				}
			}
		}
		socket.send(p);
	}

	public static Command receiveCommand(DatagramSocket socket) throws IOException {
		var buf = new byte[1024];
		var p = new DatagramPacket(buf, buf.length);
		socket.receive(p);
		var bb = ByteBuffer.Wrap(buf, 0, p.getLength());
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
		default:
			throw new RuntimeException("Unknown Command =" + c);
		}
		if (cmd.ReliableSerialNo != 0)
			Pendings.remove(cmd.ReliableSerialNo);
		return cmd;
	}

	private static class Monitor extends Thread {
		private final SocketAddress PeerSocketAddress;
		private final AtomicReferenceArray<AchillesHeelConfig> GlobalConfigs;
		private final String fileName;
		private final RandomAccessFile raf;
		private final FileChannel Channel;
		private final MappedByteBuffer MMap;
		private volatile boolean Running = true;

		public Monitor(Register reg) throws Exception {
			PeerSocketAddress = reg.Peer;
			GlobalConfigs = new AtomicReferenceArray<>(reg.GlobalCount);
			fileName = reg.MMapFileName;
			raf = new RandomAccessFile(new File(fileName), "rw");
			Channel = raf.getChannel();
			MMap = Channel.map(FileChannel.MapMode.READ_WRITE, 0, Channel.size());
		}

		public AchillesHeelConfig getConfig(int index) {
			return GlobalConfigs.get(index);
		}

		public void setConfig(int index, AchillesHeelConfig config) {
			GlobalConfigs.set(index, config);
		}

		private ByteBuffer copyMMap() throws IOException {
			synchronized (Channel) {
				// Channel.lock 对同一个进程不能并发。
				var lock = Channel.lock();
				try {
					var copy = new byte[GlobalConfigs.length() * 8];
					MMap.position(0);
					MMap.get(copy, 0, copy.length);
					return ByteBuffer.Wrap(copy);
				} finally {
					lock.release();
				}
			}
		}

		@Override
		public void run() {
			try {
				while (Running) {
					var bb = copyMMap();
					var now = System.currentTimeMillis();
					for (int i = 0; i < GlobalConfigs.length(); ++i) {
						var activeTime = bb.ReadLong8();
						var config = getConfig(i);
						if (config == null)
							continue; // skip not ready global

						var idle = now - activeTime;
						if (idle > config.ServerReleaseTimeout) {
							logger.info("destroySubprocess {} - {} > {}", now, activeTime, config.ServerReleaseTimeout);
							destroySubprocess();
							// daemon main will restart subprocess!
						} else if (idle > config.ServerDaemonTimeout) {
							logger.info("sendCommand Release-{} {} - {} > {}", i, now, activeTime, config.ServerDaemonTimeout);
							// 在Server执行Release期间，命令可能重复发送。
							// 重复命令的处理由Server完成，
							// 这里重发也是需要的，刚好解决Udp不可靠性。
							sendCommand(UdpSocket, PeerSocketAddress, new Release(i));
						}
						//noinspection BusyWait
						Thread.sleep(1000);
					}
				}
			} catch (Throwable ex) {
				logger.fatal("Monitor.run", ex);
				fatalExit();
			}
		}

		public void stopAndJoin() throws InterruptedException {
			Running = false;
			join();
			try {
				Channel.close();
			} catch (Exception e) {
				logger.error("Channel.close", e);
			}
			try {
				raf.close();
			} catch (Exception e) {
				logger.error("File.close", e);
			}
			try {
				//noinspection ResultOfMethodCallIgnored
				new File(fileName).delete(); // try delete
			} catch (Exception ignored) {
			}
		}
	}

	public static abstract class Command implements Serializable {
		private static final AtomicLong Seed = new AtomicLong();

		public SocketAddress Peer;
		public long ReliableSerialNo;
		private boolean isRequest;

		public abstract int command();

		public boolean isRequest() {
			return isRequest;
		}

		public void setReliableSerialNo() {
			do
				ReliableSerialNo = Seed.incrementAndGet();
			while (ReliableSerialNo == 0);
			isRequest = true;
		}

		@Override
		public void Encode(ByteBuffer bb) {
			bb.WriteLong(ReliableSerialNo);
		}

		@Override
		public void Decode(ByteBuffer bb) {
			ReliableSerialNo = bb.ReadLong();
		}
	}

	// 精简版本配置。仅传递Daemon需要的参数过来。
	public static class AchillesHeelConfig implements Serializable {
		public int ServerDaemonTimeout;
		public int ServerReleaseTimeout;

		@Override
		public void Encode(ByteBuffer bb) {
			bb.WriteInt(ServerDaemonTimeout);
			bb.WriteInt(ServerReleaseTimeout);
		}

		@Override
		public void Decode(ByteBuffer bb) {
			ServerDaemonTimeout = bb.ReadInt();
			ServerReleaseTimeout = bb.ReadInt();
		}
	}

	public static class Register extends Command {
		public static final int Command = 0;

		public int ServerId;
		public int GlobalCount;
		public String MMapFileName;

		public Register(int serverId, int c, String name) {
			ServerId = serverId;
			GlobalCount = c;
			MMapFileName = name;
			setReliableSerialNo(); // enable reliable
		}

		public Register(ByteBuffer bb, SocketAddress peer) {
			Decode(bb);
			Peer = peer;
		}

		@Override
		public int command() {
			return Command;
		}

		@Override
		public void Encode(ByteBuffer bb) {
			super.Encode(bb);
			bb.WriteInt(ServerId);
			bb.WriteInt(GlobalCount);
			bb.WriteString(MMapFileName);
		}

		@Override
		public void Decode(ByteBuffer bb) {
			super.Decode(bb);
			ServerId = bb.ReadInt();
			GlobalCount = bb.ReadInt();
			MMapFileName = bb.ReadString();
		}
	}

	public static class GlobalOn extends Command {
		public static final int Command = 1;

		public int ServerId;
		public int GlobalIndex;
		public final AchillesHeelConfig GlobalConfig = new AchillesHeelConfig();

		public GlobalOn(int serverId, int index, int server, int release) {
			ServerId = serverId;
			GlobalIndex = index;
			GlobalConfig.ServerDaemonTimeout = server;
			GlobalConfig.ServerReleaseTimeout = release;
			setReliableSerialNo(); // enable reliable
		}

		public GlobalOn(ByteBuffer bb, SocketAddress peer) {
			Decode(bb);
			Peer = peer;
		}

		@Override
		public int command() {
			return Command;
		}

		@Override
		public void Encode(ByteBuffer bb) {
			super.Encode(bb);
			bb.WriteInt(ServerId);
			bb.WriteInt(GlobalIndex);
			GlobalConfig.Encode(bb);
		}

		@Override
		public void Decode(ByteBuffer bb) {
			super.Decode(bb);
			ServerId = bb.ReadInt();
			GlobalIndex = bb.ReadInt();
			GlobalConfig.Decode(bb);
		}
	}

	public static class CommonResult extends Command {
		public static final int Command = 2;

		public int Code;

		public CommonResult(long serial, int code) {
			ReliableSerialNo = serial;
			Code = code;
		}

		public CommonResult(ByteBuffer bb, SocketAddress peer) {
			Decode(bb);
			Peer = peer;
		}

		@Override
		public int command() {
			return Command;
		}

		@Override
		public void Encode(ByteBuffer bb) {
			super.Encode(bb);
			bb.WriteInt(Code);
		}

		@Override
		public void Decode(ByteBuffer bb) {
			super.Decode(bb);
			Code = bb.ReadInt();
		}
	}

	public static class Release extends Command {
		public static final int Command = 3;

		public int GlobalIndex;

		public Release(int index) {
			GlobalIndex = index;
		}

		public Release(ByteBuffer bb, SocketAddress peer) {
			Decode(bb);
			Peer = peer;
		}

		@Override
		public int command() {
			return Command;
		}

		@Override
		public void Encode(ByteBuffer bb) {
			bb.WriteInt(GlobalIndex);
		}

		@Override
		public void Decode(ByteBuffer bb) {
			GlobalIndex = bb.ReadInt();
		}
	}
}
