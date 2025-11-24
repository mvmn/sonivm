package x.mvmn.sonivm.ui;

import java.awt.event.ActionEvent;
import java.util.function.Consumer;

import javax.swing.AbstractAction;

public class AbstractActionAdaptor extends AbstractAction {
	private static final long serialVersionUID = -5459199062562211681L;
	private Consumer<ActionEvent> handler;

	public static AbstractActionAdaptor of(Consumer<ActionEvent> handler) {
		return new AbstractActionAdaptor(handler);
	}

	public static AbstractActionAdaptor of(Runnable handler) {
		return new AbstractActionAdaptor(e -> handler.run());
	}

	public AbstractActionAdaptor(Consumer<ActionEvent> handler) {
		this.handler = handler;
	}

	public AbstractActionAdaptor(String name, Consumer<ActionEvent> handler) {
		super(name);
		this.handler = handler;
	}

	@Override
	public void actionPerformed(ActionEvent actEvent) {
		handler.accept(actEvent);
	}
}
