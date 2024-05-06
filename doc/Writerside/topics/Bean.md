# Bean

## Tree & Managed
Bean变量可以是一个Bean，Bean的变量可以是一个容器，容器内可以放入Bean。Bean通
常放到Table中管理。这样从Table为根，Bean的变量最终组成了一颗树，Bean的实例不
会被重复引用，也不会出现环。
Bean被加入Table就进入了Managed状态（或者被加入一个已经Managed状态的Bean
的容器中），在Managed之前，修改Bean不会被记录日志。Managed状态一旦设置，就不
会恢复，即使你从Table中或者容器中删除它。当你从Table或者容器中删除后要再次加入
进去，需要Copy一次。Managed状态只能被设置一次。如果你想加入重复的对象，使用
Bean.Copy 方法复制一份。
## 兼容性
### Variable.Id
Bean可以自由的增删variable，新旧Bean在系列化时自动兼容。其中varible.id在一个Bean
内不能重复。一般也不准回收理由删除的variable的id。删除variable，再恢复是允许的，
此时恢复的variable定义必须完全一样，这种情况被看作反悔操作，而不是新定义的variable
用了旧的id。反悔操作在原来的数据没有被覆盖的情况下，会重新读到旧的数据。一般反悔
操作是开发过程中，但还没发布的情况下发生。
### 数值类型
所有的数值类型(byte,short,int,long,float,double)之间互相兼容(运行时透明自动转换)，转换
规则跟所用编程语言的强转相同。所以注意高位截断等问题，通常应该从小范围类型改成大
范围类型。
bool类型跟数值类型也互相兼容，bool转数值成0和1，数值转bool会用"!=0"来取结果。
### Binary &amp; String
binary和string类型互相兼容，string的序列化是用UTF-8编码成binary，binary转string
时如果遇到无法UTF-8解码时会抛异常。Lua的string因为无需编解码所以跟binary完全兼
容。
### Dynamic & Bean
dynamic类型只是附带类型ID而不是根据配置选择具体类型名的bean，根据配置指定的方
法用类型ID取得具体的bean类型再反序列化。如果改成bean类型，则忽略类型ID直接
按指定的具体bean类型反序列化。如果从bean类型改成dynamic类型，则用默认0当作
dynamic的类型ID。
### List &amp; Set &amp; Array
序列容器(list,set,array)互相兼容，容器内类型的兼容性同上。
### Map
关联容器(map)内key和value的类型兼容性按上面的规则处理。
### 一般兼容描述
bean类型的兼容性只看字段ID和字段类型，与bean的命名和类型ID无关。序列化数据里
缺失的字段会当成默认值(0,false,空binary,空string,空容器,所有字段均为默认值的bean,内
容是EmptyBean的DynamicBean)。如果序列化数据里有当前bean类型中不存在的字段ID，
则直接忽略，再序列化时就会丢弃该字段。以上没提到兼容性和转换规则的转换均不支持，
遇到则用默认值取代旧值。
## BeautifulVariableId
关于bean增减variable的建议：按variable.id的顺序从1开始自增地分配和扩展；删除
variable不要直接删除，可以修改variable.name或注释来表示“临时不再使用”的含义，方便
保留数据库中已有的数据不丢失，以备之后再恢复使用，也防止增加variable时重用该
variable.id引发取出旧数据的混乱。如果有彻底全服删库的机会，可以删除不会再用的
variable，此时也可以顺便重新整理所有的variable.id。
但是开发过程中由于实现方案的修改，已经确定不需要旧的variable时（留着碍眼），可以
删除variable。再次提醒，除非反悔，不要重新利用旧的variable.id。由于开发期不稳定，
被删除的variable比较多，造成id不连续。对于有洁癖的程序员，就可以考虑利用Gen.exe
-BeautifulVariableId 功能重置id，使得他们从一开始连续编号。这个操作是个不兼容的修
改，只能用于开发期删除所有数据库数据的时候一并处理。一旦程序发布，就不能再使用
BeautifulVariableId功能了。使用BeautifulVariableId之后需要重新生成相关代码。
## Version
Table.Value可以定义一个版本号。当修改发生时自动递增。Table.Value总是一个Bean，这
个定义方式就放到这里了。声明例子：
```
<bean name=”BVersionSample” version=”VersionVarName”>
	<variable id=”1” name=” VersionVarName” type=”long”/>
	…
</bean>
```
1.	VersionVarName 可以自己命名，然后在Bean的属性version中声明哪一个变量用
来存储版本号。
2.	Type 必须是long。
3.	使用Bean.GetVersion()方法得到当前的版本号。
4.	如果Bean没有作为Table.Value，那么GetVersion总是返回0，没有意义。
5.	如果Table.Value记录被删除，然后再次加入，那么版本号重新从0开始。
