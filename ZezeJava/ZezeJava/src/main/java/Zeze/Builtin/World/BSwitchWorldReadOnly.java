// auto-generated @formatter:off
package Zeze.Builtin.World;

/*
为了不污染根空间，定义成Command了。
			<protocol name="SwitchWorld" argument="BSwitchWorld" handle="server"/> mapId==-1，进入地图由服务器控制，此时仅仅表示客户端准备好进入地图了。
			<protocol name="EnterWorld" argument="BEnterWorld" handle="client"/>
			<protocol name="EnterConfirm" argument="BEnterConfirm" handle="server"/>
			<protocol name="PutData" argument="BPutData" handle="client"/>
			<protocol name="RemoveData" argument="BRemoveData" handle="client"/>
*/
public interface BSwitchWorldReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BSwitchWorld copy();

    int getMapId();
}
