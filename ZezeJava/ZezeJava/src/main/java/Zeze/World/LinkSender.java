package Zeze.World;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import Zeze.Builtin.Provider.Send;
import Zeze.Builtin.World.Command;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Data;
import Zeze.Util.LongList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LinkSender implements ILinkSender {
	private static final Logger logger = LogManager.getLogger(LinkSender.class);

	public final World world;

	public LinkSender(World world) {
		this.world = world;
	}

	@Override
	public boolean sendLink(String linkName, ByteBuffer fullEncodedProtocol) {
		var link = world.providerApp.providerService.getLinks().get(linkName);
		if (null == link) {
			logger.info("link not found: {}", linkName);
			return false;
		}
		var socket = link.TryGetReadySocket();
		if (null == socket) {
			logger.info("link socket not ready. {}", linkName);
			return false;
		}
		return socket.Send(fullEncodedProtocol);
	}

	@Override
	public boolean sendLink(String linkName, Send send) {
		var link = world.providerApp.providerService.getLinks().get(linkName);
		if (null == link) {
			logger.info("link not found: {}", linkName);
			return false;
		}
		var socket = link.TryGetReadySocket();
		if (null == socket) {
			logger.info("link socket not ready. {}", linkName);
			return false;
		}
		return send.Send(socket, rpc -> triggerLinkBroken(
				linkName, send.isTimeout() ? send.Argument.getLinkSids() : send.Result.getErrorLinkSids()));
	}

	private static long triggerLinkBroken(String linkName, LongList errorSids) {
		if (!errorSids.isEmpty())
			logger.warn("{} errorSids={}", linkName, errorSids);
		return 0;
	}

	@Override
	public boolean sendCommand(String linkName, long linkSid, long mapInstanceId, int commandId, Data data) {
		logger.info("SendCommand {} {} {}:{}", linkName, linkSid, mapInstanceId, commandId);
		var send = encodeSend(
				java.util.List.of(linkSid),
				Command.TypeId_,
				encodeCommand(mapInstanceId, commandId, data));
		return sendLink(linkName, send);
	}

	@Override
	public boolean sendCommand(Collection<Entity> targets, long mapInstanceId, int commandId, Data data) {
		var group = new HashMap<String, ArrayList<Long>>();
		for (var target : targets) {
			var link = group.computeIfAbsent(target.getBean().getLinkName(), (key) -> new ArrayList<>());
			link.add(target.getBean().getLinkSid());
		}
		var result = true;
		var command = encodeCommand(mapInstanceId, commandId, data);
		for (var e : group.entrySet()) {
			logger.info("SendCommand {} {} {}:{}", e.getKey(), e.getValue(), mapInstanceId, commandId);
			result &= sendLink(e.getKey(), encodeSend(e.getValue(), Command.TypeId_, command));
		}
		return result;
	}

	public static Send encodeSend(Collection<Long> linkSids, long typeId, ByteBuffer wholeProtocol) {
		var send = new Send();
		send.Argument.getLinkSids().addAll(linkSids);
		send.Argument.setProtocolType(typeId);
		send.Argument.setProtocolWholeData(new Binary(wholeProtocol));

		return send;
	}

	public static ByteBuffer encodeCommand(long mapInstanceId, int commandId, Data data) {
		var cmd = new Command();
		cmd.Argument.setCommandId(commandId);
		cmd.Argument.setMapInstanceId(mapInstanceId);
		var bb = ByteBuffer.Allocate();
		data.encode(bb);
		cmd.Argument.setParam(new Binary(bb));

		return cmd.encode();
	}
}
