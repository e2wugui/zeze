
配置编辑器

特性
	动态增删列，
	支持容器(编辑时只有list，根据配置生成时可能是Map索引)，
	数据即时验证(只提示，允许保存错误数据),
	自动完成，

说明
	每一行被识别看成一个Bean，每一列看成一个变量。
	特殊列 ','	：Bean结束符，不能编辑。鼠标双击或者选中回车时增加新的列。
	特殊列 '[...'	：容器(List)的开始，不能编辑。
	特殊列 ']...'	：容器(List)的结束，不能编辑。鼠标双击或者选中回车时增加list中的Item。
	                  注意新增的Item如果没有填写数据不会被保存。

Test
	* NEW foreign 校验加入了。
	* NEW 变量改名。会更新Foreign和数据。涉及全局搜索，大量配置时，可能很慢。
	* BUG 保存了FormError的窗口位置。但是有时会变成初始化的位置。

	2021/1/4
	* NEW Build 加入错误检查。如果有错误，中断执行。
	* BUG FormError 在根据 Grid 删除错误时没有恢复 Cell 的状态。

	2021/1/5
	* NEW Add “Date” Type
	* NEW typescript gen 数据生成在代码中。
	* NEW lua gen 数据生成在代码中。生成代码没有执行过。
	* NEW Add Var.Default。

	2021/1/6
	* NEW enum

	2021/1/7
	* CHANGE OpenDocument 重构。处理一个目录下的子目录名和文件名（不含后缀）相同的情况。正常使用禁止发生。

	2021/1/10
	* CHANGE 重构：DataGridView 改成 VirtualMode。

性能
	* 几千行看看会怎么样。

TODO
	重构以后，verify获取oldValue，newValue的逻辑可能要改。
 	FormBuildProgress async。需要把gird的数据层独立出来自己管理，使用virtual模式。

	自动完成: Foreign
	更多自动完成？
		普通的列默认最近使用的n个值，根据输入在列中查找最匹配的。
	id Load 的时候记录 maxid，以后编辑AddRow都使用这个递增。
	Bean改名。需要搜索引用。
	SaveAs
	enum 现在不支持引用在其他地方定义的，有需要了再来加。

问题
	* 不要在编辑工具外部直接改名。文件改名（改路径），怎么更新相关引用？扫描所有的配置文件？
	* VarComment 先支持单行注释。多行再考虑。
	* 变量改名需要搜索全部配置，不大好优化。使用时注意。
