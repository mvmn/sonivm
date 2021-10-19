package x.mvmn.sonivm.ui;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;

import x.mvmn.sonivm.ui.util.swing.SwingUtil;

public class SupportedFileExtensionsDialog extends JDialog {
	private static final long serialVersionUID = -3668015514241023079L;

	private final JButton btnOk = new JButton("Ok");
	private final JButton btnCancel = new JButton("Cancel");
	private final JButton btnAdd = new JButton("+");
	private final JButton btnDelete = new JButton("-");
	private final DefaultListModel<String> model = new DefaultListModel<>();
	private final JList<String> extensionsList = new JList<>(model);

	public SupportedFileExtensionsDialog() {
		super((Frame) null, "Extensions");
		this.setModal(true);
		this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

		btnCancel.addActionListener(actEvent -> {
			this.setVisible(false);
			this.dispose();
		});

		this.getContentPane().setLayout(new BorderLayout());
		this.getContentPane().add(new JScrollPane(extensionsList), BorderLayout.CENTER);
		this.getContentPane().add(SwingUtil.panel(pnl -> new GridLayout(1, 2)).add(btnOk).add(btnCancel).build(), BorderLayout.SOUTH);
		this.getContentPane().add(SwingUtil.panel(pnl -> new GridLayout(1, 2)).add(btnAdd).add(btnDelete).build(), BorderLayout.NORTH);

		btnAdd.addActionListener(actEvent -> {
			String result = JOptionPane.showInputDialog(this, "File extension");
			if (result != null && !result.trim().isEmpty()) {
				model.addElement(result.trim().toLowerCase());
			}
		});
		btnDelete.addActionListener(actEvent -> {
			int[] selectedIdxs = extensionsList.getSelectedIndices();
			if (selectedIdxs.length > 0) {
				for (int i = selectedIdxs.length - 1; i >= 0; i--) {
					model.remove(selectedIdxs[i]);
				}
			}
		});
	}

	public void display(Collection<String> values, Consumer<List<String>> okHandler) {
		SwingUtil.removeAllActionListeners(btnOk);
		btnOk.addActionListener(actEvent -> {
			List<String> result = new ArrayList<>(model.getSize());
			for (int i = 0; i < model.getSize(); i++) {
				result.add(model.getElementAt(i));
			}
			this.setVisible(false);
			this.dispose();
			okHandler.accept(result);
		});
		if (values != null) {
			values.forEach(model::addElement);
		}

		this.pack();
		SwingUtil.moveToScreenCenter(this);
		this.setVisible(true);
	}
}
