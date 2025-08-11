package x.mvmn.sonivm.ui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import x.mvmn.sonivm.playqueue.PlaybackQueueEntry;
import x.mvmn.sonivm.ui.model.PlaybackQueueTableModel;
import x.mvmn.sonivm.ui.util.swing.SwingUtil;
import x.mvmn.sonivm.util.StringUtil;

public class EditMetadataDialog extends JDialog {

	private JButton btnOk = new JButton("Save");
	private JButton btnCancel = new JButton("Cancel");

	private JTextField tfTrackNumber = new JTextField("");
	private JTextField tfArtist = new JTextField("");
	private JTextField tfAlbum = new JTextField("");
	private JTextField tfTitle = new JTextField("");
	private JTextField tfDate = new JTextField("");
	private JTextField tfGenre = new JTextField("");

	private JCheckBox cbTrackNumber = new JCheckBox();
	private JCheckBox cbArtist = new JCheckBox();
	private JCheckBox cbAlbum = new JCheckBox();
	private JCheckBox cbTitle = new JCheckBox();
	private JCheckBox cbDate = new JCheckBox();
	private JCheckBox cbGenre = new JCheckBox();

	public EditMetadataDialog(JFrame parentWindow, PlaybackQueueTableModel playbackQueueTableModel, int[] selectedRows) {
		super(parentWindow, "Edit metadata", true);

		tfTrackNumber.setText("1234");
		tfArtist.setText("kinda long artist name here for proper sizing");
		tfAlbum.setText("kinda long album name here for proper sizing");
		tfTitle.setText("kinda long track name here for proper sizing");
		tfDate.setText("2025-Aug-12");
		tfGenre.setText("Gothic Doom Death Black Symphonic Melodic Progressive Metal");

		tfTrackNumber.setBorder(BorderFactory.createTitledBorder("Track #"));
		tfArtist.setBorder(BorderFactory.createTitledBorder("Artist"));
		tfAlbum.setBorder(BorderFactory.createTitledBorder("Album"));
		tfTitle.setBorder(BorderFactory.createTitledBorder("Title"));
		tfDate.setBorder(BorderFactory.createTitledBorder("Date"));
		tfGenre.setBorder(BorderFactory.createTitledBorder("Genre"));

		this.getContentPane().setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();

		gbc.insets = new Insets(5, 5, 5, 5); // spacing
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;

		JCheckBox[] checkBoxes = new JCheckBox[] { cbTrackNumber, cbArtist, cbAlbum, cbTitle, cbDate, cbGenre };
		JTextField[] textFields = new JTextField[] { tfTrackNumber, tfArtist, tfAlbum, tfTitle, tfDate, tfGenre };

		for (int row = 0; row < 6; row++) {
			gbc.gridy = row;

			gbc.gridx = 0;
			gbc.weightx = 0;
			this.getContentPane().add(checkBoxes[row], gbc);

			gbc.gridx = 1;
			gbc.weightx = 1.0;
			this.getContentPane().add(textFields[row], gbc);
		}

		JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 10, 0));
		buttonPanel.add(btnOk);
		buttonPanel.add(btnCancel);

		gbc.gridx = 0;
		gbc.gridy = 6; // row 7
		gbc.gridwidth = 2; // span both columns
		gbc.weightx = 1.0;
		this.getContentPane().add(buttonPanel, gbc);

		this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		btnCancel.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				EditMetadataDialog.this.close();
			}
		});

		btnOk.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				for (int i = 0; i < selectedRows.length; i++) {
					PlaybackQueueEntry entry = playbackQueueTableModel.getEntry(selectedRows[i]);
					boolean changed = false;
					if (cbTrackNumber.isSelected()) {
						entry.getTrackMetadata().setTrackNumber(tfTrackNumber.getText());
						changed = true;
					}
					if (cbArtist.isSelected()) {
						entry.getTrackMetadata().setArtist(tfArtist.getText());
						changed = true;
					}
					if (cbAlbum.isSelected()) {
						entry.getTrackMetadata().setAlbum(tfAlbum.getText());
						changed = true;
					}
					if (cbTitle.isSelected()) {
						entry.getTrackMetadata().setTitle(tfTitle.getText());
						changed = true;
					}
					if (cbDate.isSelected()) {
						entry.getTrackMetadata().setDate(tfDate.getText());
						changed = true;
					}
					if (cbGenre.isSelected()) {
						entry.getTrackMetadata().setGenre(tfGenre.getText());
						changed = true;
					}
					if (changed) {
						playbackQueueTableModel.rowChanged(selectedRows[i]);
					}
				}
				EditMetadataDialog.this.close();
			}
		});

		this.pack();
		this.setResizable(false);
		SwingUtil.moveToScreenCenter(this);

		PlaybackQueueEntry firstEntry = playbackQueueTableModel.getEntry(selectedRows[0]);
		tfTrackNumber.setText(firstEntry.getTrackNumber());
		tfArtist.setText(firstEntry.getArtist());
		tfAlbum.setText(firstEntry.getAlbum());
		tfTitle.setText(firstEntry.getTitle());
		tfDate.setText(firstEntry.getDate());
		tfGenre.setText(firstEntry.getGenre());

		for (int i = 0; i < 6; i++) {
			checkBoxes[i].setSelected(true);
		}

		String[] refFields = new String[] { firstEntry.getTrackNumber(), firstEntry.getArtist(), firstEntry.getAlbum(),
				firstEntry.getTitle(), firstEntry.getDate(), firstEntry.getGenre() };
		for (int i = 1; i < selectedRows.length; i++) {
			PlaybackQueueEntry entry = playbackQueueTableModel.getEntry(selectedRows[i]);
			String[] fields = new String[] { entry.getTrackNumber(), entry.getArtist(), entry.getAlbum(), entry.getTitle(), entry.getDate(),
					entry.getGenre() };
			for (int k = 0; k < 6; k++) {
				if (!StringUtil.blankForNull(refFields[k]).equals(StringUtil.blankForNull(fields[k]))) {
					checkBoxes[k].setSelected(false);
					textFields[k].setText("");
				}
			}
		}

		for (int i = 0; i < 6; i++) {
			final JCheckBox cb = checkBoxes[i];
			textFields[i].getDocument().addDocumentListener(new DocumentListener() {

				@Override
				public void removeUpdate(DocumentEvent e) {
					cb.setSelected(true);
				}

				@Override
				public void insertUpdate(DocumentEvent e) {
					cb.setSelected(true);
				}

				@Override
				public void changedUpdate(DocumentEvent e) {
					cb.setSelected(true);
				}
			});
		}
	}

	private void close() {
		this.setVisible(false);
		this.dispose();
	}
}
