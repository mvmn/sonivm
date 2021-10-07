package x.mvmn.sonivm.ui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.LayoutManager;
import java.awt.Toolkit;
import java.awt.Window;
import java.beans.PropertyChangeEvent;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JPanel;
import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import javax.swing.text.NumberFormatter;

import x.mvmn.sonivm.ui.ExceptionDisplayer.ErrorData;
import x.mvmn.sonivm.util.UnsafeOperation;

public class SwingUtil {

	public static void performSafely(final UnsafeOperation operation) {
		new Thread(() -> {
			try {
				operation.run();
			} catch (final Exception e) {
				e.printStackTrace();
				showError("Error occurred: ", e);
			}
		}).start();
	}

	public static void performSafely(final UnsafeOperation operation, final Runnable finalOp, boolean finalOpSwingThread) {
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

	public static void setLookAndFeel(String lookAndFeelName) {
		Arrays.stream(UIManager.getInstalledLookAndFeels())
				.filter(lnf -> lnf.getName().equals(lookAndFeelName))
				.findAny()
				.ifPresent(lnf -> {
					try {
						if (!UIManager.getLookAndFeel().getName().equals(lnf.getName())) {
							UIManager.setLookAndFeel(lnf.getClassName());
							updateComponentTreeUIForAllWindows();
						}
					} catch (Exception error) {
						showError("Error setting look&feel to " + lookAndFeelName, error);
					}
				});
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

		public JPanel panel() {
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
}
