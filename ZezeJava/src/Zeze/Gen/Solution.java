package Zeze.Gen;

import Zeze.*;
import java.util.*;

public class Solution extends ModuleSpace {
	private Zeze.Util.Ranges ModuleIdAllowRanges;
	public final Zeze.Util.Ranges getModuleIdAllowRanges() {
		return ModuleIdAllowRanges;
	}
	private void setModuleIdAllowRanges(Zeze.Util.Ranges value) {
		ModuleIdAllowRanges = value;
	}
	private Zeze.Util.Ranges ModuleIdCurrentRanges = new Zeze.Util.Ranges();
	public final Zeze.Util.Ranges getModuleIdCurrentRanges() {
		return ModuleIdCurrentRanges;
	}
	private void setModuleIdCurrentRanges(Zeze.Util.Ranges value) {
		ModuleIdCurrentRanges = value;
	}

	private TreeMap<String, Project> Projects = new TreeMap<String, Project> ();
	public final TreeMap<String, Project> getProjects() {
		return Projects;
	}
	private void setProjects(TreeMap<String, Project> value) {
		Projects = value;
	}

	public Solution(XmlElement self) {
		super(null, self);
		if (false == self.Name.equals("solution")) {
			throw new RuntimeException("node name is not solution");
		}

		setModuleIdAllowRanges(new Zeze.Util.Ranges(self.GetAttribute("ModuleIdAllowRanges")));
		Program.getGlobalModuleIdChecker().CheckAdd(getModuleIdAllowRanges());

		XmlNodeList childNodes = self.ChildNodes;
		for (XmlNode node : childNodes) {
			if (XmlNodeType.Element != node.NodeType) {
				continue;
			}

			XmlElement e = (XmlElement)node;
			switch (e.Name) {
				case "bean":
					new Zeze.Gen.Types.Bean(this, e);
					break;
				case "module":
					new Module(this, e);
					break;
				case "project":
					new Project(this, e);
					break;
				case "beankey":
					new Zeze.Gen.Types.BeanKey(this, e);
					break;
				case "import":
					Program.ImportSolution(e.GetAttribute("file"));
					break;
				default:
					throw new RuntimeException("unknown nodename " + e.Name + " in solution=" + getName());
			}
		}
	}

	@Override
	public void Compile() {
		for (Project project : getProjects().values()) {
			project.Compile();
		}
		super.Compile();
	}

	public final void Make() {
		for (Project project : getProjects().values()) {
			project.Make();
		}
	}
}