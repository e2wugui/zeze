Module 包含 Table 默认私有，可以通过增加方法返回并暴露出去
Module 使用 partical 把生成部分放到gen下
Config 独立？
ByteBuffer.SkipUnknownField 移到其他地方
XXX 统一 bean 和 xbean 定义的话，怎么区分生成：被 table 引用的 bean 自动生成一份到某个特别的目录
去掉 xbean.Const xbean.Data
去掉 select。拷贝数据直接由 bean duplicate 支持
managed
数据变更

<application name="demo“>
	<bean name="b1">
		<var id="1" default="1" name="s" type="int"/>
	</bean>

	<module name="m1">
		<bean name="Value">
			<var id="1" default="1" name="s" type="int"/>
		</bean>
		<cbean name="Key">
			<var id="1" default="1" name="s" type="short"/>
		</cbean>

		<protocol name="p1" id="1"/>
		<table name="table1" key="Key" value="Value"/>
	</module>
</Application>