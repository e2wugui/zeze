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
	
	private int ProcessCHandshake(Protocol _p) {
		// TODO whf
		return 0;
	}
	
	private int ProcessCHandshakeDone(Protocol _p) {
		OnHandshakeDone(_p.Sender);
        return 0;
	}

	protected final void AddHandshakeServerFactoryHandle() {
		{
			var tmp = new Zeze.Services.Handshake.CHandshake();
			HandshakeProtocols.add(tmp.getTypeId());
			AddFactoryHandle(tmp.getTypeId(), new Service.ProtocolFactoryHandle(() -> new Zeze.Services.Handshake.CHandshake()
					, this::ProcessCHandshake
					, true));
		} 
		{
			var tmp = new Zeze.Services.Handshake.CHandshakeDone();
			HandshakeProtocols.add(tmp.getTypeId());
			AddFactoryHandle(tmp.getTypeId(), new Service.ProtocolFactoryHandle(() -> new Zeze.Services.Handshake.CHandshakeDone()
					, this::ProcessCHandshakeDone
					, true));
		}
	}
	
	protected void AddHandshakeClientFactoryHandle() {
        // TODO whf
    }

    private int ProcessSHandshake(Protocol _p) {
        // TODO whf
    	return 0;
    }

    protected void StartHandshake(AsyncSocket so) {
        // TODO whf
    }
}