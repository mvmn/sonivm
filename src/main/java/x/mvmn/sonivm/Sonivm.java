package x.mvmn.sonivm;

import java.io.File;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;

import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatLightLaf;

import x.mvmn.sonivm.ui.SonivmUI;
import x.mvmn.sonivm.ui.util.swing.SwingUtil;

@SpringBootApplication
@Component
public class Sonivm implements Runnable {

	private static final Logger LOGGER = Logger.getLogger(Sonivm.class.getCanonicalName());

	@Autowired
	private SonivmUI sonivmUI;

	public static void main(String[] args) {
		initConsoleLogging();
		LOGGER.info("Sonivm startup");

		// Change application display name in macOS
		System.setProperty("apple.awt.application.name", "Sonivm");
		// Spring Boot makes app headless by default
		System.setProperty("java.awt.headless", "false");
		// Enable macOS native menu bar usage
		System.setProperty("apple.laf.useScreenMenuBar", "true");
		// Make sure macOS closes all Swing windows on app quit
		System.setProperty("apple.eawt.quitStrategy", "CLOSE_ALL_WINDOWS");

		LOGGER.info("Installing Look&Feels");
		// Install FlatLaF look&feels
		SwingUtil.installLookAndFeels(true, FlatLightLaf.class, FlatIntelliJLaf.class, FlatDarkLaf.class, FlatDarculaLaf.class);

		// Ensure data folder exists
		File userHome = new File(System.getProperty("user.home"));
		File appHomeFolder = new File(userHome, ".sonivm");
		if (!appHomeFolder.exists()) {
			LOGGER.info("Creating Sonivm data folder at " + appHomeFolder.getAbsolutePath());
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

	public void run() {
		sonivmUI.show();
	}
}
