package com.eldie.signal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;


@SpringBootApplication
public class SignalingApplication {

	@Bean
	RouterFunction<ServerResponse> helloRouterFunction() {
		return RouterFunctions.route(RequestPredicates.path("/"),(sr) ->{

			return ServerResponse.ok().build();
		});
	}



	public static void main(String[] args) {
		SpringApplication.run(SignalingApplication.class, args);
	}

}
