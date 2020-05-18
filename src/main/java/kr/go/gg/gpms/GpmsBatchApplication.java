package kr.go.gg.gpms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class GpmsBatchApplication {

	public static void main(String[] args) {
		SpringApplication.run(GpmsBatchApplication.class, args);
	}

}
