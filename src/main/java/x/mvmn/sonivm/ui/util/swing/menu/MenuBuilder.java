package x.mvmn.sonivm.ui.util.swing.menu;

import javax.swing.JMenu;

interface MenuBuilder<T> {
	public T build();

	void addSubMenu(JMenu subMenu);
}