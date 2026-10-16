package Zeze.Component;

import org.jspecify.annotations.Nullable;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Util.TaskCompletionSource;

public class TestBean extends Bean {
	private boolean isServerLiving = true; // 模拟服务器连接是否存活

	private @Nullable TaskCompletionSource<Boolean> timerFuture2;
	private int hope;

	public void resetFuture(int hope) {
		timerFuture2 = new TaskCompletionSource<>();
		this.hope = hope;
	}

	public TaskCompletionSource<Boolean> getFuture() {
		return java.util.Objects.requireNonNull(timerFuture2);
	}

	private int testValue;

	public TestBean() {
		testValue = 0;
	}

	public void addValue() {
		testValue++;
		if (testValue >= hope && null != timerFuture2)
			timerFuture2.setResult(true);
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
	public void encode(ByteBuffer bb) {
		bb.WriteInt(testValue);
	}

	@Override
	public void decode(IByteBuffer bb) {
		testValue = bb.ReadInt();
	}
}
