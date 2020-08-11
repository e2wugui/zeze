Module 包含 Table 默认私有，可以通过增加方法返回并暴露出去
Module 使用 partical 把生成部分放到gen下
Module 生成 IModule 和 Module，这样IModule接口方法发生增删，方法参数变更等，代码编译可以报错。

协议里面的数值默认大于等于0，支持负数需要明确声明。
Config 独立
ByteBuffer.SkipUnknownField 移到其他地方
XXX 统一 bean 和 xbean 定义的话，怎么区分生成：被 table 引用的 bean 自动生成一份到某个特别的目录
统一生成工具，应用框架和数据存储框架定义分开？两者的数据bean的定义应该是不一样的，重用的可能性很小。
	主要入口 application 数据库定义 database. database.module 必须在 application 中存在。

去掉 xbean.Const xbean.Data
去掉 select。拷贝数据直接由 bean duplicate 支持
managed
数据变更?
Net.Manager 怎么重新定义？现在这个不够灵活。

<application name="demo" ModuleIdAllowRange="1-3,5">
	
	<bean name="b1">
		<enum name="Enum" value="4"/>
		<var id="1" name="s" type="int" default="1"/>
		<var id="2" name="m" type="map" key="int" value="int"/>
	</bean>

	<module name="m1" id="1">
		<bean name="Value">
			<var id="1" default="1" name="s" type="int"/>
		</bean>
		<cbean name="Key">
			<var id="1" default="1" name="s" type="short"/>
		</cbean>

		<protocol name="p1" id="1" argument="Value" handle="server,client"/>
		<rpc name="r1" id="2" argument="Value" result="Value" handle="server"/>
		<table name="table1" key="Key" value="Value"/>
	</module>

	<project name="gsd" language="cs">
		<!--
		mudule 可以被多个 manager 引用，但只会生成一份.
		这样的话，协议处理代码需要根据当前manager做不同实现。
		生成的时候警告处理。
		-->
		<manager name="Server" handletype="server|client" class="gsd.Provider">
			<module ref="m1"/>
		</manager>
	</project>
</application>
