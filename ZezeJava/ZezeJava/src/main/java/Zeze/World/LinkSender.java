package Zeze.World;

import java.security.Provider;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import Zeze.Builtin.Provider.Send;
import Zeze.Builtin.World.Command;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Data;
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
	public boolean sendCommand(String linkName, long linkSid, int commandId, Data data) {
		return sendLink(linkName, encodeSend(java.util.List.of(linkSid), commandId, data));
	}

	@Override
	public boolean sendCommand(Collection<Entity> targets, int commandId, Data data) {
		var group = new HashMap<String, ArrayList<Long>>();
		for (var target : targets) {
			var link = group.computeIfAbsent(target.getBean().getLinkName(), (key) -> new ArrayList<>());
			link.add(target.getBean().getLinkSid());
		}
		var result = true;
		for (var e : group.entrySet()) {
			result &= sendLink(e.getKey(), encodeSend(e.getValue(), commandId, data));
		}
		return result;
	}

	public static ByteBuffer encodeSend(Collection<Long> linkSids, int commandId, Data data) {
		var cmd = new Command();
		cmd.Argument.setCommandId(commandId);
		var bb = ByteBuffer.Allocate();
		data.encode(bb);
		cmd.Argument.setParam(new Binary(bb));

		var send = new Send();
		send.Argument.getLinkSids().addAll(linkSids);
		send.Argument.setProtocolType(cmd.getTypeId());
		send.Argument.setProtocolWholeData(new Binary(cmd.encode()));

		return send.encode();
	}
}
