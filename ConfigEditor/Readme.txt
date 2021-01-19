
配置编辑器

特性
	动态增删列;
	支持容器(编辑时只有list，根据配置生成Map索引);
	数据即时验证(只提示，允许保存错误数据)；
	自动完成；

说明
	每一行被识别看成一个Bean，每一列看成一个变量。
	特殊列 ','	：Bean结束符，不能编辑。鼠标双击或者选中回车时增加新的列。
	特殊列 '[...'	：容器(List)的开始，不能编辑。
	特殊列 ']...'	：容器(List)的结束，不能编辑。鼠标双击或者选中回车时增加list中的Item。
	                  注意新增的Item如果没有填写数据不会被保存。

注意
	使用源码管理库
		编辑工具使用xml等文本文件保存数据便于追踪变化；
		Build导出的Release数据也可以提交一份到源码管理库；
	多人编辑
		注意涉及id,unique的列，如果两个人添加了相同的值，会导致验证失败；
	不要在编辑工具外部直接改文件名（改路径）；
	VarComment 先支持单行注释。多行再考虑;
	变量(列)改名需要搜索全部配置，不大好优化。使用时注意;

Test
	* BUG 保存了FormError的窗口位置。但是有时会变成初始化的位置。
	* XXX 双击错误列表定位到文档的功能暂时不能用了。

	2021/1/12
	装载文档增加异步模式。用于打开文件时。其他时候还是同步装载。
	Build 改成 async，实际上只有一个线程在执行，就是为了显示进度和可以取消。
	Build 后，关闭掉没有打开View及被View依赖的Document。
	FormError 还是在 UI-thread 里面执行，只是 AddError RemoveError 根据需要使用 BeginInvoke. 
	FormBuildProgress 显示彩色。

	2021/1/13
	Document.IsChanged：拦截所有的public Property，在里面设置IsChanged。保护一下成员变量，免得以后不好维护。
	BeanDefine 引用计数改成 List，记录 ReferenceFrom。准备用来优化”变量改名“，”Bean改名“，”文件改名“。

	2021/1/14
	新的变量改名，不再搜索全部文档。
	Bean改名，不搜索全部文档。如果是root，文件名也改。（还不能改目录）。
	改名还需要更新ReferenceFrom的参数。
	列改名引起的bean.rename，没有更新formdefine。再次调用FormDefine.LoadDefine
	改名更新foreign。

	2021/1/15
	变量改名需要更新引用别人的ReferenceFrom。
	变量改名，先收集需要更新的Document，去掉重复的，然后UpdateData。
	bean改名输入的时候去掉path。

	2021/1/16
	AddVar,DeleteVar更新也使用ReferenceFroms。
	查看所有的 Documents.ForEachFile，确认是否可以用 ReferenceFrom。
	SaveAs

性能
	* 几千行看看会怎么样。

TODO
	自动完成: Foreign
	自动完成：id Load 的时候记录 maxid，以后编辑AddRow都使用这个递增。
	更多自动完成？普通的列默认最近使用的n个值，根据输入在列中查找最匹配的。
	增加一个工作线程，所有数据操作都放到这里面，和UI线程交互。看情况再决定做不做。
	enum 现在不支持引用在其他文档定义的，有需要了再来加。
