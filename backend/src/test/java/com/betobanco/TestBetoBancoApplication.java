package com.betobanco;

import org.springframework.boot.SpringApplication;

public class TestBetoBancoApplication {

	public static void main(String[] args) {
		SpringApplication.from(BetoBancoApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
