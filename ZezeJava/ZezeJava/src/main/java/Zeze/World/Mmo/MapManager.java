package Zeze.World.Mmo;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Arch.ProviderUserSession;
import Zeze.Builtin.Provider.BModule;
import Zeze.Builtin.Provider.Bind;
import Zeze.Builtin.World.BCommand;
import Zeze.Builtin.World.BEnterConfirm;
import Zeze.Builtin.World.BEnterWorld;
import Zeze.Builtin.World.BLoad;
import Zeze.Builtin.World.BLoadMap;
import Zeze.Builtin.World.Command;
import Zeze.Component.AutoKey;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.Vector3;
import Zeze.Services.ServiceManager.BSubscribeInfo;
import Zeze.Transaction.Procedure;
import Zeze.World.CubeMap;
import Zeze.World.Entity;
import Zeze.World.ICommand;
import Zeze.World.IMapManager;
import Zeze.World.LockGuard;
import Zeze.World.World;

public class MapManager implements IMapManager, ICommand {
	private final AutoKey autoKeyMap;
	private final AutoKey autoKeyEntity;
	private final ConcurrentHashMap<Long, CubeMap> cubeMaps = new ConcurrentHashMap<>();
	public final World world;

	// 【本地】
	// 1. 间隔短或者即时的负载统计。
	// 2. 非事务。需要多线程保护。
	// 3. 较长间隔，定时写入全局表。【需要copy传入事务】
	protected final ConcurrentHashMap<Integer, BLoadMap> maps = new ConcurrentHashMap<>();
	protected final BLoad serverLoadSum = new BLoad();
	private volatile int maxServerPlayer = 50000;
	private volatile int maxMapPlayer = 50000;
	private volatile int maxMapInstancePlayer = 50000;

	public void setMaxServerPlayer(int value) {
		maxServerPlayer = value;
	}

	public void setMaxMapPlayer(int value) {
		maxMapPlayer = value;
	}

	public void setMaxMapInstancePlayer(int value) {
		maxMapInstancePlayer = value;
	}

	// player count 是个瞬时当前值，使用set方式修改。
	public synchronized void setLoadPlayer(int mapId, long mapInstanceId, int count) {
		var mapLoad = maps.get(mapId);
		if (null != mapLoad) {
			var instanceLoad = mapLoad.getInstances().get(mapInstanceId);
			if (null != instanceLoad) {
				var diff = count - instanceLoad.getPlayerCount();
				instanceLoad.setPlayerCount(count);
				var sumOld = mapLoad.getLoadSum().getPlayerCount();
				mapLoad.getLoadSum().setPlayerCount(sumOld + diff);
				var serverSumOld = serverLoadSum.getPlayerCount();
				serverLoadSum.setPlayerCount(serverSumOld + diff);
			}
		}
	}

	// compute count 是个累计值，只增不减。
	public synchronized void addLoadCompute(int mapId, long mapInstanceId) {
		var mapLoad = maps.get(mapId);
		if (null != mapLoad) {
			var instanceLoad = mapLoad.getInstances().get(mapInstanceId);
			if (null != instanceLoad) {
				instanceLoad.setComputeCount(instanceLoad.getComputeCount() + 1);
				mapLoad.getLoadSum().setComputeCount(mapLoad.getLoadSum().getComputeCount() + 1);
				serverLoadSum.setComputeCount(serverLoadSum.getComputeCount() + 1);
			}
		}
	}

	public MapManager(World world) {
		this.autoKeyMap = world.providerApp.zeze.getAutoKey("Zeze.World.AutoKeyMap");
		this.autoKeyEntity = world.providerApp.zeze.getAutoKey("Zeze.World.AutoKeyEntity");
		this.world = world;

		world.internalRegisterCommand(BCommand.eEnterConfirm, this);
	}

	@Override
	public long handle(String account, String playerId, Command c) throws Exception {
		switch (c.Argument.getCommandId()) {
		case BCommand.eEnterConfirm:
			return onEnterConfirm(account, playerId, ICommand.decode(new BEnterConfirm.Data(), c));

		}
		return 0;
	}

	private long onEnterConfirm(String account, String playerId, BEnterConfirm.Data arg) throws IOException {
		var map = cubeMaps.get(arg.getMapInstanceId());
		if (null == map)
			return Procedure.LogicError; // instance to found

		return map.getAoi().enter(arg.getEntityId());
	}

	@Override
	public CubeMap createMap() {
		var instanceId = autoKeyMap.nextId();
		return cubeMaps.computeIfAbsent(instanceId, (key) -> {
			var map = new CubeMap(instanceId, 64, 64);
			map.setAoi(new AoiSimple(world, map, 1, 1));
			return map;
		});
	}

	public Entity createEntity() {
		return new Entity(autoKeyEntity.nextId());
	}

	// todo，简版，仅考虑玩家数量，1. 待完善。 2. 可重载自定义。3. 创建地图现在在锁内，可能需要优化。
	protected synchronized CubeMap findOrCreateMapInstance(int mapId) {
		if (serverLoadSum.getPlayerCount() > maxServerPlayer) // 本服总玩家限制。
			return null;

		var bLoadMap = maps.get(mapId);
		if (null == bLoadMap) {
			return createMap();
		}

		if (bLoadMap.getLoadSum().getPlayerCount() > maxMapPlayer) // 地图总玩家限制
			return null;

		// 地图实例选择，按顺序查找，塞满一个算一个。【有利于玩家扎堆】
		for (var e : bLoadMap.getInstances().entrySet()) {
			if (e.getValue().getPlayerCount() > maxMapInstancePlayer) // 地图实例玩家限制
				continue;
			return cubeMaps.get(e.getKey());
		}
		return createMap();
	}

	protected void linkBind(String linkName, long linkSid) {
		var bind = new Bind();

		bind.Argument.getLinkSids().add(linkSid);

		var bModule = world.providerApp.dynamicModules.get(world.getId());
		if (null == bModule) {
			bModule = new BModule.Data();
			bModule.setChoiceType(BModule.ChoiceTypeDefault);
			bModule.setConfigType(BModule.ConfigTypeDynamic);
			bModule.setSubscribeType(BSubscribeInfo.SubscribeTypeSimple);
		}
		bind.Argument.getModules().put(world.getId(), bModule);

		world.getLinkSender().sendLink(linkName, bind.encode());
	}

	// leave after bind
	// current map instance. local leave aoi-notify.... leaveWorld(). enterMap(newMapId)
	@Override
	public long enterMap(ProviderUserSession session, int mapId, Vector3 position) throws Exception {
		// find local
		var instanceMap = findOrCreateMapInstance(mapId);
		if (null != instanceMap) {
			var entity = createEntity(); // todo 上下文参数

			// 加入 cube map
			var cube = instanceMap.getOrAdd(instanceMap.toIndex(position));
			try (var ignored = new LockGuard(cube)) {
				cube.pending.put(entity.getId(), entity);
				entity.internalSetCube(cube);
				instanceMap.indexes.put(entity.getId(), cube);
			}

			// link bind
			var linkName = session.getLinkName();
			var linkSid = session.getLinkSid();
			linkBind(linkName, linkSid);

			// send enter world to client
			var param = new BEnterWorld.Data();
			param.setMapId(mapId);
			param.setMapInstanceId(instanceMap.getInstanceId());
			param.setEntityId(entity.getId());

			// todo param
			param.setPosition(Vector3.ZERO);
			param.setDirect(Vector3.ZERO);

			world.getLinkSender().sendCommand(linkName, linkSid, BCommand.eEnterWorld, param);
			return instanceMap.getInstanceId();
		}

		// todo find global
		throw new RuntimeException("find global not implement.");
	}

	@Override
	public CubeMap getMap(long instanceId) {
		return cubeMaps.get(instanceId);
	}
}
