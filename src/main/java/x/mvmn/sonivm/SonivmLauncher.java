package x.mvmn.sonivm;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;

import x.mvmn.sonivm.audio.AudioFileInfo;
import x.mvmn.sonivm.audio.AudioService;
import x.mvmn.sonivm.audio.PlaybackEvent;
import x.mvmn.sonivm.audio.PlaybackEventListener;

@SpringBootApplication
@Component
public class SonivmLauncher implements PlaybackEventListener {

	@Autowired
	private AudioService audioService;

	private volatile JSlider slider;
	private volatile boolean sliderIsDragged;
	private volatile boolean paused;
	private final JFrame mainWindow;
	private final JButton btnOpen;

	public static void main(String[] args) throws Exception {
		System.setProperty("java.awt.headless", "false");
		SpringApplication.run(SonivmLauncher.class, args).getBean(SonivmLauncher.class);
	}

	public SonivmLauncher() {
		mainWindow = new JFrame();
		mainWindow.getContentPane().setLayout(new BorderLayout());

		btnOpen = new JButton("Open...");
		btnOpen.addActionListener(actEvent -> {
			JFileChooser jfc = new JFileChooser();
			if (JFileChooser.APPROVE_OPTION == jfc.showOpenDialog(null)) {
				File file = jfc.getSelectedFile();
				audioService.play(file);
			} else {
				audioService.shutdown();
			}
		});

		mainWindow.getContentPane().add(btnOpen, BorderLayout.CENTER);
		mainWindow.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		mainWindow.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				audioService.shutdown();
			}
		});
		mainWindow.setMinimumSize(new Dimension(400, 300));
		mainWindow.pack();
		mainWindow.setVisible(true);
	}

	private void doSeek() {
		audioService.seek(slider.getValue() * 100);
	}

	private void uiUpdateOnStop() {
		mainWindow.getContentPane().removeAll();
		mainWindow.getContentPane().add(btnOpen, BorderLayout.CENTER);
		mainWindow.invalidate();
		mainWindow.revalidate();
		mainWindow.repaint();
	}

	@Override
	public void handleEvent(PlaybackEvent event) {
		switch (event.getType()) {
			case ERROR:
				System.err.println(event.getErrorType() + ": " + event.getError());
			break;
			case FINISH:
				System.out.println("Playback complete");
				SwingUtilities.invokeLater(this::uiUpdateOnStop);
			break;
			case PROGRESS:
				Long delta = event.getPlaybackPositionMilliseconds();
				int sliderNewPosition = (int) (delta / 100);
				SwingUtilities.invokeLater(() -> {
					if (!sliderIsDragged) {
						slider.getModel().setValue(sliderNewPosition);
					}
				});
			break;
			case START: {
				AudioFileInfo audioInfo = event.getAudioMetadata();
				JButton btnStop = new JButton("Stop");
				btnStop.addActionListener(actEvent -> {
					audioService.stop();
					uiUpdateOnStop();
				});

				JButton btnPlayPause = new JButton("Pause");
				btnPlayPause.addActionListener(actEvent -> {
					if (paused) {
						this.audioService.resume();
					} else {
						this.audioService.pause();
					}
					paused = !paused;
					btnPlayPause.setText(paused ? "Play" : "Pause");
				});

				if (audioInfo.isSeekable()) {
					slider = new JSlider(0, audioInfo.getDurationSeconds().intValue() * 10, 0);
					slider.addChangeListener(actEvent -> {
						if (sliderIsDragged && !slider.getValueIsAdjusting()) {
							doSeek();
						}
					});
					slider.addMouseListener(new MouseAdapter() {
						@Override
						public void mousePressed(MouseEvent e) {
							sliderIsDragged = true;
						}

						@Override
						public void mouseReleased(MouseEvent e) {
							sliderIsDragged = false;
						}

						@Override
						public void mouseClicked(MouseEvent e) {
							doSeek();
						}
					});
					mainWindow.getContentPane().add(slider, BorderLayout.NORTH);
				} else {
					slider = null;
				}
				SwingUtilities.invokeLater(() -> {
					System.out.println("Remove all and add new");
					mainWindow.getContentPane().removeAll();
					JPanel btnPanel = new JPanel(new GridLayout(2, 1));
					btnPanel.add(btnPlayPause);
					btnPanel.add(btnStop);
					mainWindow.getContentPane().add(btnPanel, BorderLayout.SOUTH);
					if (slider != null) {
						mainWindow.getContentPane().add(slider, BorderLayout.NORTH);
					}
					mainWindow.invalidate();
					mainWindow.revalidate();
					mainWindow.repaint();
				});
			}
			break;
		}
	}
}
