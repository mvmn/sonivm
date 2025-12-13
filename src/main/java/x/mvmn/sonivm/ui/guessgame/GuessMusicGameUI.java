package x.mvmn.sonivm.ui.guessgame;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.border.Border;

import x.mvmn.sonivm.PlaybackController;
import x.mvmn.sonivm.PlaybackListener;
import x.mvmn.sonivm.audio.AudioFileInfo;
import x.mvmn.sonivm.audio.PlaybackState;
import x.mvmn.sonivm.playqueue.PlaybackQueueEntry;
import x.mvmn.sonivm.playqueue.PlaybackQueueService;
import x.mvmn.sonivm.ui.util.swing.SwingUtil;

public class GuessMusicGameUI extends JFrame implements PlaybackListener {

	protected JTextField tfScore = new JTextField("0");
	protected PlaybackQueueService playbackQueueService;

	protected JButton btnStart = new JButton("Start new game");
	protected JTabbedPane tabPane = new JTabbedPane();
	protected PlaybackController playbackController;

	protected volatile long guessedHard = 0;
	protected volatile long guessedNormal = 0;
	protected volatile long failedGuesses = 0;

	protected volatile int correctOptNormal = -1;
	protected volatile int correctOptHard = -1;

	protected volatile boolean seekNeeded = false;

	protected JButton[] options = new JButton[8];
	protected final Random random = new Random(System.currentTimeMillis());

	public GuessMusicGameUI(PlaybackQueueService playbackQueueService, PlaybackController playbackController) {
		super("Guess the music game");
		this.getContentPane().setLayout(new BorderLayout());
		this.playbackQueueService = playbackQueueService;
		this.playbackController = playbackController;

		tfScore.setEditable(false);

		JPanel pnlMedium = new JPanel(new GridLayout(4, 1));
		JPanel pnlHard = new JPanel(new GridLayout(4, 1));

		tabPane.addTab("Normal", pnlMedium);
		tabPane.addTab("Hard", pnlHard);

		JPanel[] btnPanels = new JPanel[] { pnlMedium, pnlHard };
		for (int i = 0; i < options.length; i++) {
			options[i] = new JButton();
			btnPanels[i / 4].add(options[i]);
		}

		for (int i = 0; i < 4; i++) {
			int idx = i;
			options[i].addActionListener(e -> {
				if (idx == correctOptNormal) {
					addScore(1);
				} else {
					addScore(0);
				}
				onGuessComplete();
			});
			options[4 + i].addActionListener(e -> {
				if (idx == correctOptHard) {
					addScore(2);
				} else {
					addScore(0);
				}
				onGuessComplete();
			});
		}

		JButton btnPlayPause = new JButton("play/pause");
		btnPlayPause.addActionListener(e -> {
			playbackController.onPlayPause();
		});
		JPanel pnlTop = new JPanel(new GridLayout(2, 1));
		pnlTop.add(btnPlayPause);
		pnlTop.add(SwingUtil.withTitle(tfScore, "Score"));
		this.getContentPane().add(pnlTop, BorderLayout.NORTH);
		this.getContentPane().add(tabPane, BorderLayout.CENTER);
		this.getContentPane().add(btnStart, BorderLayout.SOUTH);

		btnStart.addActionListener(e -> startNewGame());
		startNewGame();

		playbackController.addPlaybackListener(this);
	}

	protected void startNewGame() {
		guessedHard = 0;
		guessedNormal = 0;
		failedGuesses = 0;

		tfScore.setText("New game");

		switchToNextTrack();

		initGuesses();
	}

	protected void initGuesses() {
		if (playbackQueueService.getQueueSize() < 4) {
			return;
		}
		if (playbackQueueService.getCurrentQueuePosition() < 0) {
			playbackQueueService.setCurrentQueuePosition(0);
		}

		PlaybackQueueEntry curEntry = playbackQueueService.getCurrentEntry();

		int skip = playbackQueueService.getCurrentQueuePosition();
		{
			List<PlaybackQueueEntry> opts = new ArrayList<>(4);
			random.ints(0, playbackQueueService.getQueueSize())
					.distinct()
					.filter(v -> v != skip)
					.limit(3)
					.mapToObj(playbackQueueService::getEntryByIndex)
					.forEach(opts::add);
			correctOptNormal = random.nextInt(4);
			opts.add(correctOptNormal, curEntry);

			for (int i = 0; i < 4; i++) {
				PlaybackQueueEntry entry = opts.get(i);
				options[i].setText(entry.toDisplayStr());
			}
		}

		{
			List<PlaybackQueueEntry> opts = new ArrayList<>(4);
			int[] hardChoices = playbackQueueService
					.findTracks(t -> t.getArtist() != null && t.getArtist().equalsIgnoreCase(curEntry.getArtist()));
			if (hardChoices.length > 1) {
				random.ints(0, hardChoices.length)
						.distinct()
						.filter(v -> hardChoices[v] != skip)
						.limit(Math.min(hardChoices.length - 1, 3))
						.map(v -> hardChoices[v])
						.mapToObj(playbackQueueService::getEntryByIndex)
						.forEach(opts::add);
			}
			correctOptHard = random.nextInt(4);
			opts.add(correctOptHard, curEntry);

			int i = 0;
			for (PlaybackQueueEntry opt : opts) {
				options[4 + i++].setText(opt.toDisplayStr());
			}
			while (i < 4) {
				options[4 + i++].setText(opts.get(random.nextInt(opts.size())).toDisplayStr());
			}

		}

		repaintButtons();
	}

	private void repaintButtons() {
		for (JButton opt : options) {
			opt.invalidate();
			opt.revalidate();
			opt.repaint();
		}
	}

	protected void addScore(int val) {
		switch (val) {
			case 0:
				failedGuesses++;
			break;
			case 1:
				guessedNormal++;
			break;
			case 2:
				guessedHard++;
			break;
		}
		long totalGuessed = guessedNormal + guessedHard;
		tfScore.setText(String.format("Guessed %d hard, %d normal. Failed %d guesses. Total guessed %d (%d %%)", guessedHard, guessedNormal,
				failedGuesses, totalGuessed, (totalGuessed * 100) / (totalGuessed + failedGuesses)));
		tfScore.invalidate();
		tfScore.revalidate();
		tfScore.repaint();
	}

	protected void switchToNextTrack() {
		Thread trackSwitch = new Thread() {
			public void run() {
				playbackController.onTrackSelect(random.nextInt(playbackQueueService.getQueueSize()));
				seekNeeded = true;
			}
		};
		trackSwitch.start();
		try {
			trackSwitch.join();
		} catch (InterruptedException e) {
			Thread.interrupted();
			throw new RuntimeException(e);
		}
	}

	protected void onGuessComplete() {
		Border border = options[0].getBorder();

		new Thread() {
			public void run() {
				SwingUtil.runOnEDT(() -> {
					for (JButton opt : options) {
						opt.setEnabled(false);
					}
					for (int i = 0; i < 4; i++) {
						options[i].setBorder(BorderFactory.createLineBorder(i == correctOptNormal ? Color.GREEN : Color.RED, 3, true));
						options[4 + i].setBorder(BorderFactory.createLineBorder(i == correctOptHard ? Color.GREEN : Color.RED, 3, true));
					}
					repaintButtons();
				}, false);
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					Thread.interrupted();
					throw new RuntimeException(e);
				}
				SwingUtil.runOnEDT(() -> {
					for (JButton opt : options) {
						opt.setBorder(border);
						opt.setEnabled(true);
					}
					repaintButtons();
					switchToNextTrack();
					initGuesses();
				}, false);
			}
		}.start();
	}

	@Override
	public void onPlaybackStateChange(PlaybackState stopped) {

	}

	@Override
	public void onPlaybackError(String errorMessage) {

	}

	@Override
	public void onPlaybackProgress(long playbackPositionMillis, int totalDurationSeconds) {

	}

	@Override
	public void onPlaybackStart(AudioFileInfo audioInfo, PlaybackQueueEntry currentTrack) {
		if (seekNeeded) {
			playbackController.onSeek((currentTrack.getDuration() * 10) / 3);
			seekNeeded = false;
		}
	}
}
