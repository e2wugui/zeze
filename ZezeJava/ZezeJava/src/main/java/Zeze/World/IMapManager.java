package Zeze.World;

import Zeze.Arch.ProviderUserSession;
import Zeze.Builtin.World.Static.BSwitchWorld;

/**
 * 地图实例管理接口。
 * 默认实现是 Aoi.MapManager。
 * 可重用性高，一般不需要重写。
 */
public interface IMapManager {
	World getWorld();
	CubeMap createMap();

	/**
	 * 地图切换。
	 * @param switchWorld 地图切换参数，直接使用SwitchWorld的参数，便于扩展。
	 * @return error code, 0 success
	 */
	long enterMap(ProviderUserSession session, BSwitchWorld.Data switchWorld) throws Exception;

	/**
	 * 根据地图实例Id查找CubeMap实例。
	 * @param instanceId 地图实例Id。
	 * @return 返回CubeMap。
	 */
	CubeMap getMap(long instanceId);
}
