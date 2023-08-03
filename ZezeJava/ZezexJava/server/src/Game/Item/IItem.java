package Game.Item;

// item 继承体系演示。
// 有多层，
// 纯接口化以后，
// 上面层次的bean初始化有问题，没法使用abstract class了。
// 需要最终层完全实现所有方法。
// 现在只是演示，先这样了。
public interface IItem {
	int getId();
	boolean use();
	// 物品提示信息格式化。需要定义通用的 Tip 结构
	String formatTip();
	void calculateFighter(Game.Fight.IFighter fighter);
}
