package x.mvmn.sonivm.ui.util.swing;

import java.awt.BorderLayout;
import java.awt.CheckboxMenuItem;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.LayoutManager;
import java.awt.Menu;
import java.awt.MenuItem;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.lang.reflect.InvocationTargetException;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.LookAndFeel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicSliderUI;
import javax.swing.text.JTextComponent;
import javax.swing.text.NumberFormatter;

import x.mvmn.sonivm.ui.ExceptionDisplayer;
import x.mvmn.sonivm.ui.ExceptionDisplayer.ErrorData;
import x.mvmn.sonivm.util.Tuple2;
import x.mvmn.sonivm.util.Tuple4;
import x.mvmn.sonivm.util.UnsafeOperation;

public class SwingUtil {

	private static final Logger LOGGER = Logger.getLogger(SwingUtil.class.getCanonicalName());

	public static void performSafely(final UnsafeOperation<?> operation) {
		new Thread(() -> {
			try {
				operation.run();
			} catch (final Exception e) {
				e.printStackTrace();
				showError("Error occurred: ", e);
			}
		}).start();
	}

	public static void performSafely(final UnsafeOperation<?> operation, final Runnable finalOp, boolean finalOpSwingThread) {
		new Thread(() -> {
			try {
				operation.run();
			} catch (final Exception e) {
				e.printStackTrace();
				showError("Error occurred: ", e);
			} finally {
				if (finalOpSwingThread) {
					SwingUtilities.invokeLater(finalOp);
				} else {
					finalOp.run();
				}
			}
		}).start();
	}

	public static void showError(final String message, final Throwable e) {
		ExceptionDisplayer.INSTANCE.accept(ErrorData.builder().message(message).error(e).build());
	}

	public static void moveToScreenCenter(final Component component) {
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		Dimension componentSize = component.getSize();
		int newComponentX = screenSize.width - componentSize.width;
		if (newComponentX >= 0) {
			newComponentX = newComponentX / 2;
		} else {
			newComponentX = 0;
		}
		int newComponentY = screenSize.height - componentSize.height;
		if (newComponentY >= 0) {
			newComponentY = newComponentY / 2;
		} else {
			newComponentY = 0;
		}
		component.setLocation(newComponentX, newComponentY);
	}

	public static JPanel twoComponentPanel(Component a, Component b) {
		JPanel result = new JPanel(new GridLayout(1, 2));

		result.add(a);
		result.add(b);

		return result;
	}

	public static <T extends Component> T prefSizeRatioOfScreenSize(T component, float ratio) {
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		Dimension prefSize = new Dimension((int) (screenSize.width * ratio), (int) (screenSize.height * ratio));
		component.setPreferredSize(prefSize);
		return component;
	}

	public static <T extends Component> T minPrefWidth(T component, int minimumPreferredWidth) {
		component.setPreferredSize(
				new Dimension(Math.max(minimumPreferredWidth, component.getPreferredSize().width), component.getPreferredSize().height));
		return component;
	}

	public static JFormattedTextField numericOnlyTextField(Long initialValue, Long min, Long max, boolean allowNegative) {
		NumberFormatter formatter = new NumberFormatter(NumberFormat.getInstance());
		formatter.setValueClass(Long.class);
		formatter.setMinimum(min != null ? min : (allowNegative ? Long.MIN_VALUE : 0));
		formatter.setMaximum(max != null ? max : Long.MAX_VALUE);
		formatter.setAllowsInvalid(false);
		formatter.setCommitsOnValidEdit(true);
		JFormattedTextField txf = new JFormattedTextField(formatter);
		txf.setValue(initialValue != null ? initialValue : 0L);

		return txf;
	}

	public static String getLookAndFeelName(LookAndFeel lnf) {
		return Arrays.stream(UIManager.getInstalledLookAndFeels())
				.filter(lnfInfo -> lnfInfo.getClassName().equals(lnf.getClass().getCanonicalName()))
				.map(LookAndFeelInfo::getName)
				.findAny()
				.orElse(null);
	}

	public static void updateComponentTreeUIForAllWindows() {
		for (Frame frame : Frame.getFrames()) {
			updateComponentTreeUI(frame);
		}
	}

	public static void updateComponentTreeUI(Window window) {
		for (Window childWindow : window.getOwnedWindows()) {
			updateComponentTreeUI(childWindow);
		}
		SwingUtilities.updateComponentTreeUI(window);
	}

	public static void setLookAndFeel(String lookAndFeelName, boolean blockUntilDone) {
		Arrays.stream(UIManager.getInstalledLookAndFeels())
				.filter(lnf -> lnf.getName().equals(lookAndFeelName))
				.findAny()
				.ifPresent(lnf -> {
					Runnable setLnF = () -> {
						try {
							if (!UIManager.getLookAndFeel().getName().equals(lnf.getName())) {
								UIManager.setLookAndFeel(lnf.getClassName());
								updateComponentTreeUIForAllWindows();
							}
						} catch (Exception error) {
							showError("Error setting look&feel to " + lookAndFeelName, error);
						}
					};
					if (SwingUtilities.isEventDispatchThread()) {
						setLnF.run();
					} else {
						if (blockUntilDone) {
							try {
								SwingUtilities.invokeAndWait(setLnF);
							} catch (InvocationTargetException e) {
								showError("Error setting look&feel to " + lookAndFeelName, e);
							} catch (InterruptedException e) {
								Thread.interrupted();
								throw new RuntimeException(e);
							}
						} else {
							SwingUtilities.invokeLater(setLnF);
						}
					}
				});
	}

	public static void installLookAndFeels(boolean blockUntilDone, Class<?>... lookAndFeels) {
		try {
			Runnable install = () -> {
				Stream.of(lookAndFeels).forEach(lafClass -> {
					try {
						UIManager.installLookAndFeel(lafClass.getSimpleName(), lafClass.getCanonicalName());
					} catch (Exception e) {
						LOGGER.log(Level.WARNING, "Failed to install look and feel " + lafClass.getCanonicalName(), e);
					}
				});
			};
			if (blockUntilDone) {
				SwingUtilities.invokeAndWait(install);
			} else {
				SwingUtilities.invokeLater(install);
			}
		} catch (InterruptedException e) {
			Thread.interrupted();
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			LOGGER.log(Level.WARNING, "Failed to install look and feels", e);
		}
	}

	public static <T extends JTextComponent> T bind(T textComponent, Consumer<DocumentEvent> listener) {
		textComponent.getDocument().addDocumentListener(onChange(listener));
		return textComponent;
	}

	public static <T extends JFormattedTextField> T bind(T ftf, Consumer<PropertyChangeEvent> listener) {
		ftf.addPropertyChangeListener("value", e -> listener.accept(e));
		return ftf;
	}

	public static <T extends JTextComponent> T bindNumeric(T textComponent, Consumer<Long> listener) {
		textComponent.getDocument().addDocumentListener(onChange(e -> {
			String text = textComponent.getText();
			if (text != null) {
				text = text.replaceAll("[^0-9]+", "");
				if (!text.isEmpty()) {
					listener.accept(Long.parseLong(text));
				}
			}
		}));
		return textComponent;
	}

	public static <T extends AbstractButton> T bind(T button, Consumer<Boolean> listener) {
		button.addItemListener(e -> listener.accept(button.isSelected()));
		return button;
	}

	public static DocumentListener onChange(Consumer<DocumentEvent> listener) {
		return new DocumentListener() {
			@Override
			public void removeUpdate(DocumentEvent e) {
				listener.accept(e);
			}

			@Override
			public void insertUpdate(DocumentEvent e) {
				listener.accept(e);
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				listener.accept(e);
			}
		};
	}

	public static Component onDoubleClick(Component component, Consumer<MouseEvent> actEvent) {
		component.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					actEvent.accept(e);
				}
			}
		});
		return component;
	}

	public static class PanelBuilder {
		private final JPanel panel;

		public PanelBuilder(Function<JPanel, LayoutManager> layoutFactory) {
			this.panel = new JPanel();
			this.panel.setLayout(layoutFactory.apply(this.panel));
		}

		public PanelBuilder add(Component cmp) {
			this.panel.add(cmp);
			return this;
		}

		public PanelBuilder add(Component cmp, Object constraint) {
			this.panel.add(cmp, constraint);
			return this;
		}

		public PanelBuilder addNorth(Component cmp) {
			this.panel.add(cmp, BorderLayout.NORTH);
			return this;
		}

		public PanelBuilder addSouth(Component cmp) {
			this.panel.add(cmp, BorderLayout.SOUTH);
			return this;
		}

		public PanelBuilder addEast(Component cmp) {
			this.panel.add(cmp, BorderLayout.EAST);
			return this;
		}

		public PanelBuilder addWest(Component cmp) {
			this.panel.add(cmp, BorderLayout.WEST);
			return this;
		}

		public PanelBuilder addCenter(Component cmp) {
			this.panel.add(cmp, BorderLayout.CENTER);
			return this;
		}

		public PanelBuilder addSeparator(boolean vertical) {
			this.panel.add(new JSeparator(vertical ? SwingConstants.VERTICAL : SwingConstants.HORIZONTAL));
			return this;
		}

		public JPanel build() {
			return panel;
		}
	}

	public static PanelBuilder panel(Function<JPanel, LayoutManager> layoutFactory) {
		return new PanelBuilder(layoutFactory);
	}

	public static PanelBuilder panel(Supplier<LayoutManager> layoutFactory) {
		return panel(pnl -> layoutFactory.get());
	}

	public static <T extends JComponent> T withTitle(T c, String title) {
		c.setBorder(BorderFactory.createTitledBorder(title));
		return c;
	}

	public static void runOnEDT(Runnable task, boolean blockUntilDone) {
		if (SwingUtilities.isEventDispatchThread()) {
			task.run();
		} else {
			if (!blockUntilDone) {
				SwingUtilities.invokeLater(task);
			} else {
				try {
					SwingUtilities.invokeAndWait(task);
				} catch (InvocationTargetException e) {
					throw new RuntimeException(e);
				} catch (InterruptedException e) {
					Thread.interrupted();
					throw new RuntimeException(e);
				}
			}
		}
	}

	public static void makeJSliderMoveToClickPoistion(JSlider slider) {
		slider.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseReleased(MouseEvent e) {
				BasicSliderUI ui = (BasicSliderUI) slider.getUI();
				switch (slider.getOrientation()) {
					case SwingConstants.VERTICAL:
						slider.setValue(ui.valueForYPosition(e.getY()));
					break;
					case SwingConstants.HORIZONTAL:
						slider.setValue(ui.valueForXPosition(e.getX()));
					break;
				}
			}
		});
	}

	public static void makeJProgressBarMoveToClickPosition(JProgressBar progressBar, Consumer<Integer> action) {
		progressBar.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				int newValue = (int) Math.round(((double) e.getX() / (double) progressBar.getWidth()) * progressBar.getMaximum());
				progressBar.setValue(newValue);
				action.accept(newValue);
			}
		});
	}

	public static Color modifyColor(Color color, int redDelta, int greenDelta, int blueDelta) {
		return new Color(normalizeColorComponent(color.getRed() + redDelta), normalizeColorComponent(color.getGreen() + greenDelta),
				normalizeColorComponent(color.getBlue() + blueDelta), color.getAlpha());
	}

	private static int normalizeColorComponent(int value) {
		if (value < 0) {
			value = 0;
		}
		if (value > 255) {
			value = 255;
		}
		return value;
	}

	public static JComponent withEmptyBorder(JComponent cmp, int top, int left, int bottom, int right) {
		cmp.setBorder(BorderFactory.createEmptyBorder(top, left, bottom, right));
		return cmp;
	}

	public static MenuItem menuItem(String text, ActionListener handler) {
		MenuItem menuItem = new MenuItem(text);
		menuItem.addActionListener(handler);
		return menuItem;
	}

	public static CheckboxMenuItem checkboxMenuItem(String text, boolean state, ItemListener handler) {
		CheckboxMenuItem menuItem = new CheckboxMenuItem(text, state);
		menuItem.addItemListener(handler);
		return menuItem;
	}

	public static <T> Menu radiobuttonsSubmenu(String name, Map<String, T> options, T currentVal, Consumer<T> onOptionSelect) {
		Menu menu = new Menu(name);

		for (Map.Entry<String, T> option : options.entrySet()) {
			CheckboxMenuItem menuItem = new CheckboxMenuItem(option.getKey(), option.getValue().equals(currentVal));
			menuItem.addItemListener(event -> {
				T selectedValue = null;
				for (int i = 0; i < menu.getItemCount(); i++) {
					CheckboxMenuItem cbmi = (CheckboxMenuItem) menu.getItem(i);
					boolean match = cbmi == menuItem;
					cbmi.setState(match);
					if (match) {
						selectedValue = options.get(cbmi.getLabel());
					}
				}
				onOptionSelect.accept(selectedValue);
			});
			menu.add(menuItem);
		}

		return menu;
	}

	public static void showAndBringToFront(Window window) {
		window.setVisible(true);
		window.toFront();
		window.requestFocus();
	}

	public static void removeAllActionListeners(AbstractButton button) {
		Stream.of(button.getActionListeners()).forEach(actListener -> button.removeActionListener(actListener));
	}

	public static Tuple4<Boolean, String, Point, Dimension> getWindowState(Component window) {
		Point locationGlobal = window.getLocation();
		Dimension size = window.getSize();

		GraphicsDevice containingDevice = Stream.of(GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices())
				.filter(display -> display.getDefaultConfiguration().getBounds().contains(locationGlobal))
				.findAny()
				.orElse(null);
		if (containingDevice == null) {
			throw new RuntimeException("None of the screens contain " + locationGlobal);
		}

		int screenSpecificX = Math.abs(containingDevice.getDefaultConfiguration().getBounds().x - locationGlobal.x);
		int screenSpecificY = Math.abs(containingDevice.getDefaultConfiguration().getBounds().y - locationGlobal.y);

		return Tuple4.<Boolean, String, Point, Dimension> builder()
				.a(window.isVisible())
				.b(containingDevice.getIDstring())
				.c(new Point(screenSpecificX, screenSpecificY))
				.d(size)
				.build();
	}

	public static void restoreWindowState(Window window, Tuple4<Boolean, String, Point, Dimension> windowState) {
		Optional<GraphicsDevice> screenOpt = Stream.of(GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices())
				.filter(dev -> windowState.getB().equals(dev.getIDstring()))
				.findAny();

		boolean sameScreen = screenOpt.isPresent();
		GraphicsDevice screen = screenOpt.orElse(GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice());
		Rectangle screenBounds = screen.getDefaultConfiguration().getBounds();

		int x = windowState.getC().x;
		int width = windowState.getD().width;
		int y = windowState.getC().y;
		int height = windowState.getD().height;
		if (!sameScreen) {
			// Original screen not available
			{
				Tuple2<Integer, Integer> xAndWidth = fit(screenBounds.width, x, width);
				x = xAndWidth.getA();
				width = xAndWidth.getB();
			}
			{
				Tuple2<Integer, Integer> yAndHeight = fit(screenBounds.height, y, height);
				y = yAndHeight.getA();
				height = yAndHeight.getB();
			}
		}

		window.setSize(width, height);
		window.setLocation(x + screenBounds.x, y + screenBounds.y);
		window.setVisible(windowState.getA());
	}

	private static Tuple2<Integer, Integer> fit(int totalSize, int location, int size) {
		if (location + size > totalSize) {
			// If won't fit
			if (size > totalSize) {
				// If we can't fit with original size at all - move all the way to the beginning and reduce size
				location = 0;
				size = totalSize;
			} else {
				// If we can fit with original size by moving toward the beginning
				location = totalSize - size;
			}
		}
		return Tuple2.<Integer, Integer> builder().a(location).b(size).build();
	}
}
