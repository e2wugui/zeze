package UnitTest.Zeze.Component;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Bean;
import org.jetbrains.annotations.NotNull;

public class TestBean extends Bean {
	private boolean isServerLiving = true; // 模拟服务器连接是否存活

	private int testValue;

	public TestBean() {
		testValue = 0;
	}

	public void addValue() {
		testValue++;
	}

	public boolean checkLiving() {
		return isServerLiving;
	}

	public int getTestValue() {
		return testValue;
	}

	public void loseConnection() {
		isServerLiving = false;
	}

	@Override
	public void encode(@NotNull ByteBuffer bb) {
		bb.WriteInt(testValue);
	}

	@Override
	public void decode(@NotNull ByteBuffer bb) {
		testValue = bb.ReadInt();
	}
}
