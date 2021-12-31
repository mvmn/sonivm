package x.mvmn.sonivm.ui.util.swing.menu;

import java.awt.event.ActionListener;
import java.util.function.Consumer;

import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JSeparator;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

public class JMenuBarBuilder implements MenuBuilder<JMenuBar> {

	private final JMenuBar menuBar = new JMenuBar();

	public JMenuBuilder<JMenuBarBuilder> menu(String text) {
		return new JMenuBuilder<JMenuBarBuilder>(this).text(text);
	}

	public JMenuBar build() {
		return menuBar;
	}

	public void addSubMenu(JMenu subMenu) {
		menuBar.add(subMenu);
	}

	@Setter
	@Accessors(fluent = true)
	@RequiredArgsConstructor
	public static class JMenuBuilder<T extends MenuBuilder<?>> implements MenuBuilder<T> {
		private final JMenu menu = new JMenu();

		private final T parent;

		private String text;
		private Action action;
		private ActionListener actr;

		public T build() {
			if (text != null) {
				menu.setText(text);
			}
			if (action != null) {
				menu.setAction(action);
			}
			if (actr != null) {
				menu.addActionListener(actr);
			}

			parent.addSubMenu(menu);
			return parent;
		}

		public JMenuBuilder<JMenuBuilder<T>> subMenu(String text) {
			return new JMenuBuilder<>(this).text(text);
		}

		public JMenuItemBuilder<T> item(String text) {
			return new JMenuItemBuilder<T>(this).text(text);
		}

		public JMenuBuilder<T> separator() {
			menu.add(new JSeparator());
			return this;
		}

		public JMenuBuilder<T> vseparator() {
			menu.add(new JSeparator(JSeparator.VERTICAL));
			return this;
		}

		@Override
		public void addSubMenu(JMenu subMenu) {
			menu.add(subMenu);
		}
	}

	@Setter
	@Accessors(fluent = true)
	@RequiredArgsConstructor
	public static class JMenuItemBuilder<T extends MenuBuilder<?>> {
		private final JMenuBuilder<T> parent;
		private String text;
		private Action action;
		private ActionListener actr;
		private Icon icon;
		private Integer mnemonic;
		private boolean asCheckbox;
		private boolean asRadioButton;
		private boolean checked = false;
		private Consumer<JMenuItem> process;
		private ButtonGroup group;

		public JMenuItemBuilder<T> checkbox() {
			asCheckbox = true;
			asRadioButton = false;
			return this;
		}

		public JMenuItemBuilder<T> radioButton() {
			asCheckbox = false;
			asRadioButton = true;
			return this;
		}

		public JMenuBuilder<T> build() {
			JMenuItem mi;
			if (asCheckbox) {
				JCheckBoxMenuItem cbmi = new JCheckBoxMenuItem();
				cbmi.setState(checked);
				if (group != null) {
					group.add(cbmi);
				}
				mi = cbmi;
			} else if (asRadioButton) {
				JRadioButtonMenuItem rbmi = new JRadioButtonMenuItem();
				rbmi.setSelected(checked);
				if (group != null) {
					group.add(rbmi);
				}
				mi = rbmi;
			} else {
				mi = new JMenuItem();
			}
			if (text != null) {
				mi.setText(text);
			}
			if (action != null) {
				mi.setAction(action);
			}
			if (actr != null) {
				mi.addActionListener(actr);
			}
			if (icon != null) {
				mi.setIcon(icon);
			}
			if (mnemonic != null) {
				mi.setMnemonic(mnemonic);
			}
			if (process != null) {
				process.accept(mi);
			}

			parent.menu.add(mi);
			return parent;
		}
	}
}
