package NPinyin;

/**
 * NPinyin包含一个公开类Pinyin，该类实现了取汉字文本首字母、文本对应拼音、以及
 * 获取和拼音对应的汉字列表等方法。由于汉字字库大，且多音字较多，因此本组中实现的
 * 拼音转换不一定和词语中的字的正确读音完全吻合。但绝大部分是正确的。
 * 
 * 最后感谢百度网友韦祎提供的常用汉字拼音对照表。见下载地址：
 * http: //wenku.baidu.com/view/d725f4335a8102d276a22f46.html
 * 
 * 最后，我想简要地说明一下我的设计思路：
 * 首先，我将汉字按拼音分组后建立一个字符串数组（见PyCode.codes），然后使用程序
 * 将PyCode.codes中每一个汉字通过其编码值使用散列函数：
 * 
 *     f(x) = x % PyCode.codes.Length
 *   { 
 *     g(f(x)) = pos(x)
 *     
 * 其中, pos(x)为字符x所属字符串所在的PyCode.codes的数组下标, 然后散列到同
 * PyCode.codes长度相同长度的一个散列表中PyHash.hashes）。
 * 当检索一个汉字的拼音时，首先从PyHash.hashes中获取和
 * 对应的PyCode.codes中数组下标，然后从对应字符串查找，当到要查找的字符时，字符
 * 串的前6个字符即包含了该字的拼音。
 * 
 * 此种方法的好处一是节约了存储空间，二是兼顾了查询效率。
 *
 * 如有意见，请与我联系反馈。我的邮箱是：qzyzwsy@gmail.com
 * 
 * 汪思言 2011年1月3日凌晨
 * */

/*
 * v0.2.x的变化
 * =================================================================
 * 1、增加对不同编码格式文本的支持,同时增加编码转换方法Pinyin.ConvertEncoding
 * 2、重构单字符拼音的获取，未找到拼音时返回字符本身.
 * 
 * 汪思言 2012年7月23日晚
 * 
 */


  public final class Pinyin {
	/** 
	 取中文文本的拼音首字母
	 
	 @param text 编码为UTF8的文本
	 @return 返回中文对应的拼音首字母
	*/

	public static String GetInitials(String text) {
	  text = text.strip();
	  StringBuilder chars = new StringBuilder();
	  for (var i = 0; i < text.length(); ++i) {
		String py = GetPinyin(text.charAt(i));
		if (!py.equals("")) {
			chars.append(py.charAt(0));
		}
	  }

	  return chars.toString().toUpperCase();
	}

	/** 
	 取中文文本的拼音
	 
	 @param text 编码为UTF8的文本
	 @return 返回中文文本的拼音
	*/

	public static String GetPinyin(String text) {
	  StringBuilder sbPinyin = new StringBuilder();
	  for (var i = 0; i < text.length(); ++i) {
		String py = GetPinyin(text.charAt(i));
		if (!py.equals("")) {
			sbPinyin.append(py);
		}
		sbPinyin.append(" ");
	  }

	  return sbPinyin.toString().strip();
	}

	/** 
	 取和拼音相同的汉字列表
	 
	 @param Pinyin 编码为UTF8的拼音
	 @return 取拼音相同的汉字列表，如拼音"ai”将会返回“唉爱……”等</returns>
	*/
	public static String GetChineseText(String pinyin) {
	  String key = pinyin.strip().toLowerCase();

	  for (String str : PyCode.codes) {
		if (str.startsWith(key + " ") || str.startsWith(key + ":")) {
		 return str.substring(7);
		}
	  }

	  return "";
	}

	/** 
	 返回单个字符的汉字拼音
	 
	 @param ch 编码为UTF8的中文字符
	 @return ch对应的拼音
	*/
	public static String GetPinyin(char ch) {
	  short hash = GetHashIndex(ch);
	  for (var i = 0; i < PyHash.hashes[hash].length; ++i) {
		short index = PyHash.hashes[hash][i];
		var pos = PyCode.codes[index].indexOf(ch, 7);
		if (pos != -1) {
		  return PyCode.codes[index].substring(0, 6).strip();
		}
	  }
	  return String.valueOf(ch);
	}

	/** 
	 取文本索引值
	 
	 @param ch 字符
	 @return 文本索引值
	*/
	private static short GetHashIndex(char ch) {
	  return (short)((int)ch % PyCode.codes.length);
	}
  }