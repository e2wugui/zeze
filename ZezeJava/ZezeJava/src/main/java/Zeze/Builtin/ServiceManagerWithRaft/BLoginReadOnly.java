// auto-generated @formatter:off
package Zeze.Builtin.ServiceManagerWithRaft;

/*
			<bean name="BAllocateIdArgument">
				<variable id="1" name="Name" type="string"/>
				<variable id="2" name="Count" type="int"/>
			</bean>

			<bean name="BAllocateIdResult">
				<variable id="1" name="Name" type="string"/>
				<variable id="2" name="StartId" type="long"/>
				<variable id="3" name="Count" type="int"/>
			</bean>

			<bean name="BOfflineNotify">
				<variable id="1" name="ServerId" type="int"/>
				<variable id="2" name="NotifyId" type="string"/>
				<variable id="3" name="NotifySerialId" type="long"/>
				<variable id="4" name="NotifyContext" type="binary"/>
			</bean>

			<bean name="BServerLoad">
				<variable id="1" name="Ip" type="string"/>
				<variable id="2" name="Port" type="int"/>
				<variable id="3" name="Param" type="binary"/>
			</bean>

			<bean name="BServiceInfo">
				<variable id="1" name="ServiceName" type="string"/>
				<variable id="2" name="ServiceIdentity" type="string"/>
				<variable id="3" name="PassiveIp" type="string"/>
				<variable id="4" name="PassivePort" type="int"/>
				<variable id="5" name="ExtraInfo" type="binary"/>
			</bean>

			<bean name="BServiceInfos">
				<variable id="1" name="ServiceName" type="string"/>
				<variable id="2" name="ServiceInfoListSortedByIdentity" type="list[BServiceInfo]"/>
				<variable id="3" name="SerialId" type="long"/>
			</bean>

			<bean name="BServiceListVersion">
				<variable id="1" name="ServiceName" type="string"/>
				<variable id="2" name="SerialId" type="long"/>
			</bean>

			<bean name="BSubscribeInfo">
				<enum name="SubscribeTypeSimple" value="0"/>
				<enum name="SubscribeTypeReadyCommit" value="1"/>

				<variable id="1" name="ServiceName" type="string"/>
				<variable id="2" name="SubscribeType" type="int"/>
			</bean>
*/
public interface BLoginReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BLogin copy();

    public String getSessionName();
}
