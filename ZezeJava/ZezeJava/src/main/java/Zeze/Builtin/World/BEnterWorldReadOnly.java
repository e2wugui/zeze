// auto-generated @formatter:off
package Zeze.Builtin.World;

/*
为了不污染根空间，改成Command了。
			<protocol name="SwitchWorld" argument="BSwitchWorld" handle="server"/> mapId==-1，进入地图由服务器控制，此时仅仅表示客户端准备好进入地图了。
			<protocol name="EnterWorld" argument="BEnterWorld" handle="client"/>
			<protocol name="EnterConfirm" argument="BEnterConfirm" handle="server"/>

			Aoi-Notify
			<protocol name="AoiEnter" argument="BAoiEnter" handle="client"/>
			<protocol name="AoiOperate" argument="BAoiOperate" handle="client"/>
			<protocol name="AoiLeave" argument="BAoiLeave" handle="client"/>
*/
public interface BEnterWorldReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BEnterWorld copy();

    int getMapId();
    long getMapInstanceId();
    long getEntityId();
    Zeze.Serialize.Vector3 getPosition();
    Zeze.Serialize.Vector3 getDirect();
    Zeze.Transaction.Collections.PList2ReadOnly<Zeze.Builtin.World.BAoiOperates, Zeze.Builtin.World.BAoiOperatesReadOnly> getPriorityDataReadOnly();
}
