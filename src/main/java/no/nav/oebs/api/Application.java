package no.nav.oebs.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {

	private static final Logger logger = LoggerFactory.getLogger(Application.class);

	public static void main(String[] args) {
		runApplication(args, logger);
	}

	static void runApplication(String[] args, Logger appLogger) {
		boolean infoEnabled = appLogger.isInfoEnabled();
		long startMs = 0L;
		if (infoEnabled) {
			startMs = System.currentTimeMillis();
			String javaVersion = System.getProperty("java.version");
			String jvmName = System.getProperty("java.vm.name");
			String osName = System.getProperty("os.name");
			String osVersion = System.getProperty("os.version");
			long maxHeapMb = Runtime.getRuntime().maxMemory() / 1024 / 1024;
			appLogger.info("━━━ JVM ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
			appLogger.info("  Java versjon : {}", javaVersion);
			appLogger.info("  JVM          : {}", jvmName);
			appLogger.info("  OS           : {} {}", osName, osVersion);
			appLogger.info("  Max heap     : {} MB", maxHeapMb);
			appLogger.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
			appLogger.info("Starter oebs-omada-api...");
		}
		SpringApplication.run(Application.class, args);
		if (infoEnabled) {
			long elapsedMs = System.currentTimeMillis() - startMs;
			appLogger.info("SpringApplication.run() fullført på {}ms", elapsedMs);
		}
	}
}