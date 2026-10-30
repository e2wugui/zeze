package Zeze.Services;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import Zeze.Util.LongConcurrentHashMap;
import Zeze.Util.Task;
import harness.Fast;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import static org.junit.jupiter.api.Assertions.*;

@Fast
@Isolated("Daemon可靠UDP使用进程级pending表和重发任务")
public class TestDaemonReliableReply {
	@SuppressWarnings("unchecked")
	private static LongConcurrentHashMap<Object> pendings() throws Exception {
		var field = Daemon.class.getDeclaredField("pendings");
		field.setAccessible(true);
		return (LongConcurrentHashMap<Object>)field.get(null);
	}

	private static DatagramSocket socket() throws Exception {
		var socket = new DatagramSocket(0, InetAddress.getLoopbackAddress());
		socket.setSoTimeout(2_000);
		return socket;
	}

	private static Daemon.GlobalOn sendPending(DatagramSocket source, DatagramSocket peer) throws Exception {
		Task.tryInitThreadPool();
		var request = new Daemon.GlobalOn(1, 0, 8_000, 60_000);
		Daemon.sendCommand(source, peer.getLocalSocketAddress(), request);
		peer.receive(new DatagramPacket(new byte[1024], 1024));
		assertNotNull(pendings().get(request.reliableSerialNo));
		return request;
	}

	private static void checkUnrelatedPacket(int mode) throws Exception {
		try (var source = socket(); var peer = socket(); var foreign = socket(); var otherReceiver = socket()) {
			var request = sendPending(source, peer);
			var pending = pendings().get(request.reliableSerialNo);
			try {
				Daemon.Command reply;
				if (mode == 0) {
					reply = new Daemon.Release(0);
					reply.reliableSerialNo = request.reliableSerialNo;
				} else
					reply = new Daemon.CommonResult(request.reliableSerialNo, 0);
				var sender = mode == 1 ? foreign : peer;
				var receiver = mode == 2 ? otherReceiver : source;
				Daemon.sendCommand(sender, receiver.getLocalSocketAddress(), reply);
				Daemon.receiveCommand(receiver);
				assertSame(pending, pendings().get(request.reliableSerialNo),
						"非本次请求对应的应答不得取消重发，mode=" + mode);
			} finally {
				pendings().remove(request.reliableSerialNo);
			}
		}
	}

	@Test
	public void commandWithMatchingSerialDoesNotAcknowledge() throws Exception {
		checkUnrelatedPacket(0);
	}

	@Test
	public void replyFromOtherPeerDoesNotAcknowledge() throws Exception {
		checkUnrelatedPacket(1);
	}

	@Test
	public void replyToOtherSocketDoesNotAcknowledge() throws Exception {
		checkUnrelatedPacket(2);
	}

	@Test
	public void matchingReplyRemovesOnlyItsOwnPending() throws Exception {
		try (var source = socket(); var peer = socket()) {
			var request = sendPending(source, peer);
			var other = sendPending(source, peer);
			try {
				Daemon.sendCommand(peer, source.getLocalSocketAddress(), new Daemon.CommonResult(request.reliableSerialNo, 0));
				var reply = (Daemon.CommonResult)Daemon.receiveCommand(source);
				assertEquals(request.reliableSerialNo, reply.reliableSerialNo);
				assertNull(pendings().get(request.reliableSerialNo));
				assertNotNull(pendings().get(other.reliableSerialNo));
			} finally {
				pendings().remove(request.reliableSerialNo);
				pendings().remove(other.reliableSerialNo);
			}
		}
	}
}
