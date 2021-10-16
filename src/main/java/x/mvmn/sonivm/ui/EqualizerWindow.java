package x.mvmn.sonivm.ui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;

import org.springframework.beans.factory.annotation.Autowired;

import x.mvmn.sonivm.ui.util.swing.SwingUtil;

public class EqualizerWindow extends JFrame {
	private static final long serialVersionUID = -659390575435440032L;
	// TODO: account for bandCount
	private static final String[] BAND_LABELS = { "60", "170", "310", "600", "1K", "3K", "6K", "12K", "14K", "16K" };
	private JSlider[] bandSliders;
	private JSlider gainSlider;
	private JCheckBox cbEnabled;
	private JButton btnReset;

	@Autowired
	private SonivmEqualizerService changeListener;

	public EqualizerWindow(String title, int bandCount) {
		super(title);

		this.cbEnabled = new JCheckBox("EQ on", false);
		cbEnabled.addActionListener(actEvent -> changeListener.onEqualizerEnableToggle(cbEnabled.isSelected()));

		btnReset = new JButton("Reset");
		btnReset.addActionListener(actEvent -> {
			// gainSlider.setValue(500);
			for (JSlider bandSlider : bandSliders) {
				bandSlider.setValue(500);
			}
		});

		bandSliders = new JSlider[bandCount];
		for (int i = 0; i < bandCount; i++) {
			JSlider bandSlider = new JSlider(JSlider.VERTICAL, 0, 1000, 500);
			bandSliders[i] = bandSlider;
			final int bandNumber = i;
			bandSlider.addChangeListener(changeEvent -> this.onBandChange(bandNumber));
			SwingUtil.makeJSliderMoveToClickPoistion(bandSliders[i]);
			bandSlider.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					if (e.getClickCount() == 2) {
						bandSlider.setValue(500);
					}
				}
			});
		}

		gainSlider = new JSlider(JSlider.VERTICAL, 0, 1000, 300);
		gainSlider.addChangeListener(changeEvent -> this.onGainChange());
		gainSlider.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					gainSlider.setValue(500);
				}
			}
		});
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
		for (int i = 0; i < bandSliders.length; i++) {
			gridBagCnstr.weightx = 1;
			eqPanel.add(new JLabel(" "), gridBagCnstr);
			gridBagCnstr.weightx = 0;
			gridBagCnstr.gridx++;
			eqPanel.add(new JLabel(BAND_LABELS[i], JLabel.CENTER), gridBagCnstr);
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
		this.getContentPane().add(cbEnabled, BorderLayout.NORTH);
		this.getContentPane().add(btnReset, BorderLayout.SOUTH);
		this.pack();
		this.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		SwingUtil.moveToScreenCenter(this);
	}

	protected void onGainChange() {
		changeListener.onGainChange(gainSlider.getValue());
	}

	protected void onBandChange(int bandNumber) {
		changeListener.onBandChange(bandNumber, bandSliders[bandNumber].getValue());
	}
}
