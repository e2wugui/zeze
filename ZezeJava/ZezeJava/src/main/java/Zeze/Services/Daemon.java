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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.Serializable;
import Zeze.Util.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Daemon {
	public final static String PropertyNamePort = "Zeze.ProcessDaemon.Port";

	private static final Logger logger = LogManager.getLogger(Daemon.class);

	public static void main(String args[]) throws Exception {
		// udp for subprocess register
		UdpSocket = new DatagramSocket(0, InetAddress.getLoopbackAddress());

		var command = new ArrayList<String>();
		command.add("java");
		command.add("-D" + PropertyNamePort + "=" + UdpSocket.getLocalPort());
		for (int i = 0; i < args.length; ++i)
			command.add(args[i]);

		var pb = new ProcessBuilder(command);
		pb.inheritIO();
		Subprocess = pb.start();

		try {
			while (true) {
				var exitCode = mainRun();
				if (0 == exitCode)
					break;
				joinMonitors();
				logger.warn("Subprocess Restart! ExitCode=" + exitCode);
				Subprocess = pb.start();
			}
		} catch (Throwable ex) {
			logger.error("", ex);
		} finally {
			// 退出的时候，确保销毁服务进程。
			Subprocess.destroy();
		}
	}

	private static int mainRun() {
		while (true) {
			try {
				// 轮询：等待Global配置以及等待子进程退出。
				try {
					var cmd = Daemon.receiveCommand(UdpSocket);
					switch (cmd.command()) {
					case Register.Command:
						var reg = (Register)cmd;
						var code = 0;
						var monitor = new Monitor(reg);
						if (null == Monitors.putIfAbsent(reg.ServerId, monitor))
							monitor.start();
						else
							code = 1;
						Daemon.sendCommand(UdpSocket, cmd.Peer, new CommonResult(reg.ReliableSerialNo, code));
						logger.info("Register! Server={} code={}", reg.ServerId, code);
						break;

					case GlobalOn.Command:
						var on = (GlobalOn)cmd;
						Monitors.get(on.ServerId).setConfig(on.GlobalIndex, on.GlobalConfig);
						sendCommand(UdpSocket, cmd.Peer, new CommonResult(cmd.ReliableSerialNo, 0));
						logger.info("GlobalOn! Server={} ServerDaemonTimeout={} ServerReleaseTimeout={}",
								on.ServerId,
								on.GlobalConfig.ServerDaemonTimeout,
								on.GlobalConfig.ServerReleaseTimeout);
						break;
					}
				} catch (SocketTimeoutException ex) {
					// skip
				}
				if (Subprocess.waitFor(1, TimeUnit.MILLISECONDS)) {
					return Subprocess.exitValue();
				}
			} catch (Throwable ex) {
				logger.fatal("", ex);
				fatalExit();
				return -1; // never get here
			}
		}
	}

	// Key Is ServerId。每个Server对应一个Monitor。
	// 正常使用是一个Daemon对应一个Server。
	// 写成支持多个Server是为了跑Simulate测试。
	private static ConcurrentHashMap<Integer, Monitor> Monitors = new ConcurrentHashMap<>();
	private static DatagramSocket UdpSocket;
	private static Process Subprocess;

	public static void fatalExit() {
		Subprocess.destroy();
		LogManager.shutdown();
		Runtime.getRuntime().halt(-1);
	}

	public static void joinMonitors() throws InterruptedException {
		for (var monitor : Monitors.values())
			monitor.stopAndJoin();
		Monitors.clear();
	}

	public static void destroySubprocess() throws InterruptedException {
		Subprocess.destroy();
		joinMonitors();
	}

	public static class PendingPacket {
		public DatagramSocket Socket;
		public DatagramPacket Packet;
		public long SendTime;

		public PendingPacket(DatagramSocket socket, DatagramPacket packet) {
			Socket = socket;
			Packet = packet;
			SendTime = System.currentTimeMillis();
		}
	}

	private static ConcurrentHashMap<Long, PendingPacket> Pending = new ConcurrentHashMap<>();
	private static volatile Future<?> Timer;

	public static void sendCommand(DatagramSocket socket, SocketAddress peer, Command cmd) throws IOException {
		var bb = ByteBuffer.Allocate();
		bb.WriteInt(cmd.command());
		cmd.Encode(bb);
		var p = new DatagramPacket(bb.Bytes, 0, bb.Size(), peer);
		if (cmd.ReliableSerialNo != 0) {
			if (null != Pending.putIfAbsent(cmd.ReliableSerialNo, new PendingPacket(socket, p)))
				throw new RuntimeException("Duplicate ReliableSerialNo=" + cmd.ReliableSerialNo);

			// auto start Timer
			if (null == Timer) {
				synchronized (Pending) {
					if (null == Timer) {
						Timer = Task.schedule(1000, 1000, () -> {
							var now = System.currentTimeMillis();
							for (var pending : Pending.values()) {
								if (now - pending.SendTime > 1000) {
									pending.Socket.send(pending.Packet);
									pending.SendTime = now;
								}
							}
						});
						Zeze.Util.ShutdownHook.add(() -> Timer.cancel(false));
					}
				}
			}
		}
		socket.send(p);
	}

	public static Command receiveCommand(DatagramSocket socket) throws IOException {
		var cmd = _receiveCommand(socket);
		if (cmd.ReliableSerialNo != 0)
			Pending.remove(cmd.ReliableSerialNo);
		return cmd;
	}

	private static Command _receiveCommand(DatagramSocket socket) throws IOException {
		var buf = new byte[1024];
		var p = new DatagramPacket(buf, buf.length);
		socket.setSoTimeout(200);
		socket.receive(p);
		var bb = ByteBuffer.Wrap(buf, 0, p.getLength());
		var cmd = bb.ReadInt();
		switch (cmd) {
		case Register.Command -> new Register(bb, p.getSocketAddress());
		case CommonResult.Command -> new CommonResult(bb, p.getSocketAddress());
		case GlobalOn.Command -> new GlobalOn(bb, p.getSocketAddress());
		case Release.Command -> new Release(bb, p.getSocketAddress());
		}
		throw new RuntimeException("Unknown Command =" + cmd);
	}

	public static class Monitor extends Thread {
		public File FileName;

		public final int ServerId;
		public final SocketAddress PeerSocketAddress;

		private RandomAccessFile File;
		private FileChannel Channel;
		private MappedByteBuffer MMap;
		private AchillesHeelConfig [] GlobalConfigs;

		public Monitor(Register reg) throws Exception {
			ServerId = reg.ServerId;
			PeerSocketAddress = reg.Peer;
			GlobalConfigs = new AchillesHeelConfig[reg.GlobalCount];
			FileName = new File(reg.MMapFileName);
			File = new RandomAccessFile(FileName, "r");
			Channel = File.getChannel();
			MMap = Channel.map(FileChannel.MapMode.READ_ONLY, 0, Channel.size());
		}

		public synchronized AchillesHeelConfig getConfig(int index) {
			return GlobalConfigs[index];
		}

		public synchronized void setConfig(int index, AchillesHeelConfig config) {
			GlobalConfigs[index] = config;
		}

		public void close() throws IOException {
			Channel.close();
			File.close();
		}

		private volatile boolean Running = true;

		private ByteBuffer copyMMap() throws IOException {
			var lock = Channel.lock();
			try {
				var copy = new byte[GlobalConfigs.length * 8];
				MMap.get(copy, 0, copy.length);
				return ByteBuffer.Wrap(copy);
			} finally {
				lock.release();
			}
		}

		@Override
		public void run() {
			try {
				while (Running) {
					var bb = copyMMap();
					var now = System.currentTimeMillis();
					for (int i = 0; i < GlobalConfigs.length; ++i) {
						var activeTime = bb.ReadLong8();
						var config = getConfig(i);
						if (null == config)
							continue; // skip not ready global

						var idle = now - activeTime;
						if (idle > config.ServerReleaseTimeout) {
							Daemon.destroySubprocess();
							// daemon main will restart subprocess!
						} else if (idle > config.ServerDaemonTimeout) {
							// 在Server执行Release期间，命令可能重复发送。
							// 重复命令的处理由Server完成，
							// 这里重发也是需要的，刚好解决Udp不可靠性。
							Daemon.sendCommand(UdpSocket, PeerSocketAddress, new Release(i));
						}
						Thread.sleep(1000);
					}
				}
			} catch (Throwable ex) {
				logger.error(ex);
				Daemon.fatalExit();
			}
		}

		public void stopAndJoin() throws InterruptedException {
			Running = false;
			join();
		}
	}

	public static abstract class Command implements Serializable {
		public abstract int command();
		public SocketAddress Peer;
		public long ReliableSerialNo;

		private static AtomicLong Seed = new AtomicLong();

		public void setReliableSerialNo() {
			while (ReliableSerialNo == 0)
				ReliableSerialNo = Seed.incrementAndGet();
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
		public final static int Command = 0;

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
			return command();
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
		public final static int Command = 1;

		public int ServerId;
		public int GlobalIndex;
		public AchillesHeelConfig GlobalConfig = new AchillesHeelConfig();

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
		public final static int Command = 2;

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
		public final static int Command = 3;

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
