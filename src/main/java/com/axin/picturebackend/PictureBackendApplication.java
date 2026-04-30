package com.axin.picturebackend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.axin.picturebackend.mapper")
@EnableAspectJAutoProxy(exposeProxy = true)
@EnableScheduling
public class PictureBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(PictureBackendApplication.class, args);
	}

}
