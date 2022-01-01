package x.mvmn.sonivm.ui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.filechooser.FileNameExtensionFilter;

import x.mvmn.sonivm.eq.model.EqualizerPreset;
import x.mvmn.sonivm.eq.model.EqualizerState;
import x.mvmn.sonivm.ui.util.swing.SwingUtil;

public class EqualizerWindow extends JFrame {
	private static final long serialVersionUID = -659390575435440032L;

	private static final String[] BAND_LABELS = { "60", "170", "310", "600", "1K", "3K", "6K", "12K", "14K", "16K" };

	private final JSlider[] bandSliders;
	private final JSlider gainSlider;
	private final JCheckBox cbEnabled;
	private final JButton btnReset;
	private final JButton btnSave;
	private final JButton btnImport;
	private final JButton btnExport;
	private final JButton btnLoad;

	public EqualizerWindow(String title, int bandCount) {
		super(title);

		this.setResizable(false);

		this.cbEnabled = new JCheckBox("EQ on", false);

		bandSliders = new JSlider[bandCount];
		for (int i = 0; i < bandCount; i++) {
			JSlider bandSlider = new JSlider(JSlider.VERTICAL, 0, 1000, 500);
			bandSliders[i] = bandSlider;
			SwingUtil.makeJSliderMoveToClickPoistion(bandSliders[i]);
			SwingUtil.onDoubleClick(bandSlider, e -> bandSlider.setValue(500));
		}

		btnReset = new JButton("Reset");
		btnSave = new JButton("Save...");
		btnImport = new JButton("Import EQF...");
		btnExport = new JButton("Export EQF...");

		btnLoad = new JButton("Load >");

		gainSlider = new JSlider(JSlider.VERTICAL, 0, 1000, 500);
		SwingUtil.onDoubleClick(gainSlider, e -> gainSlider.setValue(500));
		SwingUtil.makeJSliderMoveToClickPoistion(gainSlider);

		Container eqPanel = new JPanel(new GridBagLayout());
		GridBagConstraints gridBagCnstr = new GridBagConstraints();

		gridBagCnstr.fill = GridBagConstraints.BOTH;
		gridBagCnstr.gridx = 0;
		gridBagCnstr.gridy = 0;
		gridBagCnstr.weightx = 0;
		gridBagCnstr.weighty = 0;

		gridBagCnstr.weightx = 1;
		eqPanel.add(new JLabel(" "), gridBagCnstr);
		gridBagCnstr.weightx = 0;
		gridBagCnstr.gridx++;
		eqPanel.add(new JLabel("Gain"), gridBagCnstr);
		gridBagCnstr.gridx++;
		gridBagCnstr.weightx = 1;
		eqPanel.add(new JLabel(" "), gridBagCnstr);
		gridBagCnstr.gridx++;
		eqPanel.add(new JLabel(" "), gridBagCnstr);
		gridBagCnstr.weightx = 0;
		gridBagCnstr.gridx++;

		List<JLabel> bandLabels = new ArrayList<>();
		for (int i = 0; i < bandSliders.length; i++) {
			gridBagCnstr.weightx = 1;
			eqPanel.add(new JLabel(" "), gridBagCnstr);
			gridBagCnstr.weightx = 0;
			gridBagCnstr.gridx++;
			JLabel label = new JLabel(BAND_LABELS[i], JLabel.CENTER);
			bandLabels.add(label);
			eqPanel.add(label, gridBagCnstr);
			gridBagCnstr.gridx++;
			gridBagCnstr.weightx = 1;
			eqPanel.add(new JLabel(" "), gridBagCnstr);
			gridBagCnstr.weightx = 0;
			gridBagCnstr.gridx++;
		}

		gridBagCnstr.gridx = 0;
		gridBagCnstr.gridy = 1;

		gridBagCnstr.weightx = 1;
		eqPanel.add(new JLabel(" "), gridBagCnstr);
		gridBagCnstr.weightx = 0;
		gridBagCnstr.gridx++;
		eqPanel.add(new JLabel("+", JLabel.CENTER), gridBagCnstr);
		gridBagCnstr.gridx++;
		gridBagCnstr.weightx = 1;
		eqPanel.add(new JLabel(" "), gridBagCnstr);
		gridBagCnstr.gridx++;
		eqPanel.add(new JLabel(" "), gridBagCnstr);
		gridBagCnstr.weightx = 0;
		gridBagCnstr.gridx++;
		for (int i = 0; i < bandSliders.length; i++) {
			gridBagCnstr.weightx = 1;
			eqPanel.add(new JLabel(" "), gridBagCnstr);
			gridBagCnstr.weightx = 0;
			gridBagCnstr.gridx++;
			eqPanel.add(new JLabel("+", JLabel.CENTER), gridBagCnstr);
			gridBagCnstr.gridx++;
			gridBagCnstr.weightx = 1;
			eqPanel.add(new JLabel(" "), gridBagCnstr);
			gridBagCnstr.weightx = 0;
			gridBagCnstr.gridx++;
		}

		gridBagCnstr.gridx = 0;
		gridBagCnstr.gridy = 2;

		gridBagCnstr.weightx = 1;
		eqPanel.add(new JLabel(" -", JLabel.RIGHT), gridBagCnstr);
		gridBagCnstr.weightx = 0;
		gridBagCnstr.gridx++;
		gridBagCnstr.weighty = 1;
		eqPanel.add(gainSlider, gridBagCnstr);
		gridBagCnstr.weighty = 0;
		gridBagCnstr.gridx++;
		gridBagCnstr.weightx = 1;
		eqPanel.add(new JLabel("- ", JLabel.LEFT), gridBagCnstr);
		gridBagCnstr.weightx = 0;
		gridBagCnstr.gridx++;
		gridBagCnstr.weightx = 1;
		eqPanel.add(new JLabel(" "), gridBagCnstr);
		gridBagCnstr.weightx = 0;
		gridBagCnstr.gridx++;

		for (int i = 0; i < bandSliders.length; i++) {
			gridBagCnstr.weightx = 1;
			eqPanel.add(new JLabel("-", JLabel.RIGHT), gridBagCnstr);
			gridBagCnstr.weightx = 0;
			gridBagCnstr.gridx++;
			gridBagCnstr.weighty = 1;
			eqPanel.add(bandSliders[i], gridBagCnstr);
			gridBagCnstr.weighty = 0;
			gridBagCnstr.gridx++;
			gridBagCnstr.weightx = 1;
			eqPanel.add(new JLabel("-", JLabel.LEFT), gridBagCnstr);
			gridBagCnstr.weightx = 0;
			gridBagCnstr.gridx++;
		}

		gridBagCnstr.gridx = 0;
		gridBagCnstr.gridy = 3;

		gridBagCnstr.weightx = 1;
		eqPanel.add(new JLabel(" "), gridBagCnstr);
		gridBagCnstr.gridx++;
		gridBagCnstr.weightx = 0;
		eqPanel.add(new JLabel("-", JLabel.CENTER), gridBagCnstr);
		gridBagCnstr.gridx++;
		gridBagCnstr.weightx = 1;
		eqPanel.add(new JLabel(" "), gridBagCnstr);
		gridBagCnstr.weightx = 0;
		gridBagCnstr.gridx++;

		gridBagCnstr.gridx++;
		for (int i = 0; i < bandSliders.length; i++) {
			gridBagCnstr.weightx = 1;
			eqPanel.add(new JLabel(" "), gridBagCnstr);
			gridBagCnstr.weightx = 0;
			gridBagCnstr.gridx++;
			eqPanel.add(new JLabel("-", JLabel.CENTER), gridBagCnstr);
			gridBagCnstr.gridx++;
			gridBagCnstr.weightx = 1;
			eqPanel.add(new JLabel(" "), gridBagCnstr);
			gridBagCnstr.weightx = 0;
			gridBagCnstr.gridx++;
		}
		this.getContentPane().setLayout(new BorderLayout());
		this.getContentPane().add(eqPanel, BorderLayout.CENTER);
		this.getContentPane().add(btnReset, BorderLayout.SOUTH);
		this.getContentPane()
				.add(SwingUtil.panel(pnl -> new GridLayout(2, 3))
						.add(cbEnabled)
						.add(btnSave)
						.add(btnLoad)
						.add(new JLabel())
						.add(btnImport)
						.add(btnExport)
						.build(), BorderLayout.NORTH);
		this.pack();
		int longestLabelWidth = bandLabels.stream().mapToInt(JLabel::getWidth).max().getAsInt();
		bandLabels.forEach(lbl -> lbl.setPreferredSize(new Dimension(longestLabelWidth, lbl.getPreferredSize().height)));
		bandLabels.forEach(lbl -> lbl.setMinimumSize(new Dimension(longestLabelWidth, lbl.getMinimumSize().height)));
		this.pack();

		this.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		SwingUtil.moveToScreenCenter(this);
	}

	public void registerHandler(SonivmUIController handler) {
		cbEnabled.addActionListener(actEvent -> handler.setEQEnabled(cbEnabled.isSelected()));
		btnLoad.addActionListener(actEvent -> handler.showPresetsMenu(btnLoad, btnLoad.getWidth(), 0));

		SwingUtil.addValueChangeByUserListener(gainSlider, changeEvent -> handler.onEQGainChange(gainSlider.getValue() / 1000.0d));

		for (int i = 0; i < bandSliders.length; i++) {
			JSlider bandSlider = bandSliders[i];
			final int bandNumber = i;
			SwingUtil.addValueChangeByUserListener(bandSlider,
					changeEvent -> handler.onEQBandChange(bandNumber, bandSlider.getValue() / 1000.0d));
		}

		btnSave.addActionListener(actEvent -> {
			String presetName = JOptionPane.showInputDialog(this, "Enter preset name", "");
			if (presetName != null && !presetName.trim().isEmpty()) {
				handler.onEQSavePreset(presetName, toPreset());
			}
		});

		btnImport.addActionListener(actEvent -> {
			JFileChooser jfc = new JFileChooser();
			jfc.setFileFilter(new FileNameExtensionFilter("WinAmp equilizer file (EQF)", "eqf"));
			jfc.setMultiSelectionEnabled(false);
			jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
			if (JFileChooser.APPROVE_OPTION == jfc.showOpenDialog(this)) {
				handler.onEQImportPreset(jfc.getSelectedFile());
			}
		});

		btnExport.addActionListener(actEvent -> {
			String presetName = JOptionPane.showInputDialog(this, "Enter preset name", "");
			if (presetName != null && !presetName.trim().isEmpty()) {
				JFileChooser jfc = new JFileChooser();
				jfc.setFileFilter(new FileNameExtensionFilter("WinAmp equilizer file (EQF)", "eqf"));
				jfc.setMultiSelectionEnabled(false);
				jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
				if (JFileChooser.APPROVE_OPTION == jfc.showSaveDialog(this)) {
					handler.onEQExportPreset(jfc.getSelectedFile(), presetName, toPreset());
				}
			}
		});

		btnReset.addActionListener(actEvent -> {
			handler.onEQReset();
		});
	}

	public void setGainValue(double gain) {
		this.gainSlider.setValue((int) Math.round(gain * 1000));
	}

	public void setBandValue(int bandNum, double value) {
		this.bandSliders[bandNum].setValue((int) Math.round(value * 1000));
	}

	public void setState(EqualizerState eqState) {
		cbEnabled.setSelected(eqState.isEnabled());
		setPreset(eqState);
	}

	public void setPreset(EqualizerPreset preset) {
		gainSlider.setValue(preset.getGain());
		int[] bandSliderStates = preset.getBands();
		for (int i = 0; i < bandSliders.length; i++) {
			bandSliders[i].setValue(bandSliderStates[i]);
		}
	}

	protected EqualizerPreset toPreset() {
		return EqualizerPreset.builder()
				.bands(Stream.of(bandSliders).mapToInt(JSlider::getValue).toArray())
				.gain(gainSlider.getValue())
				.build();
	}

	public void setEQEnabled(boolean enabled) {
		cbEnabled.setSelected(enabled);
	}
}
