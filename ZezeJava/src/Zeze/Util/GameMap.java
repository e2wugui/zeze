package Zeze.Util;

import Zeze.*;
import java.util.*;

public class GameMap extends CubeIndexMap<GameCube, GameObjectId> {

	public GameMap(int cubeSizeX, int cubeSizeY) {
		this(cubeSizeX, cubeSizeY, 256);
	}

	public GameMap(int cubeSizeX) {
		this(cubeSizeX, 256, 256);
	}

	public GameMap() {
		this(256, 256, 256);
	}

//C# TO JAVA CONVERTER NOTE: Java does not support optional parameters. Overloaded method(s) are created above:
//ORIGINAL LINE: public GameMap(int cubeSizeX = 256, int cubeSizeY = 256, int cubeSizeZ = 256)
	public GameMap(int cubeSizeX, int cubeSizeY, int cubeSizeZ) {
		super(cubeSizeX, cubeSizeY, cubeSizeZ);

	}
}