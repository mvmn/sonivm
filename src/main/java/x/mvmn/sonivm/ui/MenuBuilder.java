package x.mvmn.sonivm.ui;

import javax.swing.JMenu;

interface MenuBuilder<T> {
	public T build();

	void addSubMenu(JMenu subMenu);
}