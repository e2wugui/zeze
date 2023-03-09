package Game.Item;

public abstract class Item {
	protected final Game.Equip.BItem bItem; // bItem.Position: 根据所在容器不同，含义可能不一样：比如在包裹中是格子号，在装备中就是装备位置。

	public Item(Game.Equip.BItem bItem) {
		this.bItem = bItem;
	}

	public final int getId() {
		return bItem.getId();
	}

	public abstract boolean Use();

	// 物品提示信息格式化。需要定义通用的 Tip 结构
	public String FormatTip() {
		return "";
	}

	public void CalculateFighter(Game.Fight.Fighter fighter) {
	}

	// 其他更多操作？
}
