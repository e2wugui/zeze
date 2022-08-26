package Zege;

import org.w3c.dom.Element;

public class ZegeConfig implements Zeze.Config.ICustomize {
	public int AboutHasRead = 3;
	public int AboutLast = 20;
	public int MessageLimit = 20;

	@Override
	public String getName() {
		return "zege";
	}

	@Override
	public void Parse(Element self) {
		String attr;

		attr = self.getAttribute("AboutHasRead");
		if (!attr.isEmpty())
			AboutHasRead = Integer.parseInt(attr);

		attr = self.getAttribute("AboutLast");
		if (!attr.isEmpty())
			AboutLast = Integer.parseInt(attr);

		attr = self.getAttribute("MessageLimit");
		if (!attr.isEmpty())
			MessageLimit = Integer.parseInt(attr);
	}
}
