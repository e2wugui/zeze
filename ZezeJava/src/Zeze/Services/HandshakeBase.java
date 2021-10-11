package Zeze.Services;

import Zeze.Net.*;
import Zeze.Serialize.*;
import Zeze.Transaction.*;
import Zeze.*;
import java.util.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.*;

public class HandshakeBase extends Service {
	private static final Logger logger = LogManager.getLogger(HandshakeBase.class);

	private HashSet<Integer> HandshakeProtocols = new HashSet<Integer>();

	// For Client Only
	private java.util.concurrent.ConcurrentHashMap<Long, BigInteger> DHContext = new java.util.concurrent.ConcurrentHashMap<Long, BigInteger>();

	public HandshakeBase(String name, Config config) {
		super(name, config);
	}

	public HandshakeBase(String name, Application app) {
		super(name, app);
	}

	public final boolean IsHandshakeProtocol(int typeId) {
		return HandshakeProtocols.contains(typeId);
	}

	protected final void AddHandshakeServerFactoryHandle() {
	{
			var tmp = new Zeze.Services.Handshake.CHandshake();
			HandshakeProtocols.add(tmp.getTypeId());
			AddFactoryHandle(tmp.getTypeId(), new Service.ProtocolFactoryHandle() {Factory = () -> new Zeze.Services.Handshake.CHandshake(), Handle = ProcessCHandshake, NoProcedure = true});
	} {
			var tmp = new Zeze.Services.Handshake.CHandshakeDone();
			HandshakeProtocols.add(tmp.getTypeId());
			AddFactoryHandle(tmp.getTypeId(), new Service.ProtocolFactoryHandle() {Factory = () -> new Zeze.Services.Handshake.CHandshakeDone(), Handle = ProcessCHandshakeDone, NoProcedure = true});
		}
	}