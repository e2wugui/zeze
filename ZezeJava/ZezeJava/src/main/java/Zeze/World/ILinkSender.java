package Zeze.World;

import java.util.Collection;
import Zeze.Builtin.Provider.Send;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Data;

/**
 * 发送协议给客户端，抽象这个是为了便于写测试案例。
 */
public interface ILinkSender {
	boolean sendLink(String linkName, ByteBuffer fullEncodedProtocol);
	boolean sendLink(String linkName, Send send);
	boolean sendCommand(String linkName, long linkSid, long mapInstanceId, int commandId, Data data);
	boolean sendCommand(Collection<Entity> targets, long mapInstanceId, int commandId, Data data);
}
