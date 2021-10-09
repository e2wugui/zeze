package Zeze.Services.ToLuaService;

import Zeze.*;
import Zeze.Services.*;
import Zeze.Net.*;
import Zeze.Serialize.*;
import java.util.*;

//#endif // end USE_KERA_LUA

public class ToLua {
	private ILua Lua;
	public final ILua getLua() {
		return Lua;
	}
	private void setLua(ILua value) {
		Lua = value;
	}

	public ToLua() {
	}

	public final void InitializeLua(ILua lua) {
		this.setLua(lua);
		if (this.getLua().DoString("local Zeze = require 'Zeze'\nreturn Zeze")) {
			throw new RuntimeException("load  'Zeze.lua' Error.");
		}
		LoadMeta();
	}

	private static class VariableMeta {
		private int Id;
		public final int getId() {
			return Id;
		}
		public final void setId(int value) {
			Id = value;
		}

		private int Type;
		public final int getType() {
			return Type;
		}
		public final void setType(int value) {
			Type = value;
		}
		private long TypeBeanTypeId;
		public final long getTypeBeanTypeId() {
			return TypeBeanTypeId;
		}
		public final void setTypeBeanTypeId(long value) {
			TypeBeanTypeId = value;
		}
		private int Key;
		public final int getKey() {
			return Key;
		}
		public final void setKey(int value) {
			Key = value;
		}
		private long KeyBeanTypeId;
		public final long getKeyBeanTypeId() {
			return KeyBeanTypeId;
		}
		public final void setKeyBeanTypeId(long value) {
			KeyBeanTypeId = value;
		}
		private int Value;
		public final int getValue() {
			return Value;
		}
		public final void setValue(int value) {
			Value = value;
		}
		private long ValueBeanTypeId;
		public final long getValueBeanTypeId() {
			return ValueBeanTypeId;
		}
		public final void setValueBeanTypeId(long value) {
			ValueBeanTypeId = value;
		}
		private String Name;
		public final String getName() {
			return Name;
		}
		public final void setName(String value) {
			Name = value;
		}

		private BeanMeta BeanMeta;
		public final BeanMeta getBeanMeta() {
			return BeanMeta;
		}
		public final void setBeanMeta(BeanMeta value) {
			BeanMeta = value;
		}

		@Override
		public String toString() {
			return String.format("{[%1$s]={%2$s,%3$s,%4$s,%5$s,%6$s,%7$s,\"%8$s\"}}", getId(), getType(), getTypeBeanTypeId(), getKey(), getKeyBeanTypeId(), getValue(), getValueBeanTypeId(), getName());
		}
	}

	private static class ProtocolArgument {
		private long ArgumentBeanTypeId;
		public final long getArgumentBeanTypeId() {
			return ArgumentBeanTypeId;
		}
		public final void setArgumentBeanTypeId(long value) {
			ArgumentBeanTypeId = value;
		}
		private long ResultBeanTypeId;
		public final long getResultBeanTypeId() {
			return ResultBeanTypeId;
		}
		public final void setResultBeanTypeId(long value) {
			ResultBeanTypeId = value;
		}
		private boolean IsRpc = false;
		public final boolean isRpc() {
			return IsRpc;
		}
		public final void setRpc(boolean value) {
			IsRpc = value;
		}
	}

	private static class BeanMeta {
		private String Name;
		public final String getName() {
			return Name;
		}
		public final void setName(String value) {
			Name = value;
		}
		private ArrayList<VariableMeta> Variables = new ArrayList<VariableMeta> ();
		public final ArrayList<VariableMeta> getVariables() {
			return Variables;
		}
	}

	private final HashMap<Long, BeanMeta> BeanMetas = new HashMap<Long, BeanMeta>(); // Bean.TypeId -> vars
	private final HashMap<Integer, ProtocolArgument> ProtocolMetas = new HashMap<Integer, ProtocolArgument>(); // protocol.TypeId -> Bean.TypeId

	public final void LoadMeta() {
		BeanMetas.clear();
		ProtocolMetas.clear();

		if (getLua().DoString("local meta = require 'ZezeMeta'\nreturn meta")) {
			throw new RuntimeException("load ZezeMeta.lua error");
		}
		if (false == getLua().IsTable(-1)) {
			throw new RuntimeException("ZezeMeta not return a table");
		}
		getLua().GetField(-1, "beans");
		getLua().PushNil();
		while (getLua().Next(-2)) { // -1 value of vars(table) -2 key of bean.TypeId
			long beanTypeId = getLua().ToInteger(-2);
			var beanMeta = new BeanMeta();
			getLua().PushNil();
			while (getLua().Next(-2)) { // -1 value of varmeta(table) -2 key of varid
				var varId = (int)getLua().ToInteger(-2);
				if (0 == varId) {
					// bean full name
					beanMeta.setName(getLua().toString(-1));
					getLua().Pop(1); // pop value XXX 忘了这里要不要Pop一次了。
					continue;
				}
				VariableMeta var = new VariableMeta();
				var.setBeanMeta(beanMeta);
				var.setId(varId);
				getLua().PushNil();
				while (getLua().Next(-2)) { // -1 value of typetag -2 key of index
					switch (getLua().ToInteger(-2)) {
						case 1:
							var.setType((int)getLua().ToInteger(-1));
							break;
						case 2:
							var.setTypeBeanTypeId(getLua().ToInteger(-1));
							break;
						case 3:
							var.setKey((int)getLua().ToInteger(-1));
							break;
						case 4:
							var.setKeyBeanTypeId(getLua().ToInteger(-1));
							break;
						case 5:
							var.setValue((int)getLua().ToInteger(-1));
							break;
						case 6:
							var.setValueBeanTypeId(getLua().ToInteger(-1));
							break;
						case 7:
							var.setName(getLua().toString(-1));
							break;
						default:
							throw new RuntimeException("error index for typetag");
					}
					getLua().Pop(1); // pop value
				}
				getLua().Pop(1); // pop value
				beanMeta.getVariables().add(var);
			}
			BeanMetas.put(beanTypeId, beanMeta);
			getLua().Pop(1); // pop value
		}
		getLua().Pop(1);

		getLua().GetField(-1, "protocols");
		getLua().PushNil();
		while (getLua().Next(-2)) { // -1 value of Protocol.Argument.BeanTypeId -2 Protocol.TypeId
			ProtocolArgument pa = new ProtocolArgument();
			getLua().PushNil();
			while (getLua().Next(-2)) { // -1 value of beantypeid -2 key of index
				switch (getLua().ToInteger(-2)) {
					case 1:
						pa.setArgumentBeanTypeId(getLua().ToInteger(-1));
						pa.setRpc(false);
						break;
					case 2:
						pa.setResultBeanTypeId(getLua().ToInteger(-1));
						pa.setRpc(true);
						break;
					default:
						throw new RuntimeException("error index for protocol argument bean typeid");
				}
				getLua().Pop(1);
			}
			ProtocolMetas.put((int)getLua().ToInteger(-2), pa);
			getLua().Pop(1); // pop value
		}
		getLua().Pop(1);
	}

	public final void CallSocketClose(FromLua service, long socketSessionId) {
		if (LuaType.Function != getLua().GetGlobal("ZezeSocketClose")) { // push func onto stack
			getLua().Pop(1);
			return;
		}

		getLua().PushObject(service);
		getLua().PushInteger(socketSessionId);
		getLua().Call(2, 0);
	}

	public final void CallHandshakeDone(FromLua service, long socketSessionId) {
		// void OnHandshakeDone(service, long sessionId)
		if (LuaType.Function != this.getLua().GetGlobal("ZezeHandshakeDone")) { // push func onto stack
			getLua().Pop(1);
			throw new RuntimeException("ZezeHandshakeDone is not a function");
		}

		getLua().PushObject(service);
		getLua().PushInteger(socketSessionId);
		getLua().Call(2, 0);
	}

	private int ZezeSendProtocol(IntPtr luaState) {
		//KeraLua.Lua lua = KeraLua.Lua.FromIntPtr(luaState);
		FromLua callback = getLua().<FromLua>ToObject(-3);
		long sessionId = getLua().ToInteger(-2);
		AsyncSocket socket = callback.getService().GetSocket(sessionId);
		if (null == socket) {
			return 0;
		}
		callback.getToLua().SendProtocol(socket);
		return 0;
	}

	private int ZezeUpdate(IntPtr luaState) {
		//KeraLua.Lua lua = KeraLua.Lua.FromIntPtr(luaState);
		FromLua callback = getLua().<FromLua>ToObject(-1);
		callback.getToLua().Update(callback.getService());
		return 0;
	}

	private int ZezeConnect(IntPtr luaState) {
		FromLua service = getLua().<FromLua>ToObject(-4);
		String host = getLua().toString(-3);
		int port = (int)getLua().ToInteger(-2);
		boolean autoReconnect = getLua().ToBoolean(-1);
		boolean tempVar = service.Service instanceof HandshakeClient;
		HandshakeClient client = tempVar ? (HandshakeClient)service.Service : null;
		if (tempVar) {
			client.Connect(host, port, autoReconnect);
		}
		return 0;
	}

	// 使用静态变量，防止垃圾回收。
//C# TO JAVA CONVERTER TODO TASK: There is no preprocessor in Java:
//#if USE_KERA_LUA
	private static KeraLua.LuaFunction ZezeUpdateFunction;
	private static KeraLua.LuaFunction ZezeSendProtocolFunction;
	private static KeraLua.LuaFunction ZezeConnectFunction;
//#endif // USE_KERA_LUA
	private static Object RegisterCallbackLock = new Object();

	public final void RegisterGlobalAndCallback(FromLua callback) {
		if (getLua().DoString("local Zeze = require 'Zeze'\nreturn Zeze")) {
			throw new RuntimeException("load Zeze.lua faild");
		}
		if (false == getLua().IsTable(-1)) {
			throw new RuntimeException("Zeze.lua not return a table");
		}

		getLua().PushString("Service" + callback.getName());
		getLua().PushObject(callback);
		getLua().SetTable(-3);
		getLua().PushString("CurrentService");
		getLua().PushObject(callback);
		getLua().SetTable(-3); // 当存在多个service时，这里保存最后一个。

		synchronized (RegisterCallbackLock) {
			// 所有的ToLua实例共享回调函数。
//C# TO JAVA CONVERTER TODO TASK: There is no preprocessor in Java:
//#if USE_KERA_LUA
			if (null == ZezeUpdateFunction) {
				ZezeUpdateFunction = ZezeUpdate;
				ZezeSendProtocolFunction = ZezeSendProtocol;
				ZezeConnectFunction = ZezeConnect;

				getLua().Register("ZezeUpdate", ZezeUpdateFunction);
				getLua().Register("ZezeSendProtocol", ZezeSendProtocolFunction);
				getLua().Register("ZezeConnect", ZezeConnectFunction);
			}
//#endif // USE_KERA_LUA
		}
	}

	public final void SendProtocol(AsyncSocket socket) {
		if (false == getLua().IsTable(-1)) {
			throw new RuntimeException("SendProtocol param is not a table.");
		}

		getLua().GetField(-1, "TypeId");
		int typeId = (int)getLua().ToInteger(-1);
		getLua().Pop(1);
		getLua().GetField(-1, "ResultCode");
		int resultCode = (int)getLua().ToInteger(-1);
		getLua().Pop(1);

		TValue pa;
		if (false == (ProtocolMetas.containsKey(typeId) && (pa = ProtocolMetas.get(typeId)) == pa)) {
			throw new RuntimeException("protocol not found in meta for typeid=" + typeId);
		}

		if (pa.IsRpc) {
			getLua().GetField(-1, "IsRequest");
			boolean isRequest = getLua().ToBoolean(-1);
			getLua().Pop(1);
			getLua().GetField(-1, "Sid");
			long sid = getLua().ToInteger(-1);
			getLua().Pop(1);
			getLua().GetField(-1, "Timeout");
			int timeout = (int)getLua().ToInteger(-1);
			getLua().Pop(1);

			long argumentBeanTypeId;
			String argumentName;
			if (isRequest) {
				argumentBeanTypeId = pa.ArgumentBeanTypeId;
				argumentName = "Argument";
			}
			else {
				argumentBeanTypeId = pa.ResultBeanTypeId;
				argumentName = "Result";
			}

			// see Rpc.Encode
			ByteBuffer bb = ByteBuffer.Allocate();
			bb.WriteInt4(typeId);
			int outstate;
			tangible.OutObject<Integer> tempOut_outstate = new tangible.OutObject<Integer>();
			bb.BeginWriteWithSize4(tempOut_outstate);
		outstate = tempOut_outstate.outArgValue;
			bb.WriteBool(isRequest);
			bb.WriteLong(sid);
			bb.WriteInt(resultCode);
			getLua().GetField(-1, argumentName);
			EncodeBean(bb, argumentBeanTypeId);
			getLua().Pop(1);
			bb.EndWriteWithSize4(outstate);
			socket.Send(bb);

			if (timeout > 0) {
				Util.Scheduler.getInstance().Schedule((ThisTask) -> {
					SetRpcTimeout(sid);
				}, timeout, -1);
			}
		}
		else {
			// see Protocol.Encode
			ByteBuffer bb = ByteBuffer.Allocate();
			bb.WriteInt4(typeId);
			int state;
			tangible.OutObject<Integer> tempOut_state = new tangible.OutObject<Integer>();
			bb.BeginWriteWithSize4(tempOut_state);
		state = tempOut_state.outArgValue;
			bb.WriteInt(resultCode);
			getLua().GetField(-1, "Argument");
			EncodeBean(bb, pa.ArgumentBeanTypeId);
			getLua().Pop(1);
			bb.EndWriteWithSize4(state);
			socket.Send(bb);
		}
	}

	private void EncodeBean(ByteBuffer bb, long beanTypeId) {
		if (false == getLua().IsTable(-1)) {
			throw new RuntimeException("encodebean need a table");
		}

		if (beanTypeId == Zeze.Transaction.EmptyBean.TYPEID) {
			bb.WriteInt(0);
			return;
		}
		TValue beanMeta;
		if (false == (BeanMetas.containsKey(beanTypeId) && (beanMeta = BeanMetas.get(beanTypeId)) == beanMeta)) {
			throw new RuntimeException("bean not found in meta for beanTypeId=" + beanTypeId);
		}

		// 先遍历一遍，得到填写了的var的数量
		int varsCount = 0;
		for (var v : beanMeta.Variables) {
			getLua().PushInteger(v.Id);
			getLua().GetTable(-2);
			if (false == getLua().IsNil(-1)) {
				++varsCount;
			}
			getLua().Pop(1);
		}
		bb.WriteInt(varsCount);

		for (var v : beanMeta.Variables) {
			getLua().PushInteger(v.Id);
			getLua().GetTable(-2);
			if (getLua().IsNil(-1)) { // allow var not set
				getLua().Pop(1);
				continue;
			}
			EncodeVariable(bb, v);
			getLua().Pop(1);
		}
	}

	private int EncodeGetTableLength() {
		if (false == getLua().IsTable(-1)) {
			throw new RuntimeException("EncodeGetTableLength: not a table");
		}
		int len = 0;
		getLua().PushNil();
		while (getLua().Next(-2)) {
			++len;
			getLua().Pop(1);
		}
		return len;
	}


	private void EncodeVariable(ByteBuffer _os_, VariableMeta v) {
		EncodeVariable(_os_, v, -1);
	}

//C# TO JAVA CONVERTER NOTE: Java does not support optional parameters. Overloaded method(s) are created above:
//ORIGINAL LINE: private void EncodeVariable(ByteBuffer _os_, VariableMeta v, int index = -1)
	private void EncodeVariable(ByteBuffer _os_, VariableMeta v, int index) {
		if (v.getId() > 0) { // 编码容器中项时，Id为0，此时不需要编码 tagid.
			_os_.WriteInt(v.getType() | v.getId() << ByteBuffer.TAG_SHIFT);
		}

		switch (v.getType()) {
			case ByteBuffer.BOOL:
				_os_.WriteBool(getLua().ToBoolean(index));
				break;
			case ByteBuffer.BYTE:
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: _os_.WriteByte((byte)Lua.ToInteger(index));
				_os_.WriteByte((byte)getLua().ToInteger(index));
				break;
			case ByteBuffer.SHORT:
				_os_.WriteShort((short)getLua().ToInteger(index));
				break;
			case ByteBuffer.INT:
				_os_.WriteInt((int)getLua().ToInteger(index));
				break;
			case ByteBuffer.LONG:
				_os_.WriteLong(getLua().ToInteger(index));
				break;
			case ByteBuffer.FLOAT:
				_os_.WriteFloat((float)getLua().ToNumber(index));
				break;
			case ByteBuffer.DOUBLE:
				_os_.WriteDouble(getLua().ToNumber(index));
				break;
			case ByteBuffer.STRING:
				_os_.WriteString(getLua().toString(index));
				break;
			case ByteBuffer.BYTES:
				_os_.WriteBytes(getLua().ToBuffer(index));
				break;
			case ByteBuffer.LIST: {
					if (false == getLua().IsTable(-1)) {
						throw new RuntimeException("list must be a table");
					}
					if (v.getId() <= 0) {
						throw new RuntimeException("list cannot define in collection");
					}
					int _state_;
					tangible.OutObject<Integer> tempOut__state_ = new tangible.OutObject<Integer>();
					_os_.BeginWriteSegment(tempOut__state_);
				_state_ = tempOut__state_.outArgValue;
					_os_.WriteInt(v.getValue());
					_os_.WriteInt(EncodeGetTableLength());
					getLua().PushNil();
					while (getLua().Next(-2)) {
						VariableMeta tempVar = new VariableMeta();
						tempVar.setId(0);
						tempVar.setType(v.getValue());
						tempVar.setTypeBeanTypeId(v.getValueBeanTypeId());
						EncodeVariable(_os_, tempVar);
						getLua().Pop(1);
					}
					_os_.EndWriteSegment(_state_);
			}
				break;
			case ByteBuffer.SET: {
					if (false == getLua().IsTable(-1)) {
						throw new RuntimeException("set must be a table");
					}
					if (v.getId() <= 0) {
						throw new RuntimeException("set cannot define in collection");
					}
					int _state_;
					tangible.OutObject<Integer> tempOut__state_2 = new tangible.OutObject<Integer>();
					_os_.BeginWriteSegment(tempOut__state_2);
				_state_ = tempOut__state_2.outArgValue;
					_os_.WriteInt(v.getValue());
					_os_.WriteInt(EncodeGetTableLength());
					getLua().PushNil();
					while (getLua().Next(-2)) {
						getLua().Pop(1); // set：encode key
						VariableMeta tempVar2 = new VariableMeta();
						tempVar2.setId(0);
						tempVar2.setType(v.getValue());
						tempVar2.setTypeBeanTypeId(v.getValueBeanTypeId());
						EncodeVariable(_os_, tempVar2);
					}
					_os_.EndWriteSegment(_state_);
			}
				break;
			case ByteBuffer.MAP: {
					if (false == getLua().IsTable(-1)) {
						throw new RuntimeException("map must be a table");
					}
					if (v.getId() <= 0) {
						throw new RuntimeException("map cannot define in collection");
					}
					int _state_;
					tangible.OutObject<Integer> tempOut__state_3 = new tangible.OutObject<Integer>();
					_os_.BeginWriteSegment(tempOut__state_3);
				_state_ = tempOut__state_3.outArgValue;
					_os_.WriteInt(v.getKey());
					_os_.WriteInt(v.getValue());
					_os_.WriteInt(EncodeGetTableLength());
					getLua().PushNil();
					while (getLua().Next(-2)) {
						VariableMeta tempVar3 = new VariableMeta(), -2);
						tempVar3.setId(0);
						tempVar3.setType(v.getKey());
						tempVar3.setTypeBeanTypeId(v.getKeyBeanTypeId());
						EncodeVariable(_os_, tempVar3, -2);
						VariableMeta tempVar4 = new VariableMeta();
						tempVar4.setId(0);
						tempVar4.setType(v.getValue());
						tempVar4.setTypeBeanTypeId(v.getValueBeanTypeId());
						EncodeVariable(_os_, tempVar4);
						getLua().Pop(1);
					}
					_os_.EndWriteSegment(_state_);
			}
				break;
			case ByteBuffer.BEAN: {
					if (v.getId() > 0) {
						int _state_;
						tangible.OutObject<Integer> tempOut__state_4 = new tangible.OutObject<Integer>();
						_os_.BeginWriteSegment(tempOut__state_4);
					_state_ = tempOut__state_4.outArgValue;
						EncodeBean(_os_, v.getTypeBeanTypeId());
						_os_.EndWriteSegment(_state_);
					}
					else {
						// in collection. direct encode
						EncodeBean(_os_, v.getTypeBeanTypeId());
					}
			}
				break;
			case ByteBuffer.DYNAMIC: {
					if (v.getId() <= 0) {
						throw new RuntimeException("dynamic cannot define in collection");
					}
					getLua().GetField(-1, "_TypeId_");
					if (getLua().IsNil(-1)) {
						throw new RuntimeException("'_TypeId_' not found. dynamic bean needed.");
					}
					long beanTypeId = getLua().ToInteger(-1);
					getLua().Pop(1);

					var funcName = String.format("Zeze_GetRealBeanTypeIdFromSpecial_%1$s_%2$s", v.getBeanMeta().getName(), v.getName());
					if (LuaType.Function != this.getLua().GetGlobal(funcName)) { // push func onto stack
						getLua().Pop(1);
						throw new RuntimeException(String.format("%1$s is not a function", funcName));
					}
					getLua().PushInteger(beanTypeId);
					getLua().Call(1, 1);
					var realBeanTypeId = getLua().ToInteger(-1);
					getLua().Pop(1);

					_os_.WriteLong8(beanTypeId);
					int _state_;
					tangible.OutObject<Integer> tempOut__state_5 = new tangible.OutObject<Integer>();
					_os_.BeginWriteSegment(tempOut__state_5);
				_state_ = tempOut__state_5.outArgValue;
					EncodeBean(_os_, realBeanTypeId);
					_os_.EndWriteSegment(_state_);
			}
				break;
			default:
				throw new RuntimeException("Unkown Tag Type");
		}
	}

	private void CallRpcTimeout(long sid) {
		if (LuaType.Function != this.getLua().GetGlobal("ZezeDispatchProtocol")) { // push func onto stack
			getLua().Pop(1);
			return;
		}
		// see Zeze.lua ：ZezeDispatchProtocol。这里仅设置必要参数。
		getLua().CreateTable(0, 16);

		getLua().PushString("IsRpc");
		getLua().PushBoolean(true);
		getLua().SetTable(-3);

		getLua().PushString("IsRequest");
		getLua().PushBoolean(false);
		getLua().SetTable(-3);

		getLua().PushString("Sid");
		getLua().PushInteger(sid);
		getLua().SetTable(-3);

		getLua().PushString("IsTimeout");
		getLua().PushBoolean(true);
		getLua().SetTable(-3);

		getLua().Call(1, 1);
		getLua().Pop(1);
	}

	public final boolean DecodeAndDispatch(Net.Service service, long sessionId, int typeId, ByteBuffer _os_) {
		if (LuaType.Function != this.getLua().GetGlobal("ZezeDispatchProtocol")) { // push func onto stack
			getLua().Pop(1);
			return false;
		}

		TValue pa;
		if (false == (ProtocolMetas.containsKey(typeId) && (pa = ProtocolMetas.get(typeId)) == pa)) {
			throw new RuntimeException("protocol not found in meta for typeid=" + typeId);
		}

		// 现在不支持 Rpc.但是代码没有检查。
		// 生成的时候报错。
		getLua().CreateTable(0, 16);

		boolean tempVar = service instanceof FromLua;
		FromLua fromLua = tempVar ? (FromLua)service : null;
		if (tempVar) { // 必须是，不报错了。
			getLua().PushString("Service");
			getLua().PushObject(fromLua);
			getLua().SetTable(-3);
		}

		getLua().PushString("SessionId");
		getLua().PushInteger(sessionId);
		getLua().SetTable(-3);

		getLua().PushString("ModuleId");
//C# TO JAVA CONVERTER WARNING: The right shift operator was not replaced by Java's logical right shift operator since the left operand was not confirmed to be of an unsigned type, but you should review whether the logical right shift operator (>>>) is more appropriate:
		getLua().PushInteger((typeId >> 16) & 0xffff);
		getLua().SetTable(-3);

		getLua().PushString("ProtcolId");
		getLua().PushInteger(typeId & 0xffff);
		getLua().SetTable(-3);

		getLua().PushString("TypeId");
		getLua().PushInteger(typeId);
		getLua().SetTable(-3);

		if (pa.IsRpc) {
			boolean IsRequest = _os_.ReadBool();
			long sid = _os_.ReadLong();
			int resultCode = _os_.ReadInt();
			String argument;
			if (IsRequest) {
				argument = "Argument";
			}
			else {
				argument = "Result";
			}
			getLua().PushString("IsRpc");
			getLua().PushBoolean(true);
			getLua().SetTable(-3);
			getLua().PushString("IsRequest");
			getLua().PushBoolean(IsRequest);
			getLua().SetTable(-3);
			getLua().PushString("Sid");
			getLua().PushInteger(sid);
			getLua().SetTable(-3);
			getLua().PushString("ResultCode");
			getLua().PushInteger(resultCode);
			getLua().SetTable(-3);
			getLua().PushString(argument);
			DecodeBean(_os_);
			getLua().SetTable(-3);
		}
		else {
			getLua().PushString("ResultCode");
			getLua().PushInteger(_os_.ReadInt());
			getLua().SetTable(-3);
			getLua().PushString("Argument");
			DecodeBean(_os_);
			getLua().SetTable(-3);
		}

		getLua().Call(1, 1);
		boolean result = false;
		if (false == getLua().IsNil(-1)) {
			result = getLua().ToBoolean(-1);
		}
		getLua().Pop(1);
		return result;
	}

	private void DecodeBean(ByteBuffer _os_) {
		getLua().CreateTable(0, 32);
		for (int _varnum_ = _os_.ReadInt(); _varnum_ > 0; --_varnum_) {
			int _tagid_ = _os_.ReadInt();
//C# TO JAVA CONVERTER WARNING: The right shift operator was not replaced by Java's logical right shift operator since the left operand was not confirmed to be of an unsigned type, but you should review whether the logical right shift operator (>>>) is more appropriate:
			int _varid_ = (_tagid_ >> ByteBuffer.TAG_SHIFT) & ByteBuffer.ID_MASK;
			int _tagType_ = _tagid_ & ByteBuffer.TAG_MASK;
			getLua().PushInteger(_varid_);
			DecodeVariable(_os_, _tagType_);
			getLua().SetTable(-3);
		}
	}


	private void DecodeVariable(ByteBuffer _os_, int _tagType_) {
		DecodeVariable(_os_, _tagType_, false);
	}

//C# TO JAVA CONVERTER NOTE: Java does not support optional parameters. Overloaded method(s) are created above:
//ORIGINAL LINE: private void DecodeVariable(ByteBuffer _os_, int _tagType_, bool inCollection = false)
	private void DecodeVariable(ByteBuffer _os_, int _tagType_, boolean inCollection) {
		switch (_tagType_) {
			case ByteBuffer.BOOL:
				getLua().PushBoolean(_os_.ReadBool());
				break;
			case ByteBuffer.BYTE:
				getLua().PushInteger(_os_.ReadByte());
				break;
			case ByteBuffer.SHORT:
				getLua().PushInteger(_os_.ReadShort());
				break;
			case ByteBuffer.INT:
				getLua().PushInteger(_os_.ReadInt());
				break;
			case ByteBuffer.LONG:
				getLua().PushInteger(_os_.ReadLong());
				break;
			case ByteBuffer.FLOAT:
				getLua().PushNumber(_os_.ReadFloat());
				break;
			case ByteBuffer.DOUBLE:
				getLua().PushNumber(_os_.ReadDouble());
				break;
			case ByteBuffer.STRING:
				getLua().PushString(_os_.ReadString());
				break;
			case ByteBuffer.BYTES:
				getLua().PushBuffer(_os_.ReadBytes());
				break;
			case ByteBuffer.LIST: {
					int _state_;
					tangible.OutObject<Integer> tempOut__state_ = new tangible.OutObject<Integer>();
					_os_.BeginReadSegment(tempOut__state_);
				_state_ = tempOut__state_.outArgValue;
					int _valueTagType_ = _os_.ReadInt();
					getLua().CreateTable(128, 128); // 不知道用哪个参数。
					int i = 1; // 从1开始？
					for (int _size_ = _os_.ReadInt(); _size_ > 0; --_size_) {
						getLua().PushInteger(i);
						DecodeVariable(_os_, _valueTagType_, true);
						getLua().SetTable(-3);
						++i;
					}
					_os_.EndReadSegment(_state_);
			}
				break;
			case ByteBuffer.SET: {
					int _state_;
					tangible.OutObject<Integer> tempOut__state_2 = new tangible.OutObject<Integer>();
					_os_.BeginReadSegment(tempOut__state_2);
				_state_ = tempOut__state_2.outArgValue;
					int _valueTagType_ = _os_.ReadInt();
					getLua().CreateTable(128, 128); // 不知道用哪个参数。
					int i = 1;
					for (int _size_ = _os_.ReadInt(); _size_ > 0; --_size_) {
						DecodeVariable(_os_, _valueTagType_, true);
						getLua().PushInteger(0);
						getLua().SetTable(-3);
						++i;
					}
					_os_.EndReadSegment(_state_);
			}
				break;
			case ByteBuffer.MAP: {
					int _state_;
					tangible.OutObject<Integer> tempOut__state_3 = new tangible.OutObject<Integer>();
					_os_.BeginReadSegment(tempOut__state_3);
				_state_ = tempOut__state_3.outArgValue;
					int _keyTagType_ = _os_.ReadInt();
					int _valueTagType_ = _os_.ReadInt();
					getLua().CreateTable(128, 0);
					for (int _size_ = _os_.ReadInt(); _size_ > 0; --_size_) {
						DecodeVariable(_os_, _keyTagType_, true);
						DecodeVariable(_os_, _valueTagType_, true);
						getLua().SetTable(-3);
					}
					_os_.EndReadSegment(_state_);
			}
				break;
			case ByteBuffer.BEAN: {
					if (inCollection) {
						DecodeBean(_os_);
					}
					else {
						int _state_;
						tangible.OutObject<Integer> tempOut__state_4 = new tangible.OutObject<Integer>();
						_os_.BeginReadSegment(tempOut__state_4);
					_state_ = tempOut__state_4.outArgValue;
						DecodeBean(_os_);
						_os_.EndReadSegment(_state_);
					}
			}
				break;
			case ByteBuffer.DYNAMIC: {
					long beanTypeId = _os_.ReadLong8();
					if (beanTypeId == Transaction.EmptyBean.TYPEID) {
						// 这个EmptyBean完全没有实现Encode,Decode，没有遵守Bean的系列化协议，所以需要特殊处理一下。
						int _state_;
						tangible.OutObject<Integer> tempOut__state_5 = new tangible.OutObject<Integer>();
						_os_.BeginReadSegment(tempOut__state_5);
					_state_ = tempOut__state_5.outArgValue;
						_os_.EndReadSegment(_state_);
						getLua().CreateTable(0, 0);
					}
					else {
						int _state_;
						tangible.OutObject<Integer> tempOut__state_6 = new tangible.OutObject<Integer>();
						_os_.BeginReadSegment(tempOut__state_6);
					_state_ = tempOut__state_6.outArgValue;
						DecodeBean(_os_);
						_os_.EndReadSegment(_state_);
					}
					// 动态bean额外把TypeId加到变量里面。总是使用varid==0表示。程序可以使用这个动态判断是哪个具体的bean。
					getLua().PushInteger(0);
					getLua().PushInteger(beanTypeId);
					getLua().SetTable(-3);
			}
				break;
			default:
				throw new RuntimeException("Unkown Tag Type");
		}
	}

	private HashMap<Long, ByteBuffer> ToLuaBuffer = new HashMap<Long, ByteBuffer>();
	private HashMap<Long, FromLua> ToLuaHandshakeDone = new HashMap<Long, FromLua>();
	private HashMap<Long, FromLua> ToLuaSocketClose = new HashMap<Long, FromLua>();
	private HashSet<Long> ToLuaRpcTimeout = new HashSet<Long>();

	public final void SetRpcTimeout(long sid) {
		synchronized (this) {
			ToLuaRpcTimeout.add(sid);
		}
	}

	public final void SetHandshakeDone(long socketSessionId, FromLua service) {
		synchronized (this) {
			ToLuaHandshakeDone.put(socketSessionId, service);
		}
	}

	public final void SetSocketClose(long socketSessionId, FromLua service) {
		synchronized (this) {
			ToLuaSocketClose.put(socketSessionId, service);
		}
	}

	public final void AppendInputBuffer(long socketSessionId, ByteBuffer buffer) {
		synchronized (this) {
			TValue exist;
			if (ToLuaBuffer.containsKey(socketSessionId) && (exist = ToLuaBuffer.get(socketSessionId)) == exist) {
				exist.Append(buffer.getBytes(), buffer.getReadIndex(), buffer.getSize());
				return;
			}
			ByteBuffer newBuffer = ByteBuffer.Allocate();
			ToLuaBuffer.put(socketSessionId, newBuffer);
			newBuffer.Append(buffer.getBytes(), buffer.getReadIndex(), buffer.getSize());
		}
	}

	public final void Update(Net.Service service) {
		HashMap<Long, FromLua> handshakeTmp;
		HashMap<Long, FromLua> socketCloseTmp;
		HashMap<Long, Serialize.ByteBuffer> inputTmp;
		HashSet<Long> rpcTimeout;
		synchronized (this) {
			handshakeTmp = ToLuaHandshakeDone;
			socketCloseTmp = ToLuaSocketClose;
			inputTmp = ToLuaBuffer;
			rpcTimeout = ToLuaRpcTimeout;
			ToLuaBuffer = new HashMap<Long, ByteBuffer>();
			ToLuaHandshakeDone = new HashMap<Long, FromLua>();
			ToLuaSocketClose = new HashMap<Long, FromLua>();
			ToLuaRpcTimeout = new HashSet<Long>();
		}

		for (var e : socketCloseTmp.entrySet()) {
			this.CallSocketClose(e.getValue(), e.getKey());
		}

		for (var e : handshakeTmp.entrySet()) {
			this.CallHandshakeDone(e.getValue(), e.getKey());
		}

		for (var sid : rpcTimeout) {
			this.CallRpcTimeout(sid);
		}

		for (var e : inputTmp.entrySet()) {
			AsyncSocket sender = service.GetSocket(e.getKey());
			if (null == sender) {
				continue;
			}

			Net.Protocol.Decode(service, sender, e.getValue(), this);
		}

		synchronized (this) {
			for (var e : inputTmp.entrySet()) {
				if (e.getValue().Size <= 0) {
					continue; // 数据全部处理完成。
				}

				e.getValue().Campact();
				TValue exist;
				if (ToLuaBuffer.containsKey(e.getKey()) && (exist = ToLuaBuffer.get(e.getKey())) == exist) {
					// 处理过程中有新数据到来，加到当前剩余数据后面，然后覆盖掉buffer。
					e.getValue().Append(exist.Bytes, exist.ReadIndex, exist.Size);
					ToLuaBuffer.put(e.getKey(), e.getValue());
				}
				else {
					// 没有新数据到来，有剩余，加回去。下一次update再处理。
					ToLuaBuffer.put(e.getKey(), e.getValue());
				}
			}
		}
	}
}