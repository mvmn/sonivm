package x.mvmn.sonivm;

import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
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
	private volatile int sliderInitialPosition;
	private volatile long playTimeSum;
	private volatile boolean sliderIsDragged;

	public static void main(String[] args) throws Exception {
		System.setProperty("java.awt.headless", "false");
		SpringApplication.run(SonivmLauncher.class, args).getBean(SonivmLauncher.class).openFile();
	}

	public void openFile() {
		SwingUtilities.invokeLater(() -> {
			JFileChooser jfc = new JFileChooser();
			if (JFileChooser.APPROVE_OPTION == jfc.showOpenDialog(null)) {
				File file = jfc.getSelectedFile();
				audioService.play(file);
			} else {
				audioService.shutdown();
			}
		});
	}

	private void doSeek() {
		audioService.seek(slider.getValue() * 100);
		sliderInitialPosition = slider.getValue();
		playTimeSum = 0;
	}

	@Override
	public void handleEvent(PlaybackEvent event) {
		switch (event.getType()) {
			case ERROR:
				System.err.println(event.getErrorType() + ": " + event.getError());
			break;
			case FINISH:
				System.out.println("Playback complete");
			break;
			case PROGRESS:
				Long delta = event.getPlaybackTimeDelta();
				playTimeSum += delta;
				int sliderNewPosition = (int) (sliderInitialPosition + playTimeSum / 100000);
				SwingUtilities.invokeLater(() -> {
					if (!sliderIsDragged) {
						slider.getModel().setValue(sliderNewPosition);
					}
				});
			break;
			case START: {
				AudioFileInfo audioInfo = event.getAudioMetadata();
				JFrame wnd = new JFrame();
				JButton btnStop = new JButton("Stop");
				btnStop.addActionListener(actEvent -> {
					audioService.stop();
					audioService.shutdown();
					wnd.setVisible(false);
					wnd.dispose();
				});

				wnd.getContentPane().setLayout(new BorderLayout());
				wnd.getContentPane().add(btnStop, BorderLayout.CENTER);
				if (audioInfo.isSeekable()) {
					slider = new JSlider(0, audioInfo.getDurationSeconds().intValue() * 10, 0);
					sliderInitialPosition = 0;
					playTimeSum = 0;
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
					wnd.getContentPane().add(slider, BorderLayout.NORTH);
				}
				wnd.pack();
				wnd.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
				wnd.setVisible(true);
				wnd.addWindowListener(new WindowListener() {

					@Override
					public void windowClosing(WindowEvent e) {
						audioService.shutdown();
					}

					@Override
					public void windowOpened(WindowEvent e) {}

					@Override
					public void windowIconified(WindowEvent e) {}

					@Override
					public void windowDeiconified(WindowEvent e) {}

					@Override
					public void windowDeactivated(WindowEvent e) {}

					@Override
					public void windowClosed(WindowEvent e) {}

					@Override
					public void windowActivated(WindowEvent e) {}
				});

			}
			break;
		}
	}
}
