package x.mvmn.sonivm.ui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSlider;
import javax.swing.filechooser.FileNameExtensionFilter;

import x.mvmn.sonivm.eq.EqualizerPresetService;
import x.mvmn.sonivm.eq.SonivmEqualizerService;
import x.mvmn.sonivm.eq.model.EqualizerPreset;
import x.mvmn.sonivm.eq.model.EqualizerState;
import x.mvmn.sonivm.ui.util.swing.SwingUtil;
import x.mvmn.sonivm.util.Tuple2;

public class EqualizerWindow extends JFrame {
	private static final long serialVersionUID = -659390575435440032L;

	private static final Logger LOGGER = Logger.getLogger(EqualizerWindow.class.getCanonicalName());
	private static final String[] BAND_LABELS = { "60", "170", "310", "600", "1K", "3K", "6K", "12K", "14K", "16K" };

	private final SonivmEqualizerService eqService;
	private final EqualizerPresetService equalizerPresetService;

	private final JSlider[] bandSliders;
	private final JSlider gainSlider;
	private final JCheckBox cbEnabled;
	private final JButton btnReset;
	private final JButton btnSave;
	private final JButton btnImport;
	private final JButton btnExport;
	private final JButton btnLoad;

	public EqualizerWindow(String title, int bandCount, SonivmEqualizerService eqService, EqualizerPresetService equalizerPresetService) {
		super(title);

		this.setResizable(false);

		this.eqService = eqService;
		this.equalizerPresetService = equalizerPresetService;

		this.cbEnabled = new JCheckBox("EQ on", false);
		cbEnabled.addActionListener(actEvent -> eqService.onEqualizerEnableToggle(cbEnabled.isSelected()));

		bandSliders = new JSlider[bandCount];
		for (int i = 0; i < bandCount; i++) {
			JSlider bandSlider = new JSlider(JSlider.VERTICAL, 0, 1000, 500);
			bandSliders[i] = bandSlider;
			final int bandNumber = i;
			SwingUtil.addValueChangeByUserListener(bandSlider, changeEvent -> this.onBandChange(bandNumber));
			SwingUtil.makeJSliderMoveToClickPoistion(bandSliders[i]);
			SwingUtil.onDoubleClick(bandSlider, e -> bandSlider.setValue(500));
		}

		btnReset = new JButton("Reset");
		btnReset.addActionListener(actEvent -> {
			for (JSlider bandSlider : bandSliders) {
				bandSlider.setValue(500);
			}
			onAllBandsChange();
		});
		btnSave = new JButton("Save...");
		btnSave.addActionListener(actEvent -> {
			String presetName = JOptionPane.showInputDialog(this, "Enter preset name", "");
			if (presetName != null && !presetName.trim().isEmpty()) {
				onSavePreset(presetName, toPreset());
			}
		});
		btnImport = new JButton("Import EQF...");
		btnImport.addActionListener(actEvent -> {
			JFileChooser jfc = new JFileChooser();
			jfc.setFileFilter(new FileNameExtensionFilter("WinAmp equilizer file (EQF)", "eqf"));
			jfc.setMultiSelectionEnabled(false);
			jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
			if (JFileChooser.APPROVE_OPTION == jfc.showOpenDialog(this)) {
				onImportPreset(jfc.getSelectedFile());
			}
		});
		btnExport = new JButton("Export EQF...");
		btnExport.addActionListener(actEvent -> {
			String presetName = JOptionPane.showInputDialog(this, "Enter preset name", "");
			if (presetName != null && !presetName.trim().isEmpty()) {
				JFileChooser jfc = new JFileChooser();
				jfc.setFileFilter(new FileNameExtensionFilter("WinAmp equilizer file (EQF)", "eqf"));
				jfc.setMultiSelectionEnabled(false);
				jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
				if (JFileChooser.APPROVE_OPTION == jfc.showSaveDialog(this)) {
					onExportPreset(jfc.getSelectedFile(), presetName, toPreset());
				}
			}
		});

		btnLoad = new JButton("Load >");
		btnLoad.addActionListener(actEvent -> buildPresetsMenu().show(btnLoad, btnLoad.getWidth(), 0));

		gainSlider = new JSlider(JSlider.VERTICAL, 0, 1000, 500);
		SwingUtil.addValueChangeByUserListener(gainSlider, changeEvent -> this.onGainChange());
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

		this.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentShown(ComponentEvent e) {
				setState(eqService.getCurrentState());
			}
		});

		this.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		SwingUtil.moveToScreenCenter(this);
	}

	protected JPopupMenu buildPresetsMenu() {
		JPopupMenu presetsMenu = new JPopupMenu("Presets");
		equalizerPresetService.listPresets()
				.stream()
				.map(presetName -> new JMenuItem(presetName))
				.peek(jMenuItem -> jMenuItem.addActionListener(actEvent -> onLoadPreset(jMenuItem.getText())))
				.forEach(presetsMenu::add);
		return presetsMenu;
	}

	protected void onGainChange() {
		eqService.setGain(gainSlider.getValue());
	}

	protected void onBandChange(int bandNumber) {
		eqService.onBandChange(bandNumber, bandSliders[bandNumber].getValue());
	}

	protected void onAllBandsChange() {
		for (int i = 0; i < bandSliders.length; i++) {
			onBandChange(i);
		}
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

	private void onSavePreset(String name, EqualizerPreset equalizerPreset) {
		new Thread(() -> {
			try {
				equalizerPresetService.savePreset(name, equalizerPreset);
			} catch (Throwable t) {
				LOGGER.log(Level.WARNING, "Failed to save preset " + name, t);
			}
		}).start();
	}

	private void onLoadPreset(String name) {
		new Thread(() -> {
			try {
				EqualizerPreset preset = equalizerPresetService.loadPreset(name);
				eqService.setPreset(preset);
				SwingUtil.runOnEDT(() -> setPreset(preset), false);
			} catch (Throwable t) {
				LOGGER.log(Level.WARNING, "Failed to load preset " + name, t);
			}
		}).start();
	}

	private void onExportPreset(File file, String name, EqualizerPreset equalizerPreset) {
		new Thread(() -> {
			try (FileOutputStream fos = new FileOutputStream(file)) {
				equalizerPresetService.exportWinAmpEqfPreset(name, equalizerPreset, fos);
			} catch (Throwable t) {
				LOGGER.log(Level.WARNING, "Failed to export WinAmp EQF preset to file " + file.getAbsolutePath(), t);
			}
		}).start();
	}

	private void onImportPreset(File presetFile) {
		if (presetFile.exists() && presetFile.length() == 299) {
			new Thread(() -> {
				try (FileInputStream fis = new FileInputStream(presetFile)) {
					Tuple2<String, EqualizerPreset> presetWithName = equalizerPresetService.importWinAmpEqfPreset(fis);
					equalizerPresetService.savePreset(presetWithName.getA(), presetWithName.getB());
					setPreset(presetWithName.getB());
					SwingUtil.runOnEDT(() -> setPreset(presetWithName.getB()), false);
				} catch (Throwable t) {
					LOGGER.log(Level.WARNING, "Failed to import WinAmp EQF preset from file " + presetFile.getAbsolutePath(), t);
				}
			}).start();
		}
	}
}
