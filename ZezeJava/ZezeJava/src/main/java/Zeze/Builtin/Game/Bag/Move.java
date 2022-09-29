// auto-generated @formatter:off
package Zeze.Builtin.Game.Bag;

/*
				<bean name="BChanged">
					<enum name="TagIncrementChange" value="0"/> 增量修改。
					<enum name="TagRecordRemoved"   value="1"/> 整个记录删除了。
					<enum name="TagRecordReplace"   value="2"/> 整个记录发生了变更，需要先清除本地数据，再替换进去。

					<variable id="1" name="BagName" type="string"/>
					<variable id="2" name="Tag" type="int"/> 处理方式
					<variable id="3" name="Replaced" type="map" key="int" value="BItem"/> key is position
					<variable id="4" name="Removed" type="set" value="int"/> key is position
				</bean>
				<protocol name="NotifyChanged" argument="BChanged" handle="client"/> 所有的包裹改变都通过这个协议发送。
				<protocol name="NotifyBag" argument="BBag" handle="client"/> 包裹全部改变时发送这个协议。在可靠消息框架内使用。不直接处理。
*/
public class Move extends Zeze.Net.Rpc<Zeze.Builtin.Game.Bag.BMove, Zeze.Transaction.EmptyBean> {
    public static final int ModuleId_ = 11014;
    public static final int ProtocolId_ = -790071751; // 3504895545
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47308274693689

    @Override
    public int getModuleId() {
        return ModuleId_;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
    }

    public Move() {
        Argument = new Zeze.Builtin.Game.Bag.BMove();
        Result = Zeze.Transaction.EmptyBean.instance;
    }

    public Move(Zeze.Builtin.Game.Bag.BMove arg) {
        Argument = arg;
        Result = Zeze.Transaction.EmptyBean.instance;
    }
}
