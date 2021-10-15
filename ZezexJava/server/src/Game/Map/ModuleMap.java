package Game.Map;

import Zeze.Transaction.*;
import Game.*;

//ZEZE_FILE_CHUNK {{{ IMPORT GEN
//ZEZE_FILE_CHUNK }}} IMPORT GEN

public final class ModuleMap extends AbstractModule {
	public void Start(App app) {
	}

	public void Stop(App app) {
	}

	@Override
	public int ProcessCEnterWorld(CEnterWorld protocol) {
		Game.Login.Session session = Game.Login.Session.Get(protocol);
		if (session.getRoleId().equals(null)) {
			return Procedure.LogicError;
		}

		// TODO map
		return Procedure.NotImplement;
	}

	@Override
	public int ProcessCEnterWorldDone(CEnterWorldDone protocol) {
		// TODO map
		return Procedure.NotImplement;
	}

	// ZEZE_FILE_CHUNK {{{ GEN MODULE
	public static final int ModuleId = 8;


	private App App;
	public App getApp() {
		return App;
	}

	public ModuleMap(App app) {
		App = app;
		// register protocol factory and handles
		getApp().getServer().AddFactoryHandle(546916, new Zeze.Net.Service.ProtocolFactoryHandle() {Factory = () -> new Game.Map.CEnterWorld(), Handle = Zeze.Net.Service.<CEnterWorld>MakeHandle(this, this.getClass().getMethod("ProcessCEnterWorld"))});
		getApp().getServer().AddFactoryHandle(537032, new Zeze.Net.Service.ProtocolFactoryHandle() {Factory = () -> new Game.Map.CEnterWorldDone(), Handle = Zeze.Net.Service.<CEnterWorldDone>MakeHandle(this, this.getClass().getMethod("ProcessCEnterWorldDone"))});
		// register table
	}

	@Override
	public void UnRegister() {
		TValue _;
		tangible.OutObject<Zeze.Net.Service.ProtocolFactoryHandle> tempOut__ = new tangible.OutObject<Zeze.Net.Service.ProtocolFactoryHandle>();
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
		getApp().getServer().getFactorys().TryRemove(546916, tempOut__);
	_ = tempOut__.outArgValue;
		TValue _;
		tangible.OutObject<Zeze.Net.Service.ProtocolFactoryHandle> tempOut__2 = new tangible.OutObject<Zeze.Net.Service.ProtocolFactoryHandle>();
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
		getApp().getServer().getFactorys().TryRemove(537032, tempOut__2);
	_ = tempOut__2.outArgValue;
	}
	// ZEZE_FILE_CHUNK }}} GEN MODULE
}