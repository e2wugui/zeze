package Zeze.World;

import Zeze.Arch.ProviderUserSession;
import Zeze.Serialize.Vector3;

/**
 * 地图实例管理接口。
 * 默认实现是 Aoi.MapManager。
 * 可重用性高，一般不需要重写。
 */
public interface IMapManager {
	/**
	 * 地图切换。
	 * @param mapId 地图配置id。
	 * @return error code, 0 success
	 */
	long enterMap(ProviderUserSession session, int mapId);

	/**
	 * 根据地图实例Id查找CubeMap实例。
	 * @param instanceId 地图实例Id。
	 * @return 返回CubeMap。
	 */
	CubeMap getCubeMap(long instanceId);
}
