package no.nav.oebs.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableCaching
@EnableScheduling
public class Application {

	private static final Logger logger = LoggerFactory.getLogger(Application.class);

	public static void main(String[] args) {
		long startMs = System.currentTimeMillis();
		logger.info("━━━ JVM ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
		logger.info("  Java versjon : {}", System.getProperty("java.version"));
		logger.info("  JVM          : {}", System.getProperty("java.vm.name"));
		logger.info("  OS           : {} {}", System.getProperty("os.name"), System.getProperty("os.version"));
		logger.info("  Max heap     : {} MB", Runtime.getRuntime().maxMemory() / 1024 / 1024);
		logger.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
		logger.info("Starter oebs-omada-api...");
		SpringApplication.run(Application.class, args);
		logger.info("SpringApplication.run() fullført på {}ms", System.currentTimeMillis() - startMs);
	}
}