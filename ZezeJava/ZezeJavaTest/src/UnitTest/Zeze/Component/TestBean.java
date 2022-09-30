package UnitTest.Zeze.Component;

import Zeze.Transaction.DynamicBean;

public class TestBean extends DynamicBean {
	private boolean isServerLiving = true; // 模拟服务器连接是否存活

	private int testValue;

	public TestBean() {
		super(0, value -> 0, value -> null);
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
}
