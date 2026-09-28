package Zeze.Util;

// 游戏地图：CubeIndexMap 的三维格子空间，管理玩家/物体所在格与近邻查询
public class GameMap extends CubeIndexMap<GameCube, GameObjectId> {
	public GameMap(Factory<GameCube> factory, int cubeSizeX, int cubeSizeY) {
		this(factory, cubeSizeX, cubeSizeY, 256);
	}

	public GameMap(Factory<GameCube> factory, int cubeSizeX) {
		this(factory, cubeSizeX, 256, 256);
	}

	public GameMap(Factory<GameCube> factory) {
		this(factory, 256, 256, 256);
	}

	public GameMap(Factory<GameCube> factory, int cubeSizeX, int cubeSizeY, int cubeSizeZ) {
		super(factory, cubeSizeX, cubeSizeY, cubeSizeZ);
	}
}
