package Zeze.Services;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.Serializable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Daemon {
	public final static String PropertyNamePort = "Zeze.ProcessDaemon.Port";
	public final static String PropertyNameMMap = "Zeze.ProcessDaemon.MMap";

	private static final Logger logger = LogManager.getLogger(Daemon.class);

	public static void main(String args[]) throws Exception {
		var maxGlobalCount = 1024;
		var transferArgStart = 0;
		if (args[0].equals("-MaxGlobalCount")) {
			maxGlobalCount = Integer.parseInt(args[1]);
			transferArgStart = 2;
		}
		var monitor = new Monitor(maxGlobalCount);
		var command = new ArrayList<String>();
		command.add("java");
		command.add("-D" + PropertyNamePort + "=" + monitor.UdpSocket.getLocalPort());
		command.add("-D" + PropertyNameMMap + "=" + monitor.FileName);
		for (int i = transferArgStart; i < args.length; ++i)
			command.add(args[i]);

		var pb = new ProcessBuilder(command);
		pb.inheritIO();
		Subprocess = pb.start();
		monitor.start();

		while (true) {
			monitor.waitApplicationReady();
			logger.info("Application Register Ok!");
			var exitCode = Subprocess.waitFor();
			if (exitCode == 0)
				break;
			logger.warn("Application Restart Now. ExitCode=" + exitCode);
			Subprocess = pb.start();
		}
		monitor.stopAndJoin();
	}

	private static Process Subprocess;
	public static void fatalExit() {
		Subprocess.destroy();
		LogManager.shutdown();
		Runtime.getRuntime().halt(-1);
	}

	public static void sendCommand(DatagramSocket socket, Command cmd, SocketAddress peer) throws IOException {
		var bb = ByteBuffer.Allocate();
		bb.WriteInt(cmd.command());
		cmd.Encode(bb);
		var p = new DatagramPacket(bb.Bytes, 0, bb.Size(), peer);
		socket.send(p);
	}

	public static Command receiveCommand(DatagramSocket socket) throws IOException {
		var buf = new byte[1024];
		var p = new DatagramPacket(buf, buf.length);
		socket.receive(p);
		var bb = ByteBuffer.Wrap(buf, 0, p.getLength());
		var cmd = bb.ReadInt();
		switch (cmd) {
		case Register.Command -> new Register(bb, p.getSocketAddress());
		case RegisterResult.Command -> new RegisterResult(bb, p.getSocketAddress());
		case Release.Command -> new Release(bb, p.getSocketAddress());
		}
		throw new RuntimeException("Unknown Command =" + cmd);
	}

	public static class Monitor extends Thread {
		public DatagramSocket UdpSocket;
		public File FileName;

		private RandomAccessFile File;
		private FileChannel Channel;
		private MappedByteBuffer MMap;
		private ArrayList<AchillesHeelConfig> GlobalConfigs;
		private SocketAddress SubprocessSocketAddress;

		public Monitor(int globalCount) throws Exception {
			// udp for subprocess register
			UdpSocket = new DatagramSocket(0, InetAddress.getLoopbackAddress());

			// mmap for subprocess report Global ActiveTime
			FileName = Files.createTempFile("zeze", ".mmap").toFile();
			File = new RandomAccessFile(FileName, "rw");
			File.setLength(8 * globalCount + 4);
			Channel = File.getChannel();
			MMap = Channel.map(FileChannel.MapMode.READ_WRITE, 0, Channel.size());
		}

		public void close() throws IOException {
			Channel.close();
			File.close();
		}

		public void waitApplicationReady() throws Exception {
			var cmd = Daemon.receiveCommand(UdpSocket);
			if (cmd.command() != Register.Command)
				throw new RuntimeException("Not Register Command. is " + cmd.command());
			var reg = (Register)cmd;
			Daemon.sendCommand(UdpSocket, new RegisterResult(), cmd.Peer);
			GlobalConfigs = reg.GlobalConfigs;
			SubprocessSocketAddress = cmd.Peer;
		}

		private volatile boolean Running = true;

		private ByteBuffer copyMMap() throws IOException {
			var lock = Channel.lock();
			try {
				var mmap = MMap.array();
				var bb = ByteBuffer.Wrap(mmap);
				var count = bb.ReadInt4();
				return ByteBuffer.Wrap(Arrays.copyOf(mmap, count * 8 + 4));
			} finally {
				lock.release();
			}
		}

		@Override
		public void run() {
			try {
				while (Running) {
					var bb = copyMMap();
					var count = bb.ReadInt4();
					var now = System.currentTimeMillis();
					for (int i = 0; i < count; ++i) {
						var activeTime = bb.ReadLong8();
						var config = GlobalConfigs.get(i);
						var idle = now - activeTime;
						if (idle > config.ServerReleaseTimeout) {
							Daemon.Subprocess.destroy();
							// daemon main will restart subprocess!
						} else if (idle > config.ServerDaemonTimeout) {
							// 在Server执行Release期间，命令可能重复发送。
							// 重复命令的处理由Server完成，
							// 这里重发也是需要的，刚好解决Udp不可靠性。
							Daemon.sendCommand(UdpSocket, new Release(i), SubprocessSocketAddress);
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
		public int Port;
		public ArrayList<AchillesHeelConfig> GlobalConfigs = new ArrayList<>();

		public Register() {

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
			bb.WriteInt(Port);
			bb.WriteInt(GlobalConfigs.size());
			for (var config : GlobalConfigs)
				config.Encode(bb);
		}

		@Override
		public void Decode(ByteBuffer bb) {
			Port = bb.ReadInt();
			GlobalConfigs.clear();
			for (int count = bb.ReadInt(); count > 0; --count) {
				var config = new AchillesHeelConfig();
				config.Decode(bb);
				GlobalConfigs.add(config);
			}
		}
	}

	public static class RegisterResult extends Command {
		public final static int Command = 1;

		public int Code;

		public RegisterResult() {

		}

		public RegisterResult(ByteBuffer bb, SocketAddress peer) {
			Decode(bb);
			Peer = peer;
		}

		@Override
		public int command() {
			return Command;
		}

		@Override
		public void Encode(ByteBuffer bb) {
			bb.WriteInt(Code);
		}

		@Override
		public void Decode(ByteBuffer bb) {
			Code = bb.ReadInt();
		}
	}

	public static class Release extends Command {
		public final static int Command = 2;

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
