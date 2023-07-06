package Zeze.World;

public interface IMapManager {
	/**
	 * 地图切换。
	 * @param mapId 地图配置id。
	 * @return error code, 0 success
	 */
	long enterMap(int mapId);

	/**
	 * 根据地图实例Id查找CubeMap实例。
	 * @param instanceId 地图实例Id。
	 * @return 返回CubeMap。
	 */
	CubeMap getCubeMap(long instanceId);
}
