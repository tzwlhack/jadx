package jadx.gui.ui.codearea.mode;

import javax.swing.Icon;

import org.jetbrains.annotations.Nullable;

import jadx.api.DecompilationMode;
import jadx.api.ICodeInfo;
import jadx.core.dex.nodes.ClassNode;
import jadx.gui.treemodel.JClass;
import jadx.gui.treemodel.JNode;

public class JCodeMode extends JNode {

	private final JClass jCls;
	private final DecompilationMode mode;

	public JCodeMode(JClass jClass, DecompilationMode mode) {
		this.jCls = jClass;
		this.mode = mode;
	}

	@Override
	public JClass getJParent() {
		return jCls.getJParent();
	}

	@Override
	public Icon getIcon() {
		return jCls.getIcon();
	}

	@Override
	public String makeString() {
		return jCls.makeString();
	}

	@Override
	public @Nullable ICodeInfo getCodeInfo() {
		ClassNode cls = jCls.getCls().getClassNode();
		return cls.decompileWithMode(mode);
	}

	@Override
	public String getContent() {
		return getCodeInfo().getCodeStr();
	}

	@Override
	public String getSyntaxName() {
		return jCls.getSyntaxName();
	}
}
