package x.mvmn.sonivm;

import java.awt.Desktop;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import javax.imageio.ImageIO;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;

import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatLightLaf;

import x.mvmn.sonivm.ui.SonivmController;
import x.mvmn.sonivm.ui.SonivmUI;
import x.mvmn.sonivm.ui.util.swing.SwingUtil;

@SpringBootApplication
@Component
public class Sonivm implements Runnable {

	private static final Logger LOGGER = Logger.getLogger(Sonivm.class.getCanonicalName());

	public static BufferedImage sonivmIcon;

	@Autowired
	private SonivmController sonivmController;

	@Autowired
	private SonivmUI sonivmUI;

	public static void main(String[] args) {
		initConsoleLogging();

		// Spring Boot makes app headless by default
		System.setProperty("java.awt.headless", "false");
		// Enable macOS native menu bar usage
		System.setProperty("apple.laf.useScreenMenuBar", "true");
		// Make sure macOS closes all Swing windows on app quit
		System.setProperty("apple.eawt.quitStrategy", "CLOSE_ALL_WINDOWS");

		try {
			sonivmIcon = ImageIO.read(Sonivm.class.getResourceAsStream("/sonivm_logo.png"));
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, "Failed to read Sonivm icon image", e);
			sonivmIcon = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
		}

		SwingUtil.runOnEDT(() -> initTaskbarIcon(), true);

		// Install FlatLaF look&feels
		SwingUtil.installLookAndFeels(true, FlatLightLaf.class, FlatIntelliJLaf.class, FlatDarkLaf.class, FlatDarculaLaf.class);

		// Ensure data folder exists
		File userHome = new File(System.getProperty("user.home"));
		File appHomeFolder = new File(userHome, ".sonivm");
		if (!appHomeFolder.exists()) {
			appHomeFolder.mkdir();
		}

		System.setProperty("sonivm_home_folder", appHomeFolder.getAbsolutePath());

		// Run the app
		Sonivm launcher = SpringApplication.run(Sonivm.class, args).getBean(Sonivm.class);
		SwingUtil.runOnEDT(launcher::run, false);
	}

	private static void initConsoleLogging() {
		ConsoleHandler handler = new ConsoleHandler();
		handler.setFormatter(new SimpleFormatter());
		Logger rootLogger = Logger.getLogger("x.mvmn.sonivm");
		rootLogger.setLevel(Level.INFO);
		rootLogger.addHandler(handler);
		handler.setLevel(Level.INFO);
	}

	private static void initTaskbarIcon() {
		try {
			Class<?> util = Class.forName("com.apple.eawt.Application");
			Method getApplication = util.getMethod("getApplication", new Class[0]);
			Object application = getApplication.invoke(util);
			Method setDockIconImage = util.getMethod("setDockIconImage", new Class[] { Image.class });
			setDockIconImage.invoke(application, sonivmIcon);

			// Taskbar.getTaskbar().setIconImage(sonivmIcon); // Requires Java 1.9+
		} catch (ClassNotFoundException cnfe) {
			LOGGER.info("Not macOS, or on Java9+ - can't register dock icon image Java8/macOS style due to class not found: "
					+ cnfe.getMessage() + ". Trying Java9+ way.");
			initTaskbarIconJava9();
		} catch (Throwable t) {
			LOGGER.log(Level.WARNING, "Failed to set main macOS doc icon.", t);
		}
	}

	private static void initTaskbarIconJava9() {
		try {
			Class<?> util = Class.forName("java.awt.Taskbar");
			Method getTaskbar = util.getMethod("getTaskbar", new Class[0]);
			Object taskbarInstance = getTaskbar.invoke(null);
			Method setIconImage = util.getMethod("setIconImage", new Class[] { Image.class });
			setIconImage.invoke(taskbarInstance, sonivmIcon);
		} catch (ClassNotFoundException cnfe) {
			LOGGER.info("Class not found on performing java.awt.Taskbar.getTaskbar().setIconImage: " + cnfe.getMessage());
		} catch (Throwable t) {
			LOGGER.log(Level.WARNING, "Failed to set taskbar icon.", t);
		}
	}

	private void initQuitHandler() {
		try {
			Class<?> quitHandlerClass = Class.forName("com.apple.eawt.QuitHandler");
			Object quitHandler = Proxy.newProxyInstance(Sonivm.class.getClassLoader(), new Class[] { quitHandlerClass },
					new InvocationHandler() {

						@Override
						public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
							try {
								sonivmController.onQuit();
								LOGGER.info("Calling macOS specific QuitResponse.performQuit()");
								Object quitResponse = args[1];
								Method performQuitMethod = quitResponse.getClass().getMethod("performQuit");
								performQuitMethod.invoke(quitResponse);
							} catch (Throwable t) {
								LOGGER.log(Level.SEVERE, "Error in quit handler", t);
							}
							return null;
						}
					});

			Class<?> util = Class.forName("com.apple.eawt.Application");
			Method getApplication = util.getMethod("getApplication", new Class[0]);
			Object application = getApplication.invoke(util);
			Method setQuitHandler = util.getMethod("setQuitHandler", new Class[] { quitHandlerClass });
			setQuitHandler.invoke(application, quitHandler);
		} catch (ClassNotFoundException cnfe) {
			LOGGER.info("Not macOS, or on Java9+ - can't register quit handler Java8/macOS style due to class not found: "
					+ cnfe.getMessage() + ". Trying Java9+ way.");
			initQuitHandlerJava9();
		} catch (Throwable t) {
			LOGGER.log(Level.WARNING, "Failed to register quit handler", t);
		}
	}

	private void initQuitHandlerJava9() {
		// Requires Java 1.9+
		try {
			Class<?> quitHandlerClass = Class.forName("java.awt.desktop.QuitHandler");
			Desktop desktop = Desktop.getDesktop();
			Method setQuitHandler = desktop.getClass().getMethod("setQuitHandler", new Class[] { quitHandlerClass });

			Object quitHandler = Proxy.newProxyInstance(Sonivm.class.getClassLoader(), new Class[] { quitHandlerClass },
					new InvocationHandler() {

						@Override
						public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
							try {
								sonivmController.onQuit();
								LOGGER.info("Calling QuitResponse.performQuit()");
								Object quitResponse = args[1];
								Method performQuitMethod = quitResponse.getClass().getMethod("performQuit");
								performQuitMethod.invoke(quitResponse);
							} catch (Throwable t) {
								LOGGER.log(Level.SEVERE, "Error in quit handler", t);
							}
							return null;
						}
					});
			setQuitHandler.invoke(desktop, quitHandler);

			// desktop.setQuitHandler(new QuitHandler() {
			// @Override
			// public void handleQuitRequestWith(QuitEvent e, QuitResponse response) {
			// sonivmController.onQuit();
			// response.performQuit();
			// }
			// });
		} catch (Throwable t) {
			LOGGER.log(Level.WARNING, "Failed to register Java9 quit handler", t);
		}
	}

	public void run() {
		SwingUtil.prefSizeRatioOfScreenSize(sonivmUI.getMainWindow(), 3f / 4f);
		sonivmController.onBeforeUiPack();
		sonivmUI.getMainWindow().pack();
		SwingUtil.moveToScreenCenter(sonivmUI.getMainWindow());
		sonivmController.onBeforeUiSetVisible();

		initQuitHandler();
	}
}
