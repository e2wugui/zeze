
配置编辑器

特性
	动态增删列，
	自动类型识别(Gen)，
	支持容器(list)，
	数据即时验证(只提示，允许保存错误数据)
	自动完成，

说明
	每一行被识别看成一个Bean，每一列看成一个变量。
	特殊列 ','	：Bean结束符，不能编辑。鼠标双击或者选中回车时增加新的列。
	特殊列 '[...'	：容器(List)的开始，不能编辑。
	特殊列 ']...'	：容器(List)的结束，不能编辑。鼠标双击或者选中回车时增加list中的Item。
	                  注意新增的Item如果没有填写数据不会被保存。

Test
	define 编辑基本完工。
	嵌套list问题：define中add时创建两个item。

TODO
	类型识别和Gen。
	自动完成和enum识别。
	1 id存一个种子自动递增，或者从上一行的id往后找一个未用的。
	2 普通的列默认最近使用的n个值，根据输入在列中查找最匹配的。
	SaveAs

	Gen 先转换数据（并进行识别和统计），然后生成代码。
	?List.Count <= 1时，生成一个Bean实例，但不能递归(好像null-able的话并且不是马上递归自身也可以)?
	Bean1
	   List<Bean2> varlist2;
	   --> Bean2 varlist2
	Bean2
	   List<Bean2> varlist2; // 这种递归的话，就不能简化，因为内部嵌套可能大于1，递归判断嵌套也<=1，规则太复杂也不好处理。
	   List<Bean3> varlist3; // 这种递归的话，好像可以？待确认？？？？？？？？
	Bean3
	   List<Bean2> varlist3;

问题
	1 Browse Dialog 初始显示位置偏离，可能跟windows放大有关，其他机器也许可以。
	2 Grid.Column.Width 保存在定义的Bean.Var中，如果Bean被多处引用或者多个实例（比如List中），
	  那么这些列共享一个配置. 当然编辑的时候，可以把同一个Bean.Var的列调整成不同的Width。
	  此时Bean.Var中保存最后一次调整Column.Width时的值。
	3 不要在编辑工具外部直接改名。
	  文件改名（改路径），怎么更新相关引用？扫描所有的配置文件？
	4 VarComment 先支持单行注释。多行再考虑。
