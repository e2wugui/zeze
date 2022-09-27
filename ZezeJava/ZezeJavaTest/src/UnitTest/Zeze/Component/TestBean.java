package UnitTest.Zeze.Component;

import java.util.function.LongFunction;
import java.util.function.ToLongFunction;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.DynamicBean;
import Zeze.Transaction.Record;

public class TestBean extends DynamicBean {

	private boolean isServerLiving = true; // 模拟服务器连接是否存活

	private int testValue;

	public TestBean() {
		super(0, new ToLongFunction<Bean>() {
			@Override
			public long applyAsLong(Bean value) {
				return 0;
			}
		}, new LongFunction<Bean>() {
			@Override
			public Bean apply(long value) {
				return null;
			}
		});
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

	public void loseConnection(){
		isServerLiving = false;
	}

	@Override
	public void Encode(ByteBuffer bb) {
		super.Encode(bb);
	}

	@Override
	public void Decode(ByteBuffer bb) {
		super.Decode(bb);
	}
}
