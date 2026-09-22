package Game.Fight;

public interface IFighter {
	//BFighterId getId(); // 用来演示接口里面可以用Bean。

	// 下面的演示包装Bean的方法。Bean支持在接口里面使用后，可以一个简单的
	// getBean 搞定。

	float getAttack();
	float getDefence();
	void setAttack(float value);
	void setDefence(float value);
}
