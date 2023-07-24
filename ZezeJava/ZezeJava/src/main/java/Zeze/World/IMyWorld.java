package Zeze.World;

import Zeze.Builtin.World.Static.BSwitchWorld;
import Zeze.World.Mmo.AoiSimple;

/**
 * createXXX 框架创建对象时调用。当需要创建自己的重载类，或者自定义初始化参数，等等，重新重载实现。
 * onCreateXXX 一般用于初始化对象。
 */
public interface IMyWorld {
	/**
	 * 创建新的Entity。重载Entity类时需要重载这个方法。
	 * @param entityId entityId
	 * @return entity
	 */
	default Entity createEntity(long entityId) {
		// 所有的实体都通过这个方法创建。用来创建自定义的Entity子类。
		// 【注意】实体的初始化框架会进行区分，分别调用下面的onCreatePlayer等，这里不需要调用。
		return new Entity(entityId);
	}

	/**
	 * 重载并初始化玩家数据。
	 * 一般需要初始化position,direct。
	 *
	 * @param switchWorld 切换地图参数，用来初始化玩家切换地图的初始位置。
	 * @param player player
	 */
	default void onCreatePlayer(Entity player, BSwitchWorld.Data switchWorld) {

	}

	/**
	 * 可重载，用来创建扩展的Cube类。
	 *
	 * @param index index
	 * @return new cube
	 */
	default Cube createCube(CubeMap map, CubeIndex index) {
		var cube = new Cube(index, map);
		onCreateCube(cube);
		return cube;
	}

	/**
	 * 用来初始化Cube。
	 * 场景编辑器把里面的实体按Map<CubeIndex, List<EntityConf>>方式组织数据。
	 * 根据玩家的活动，根据需要，一个一个的初始化Cube。
	 *
	 * @param cube cube
	 */
	default void onCreateCube(Cube cube) {
		/*
		 * 初始化Cube的基本操作是装载配置的Entity；
		 * 然后根据Entity的数据类型，进一步初始化相关服务，比如AI。
		 * 最灵活的服务器初始化是在这里做。
		 *
		 * 基础服务功能增加或者定义出抽象接口以后，再细化Cube初始化的模式。
		 */
	}

	/**
	 * 创建Map实例。
	 * 目前Map本身不需要初始化，它动态创建Cube并初始化Cube。
	 * @return 返回新建的CubeMap实例。
	 */
	default CubeMap createMap(IMapManager mapManager, long instanceId) {
		var map = new CubeMap(mapManager, instanceId, 64, 64);
		map.setAoi(new AoiSimple(mapManager.getWorld(), map, 1, 1));
		return map;
	}
}