package com.portfolio.newkeyword;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class NewKeywordApplication {

    public static void main(String[] args) {
        SpringApplication.run(NewKeywordApplication.class, args);
    }

}
