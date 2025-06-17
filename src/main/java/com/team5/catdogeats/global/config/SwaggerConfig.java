package com.team5.catdogeats.global.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        String csrf = "X-XSRF-TOKEN";

        return new OpenAPI()
                .info(new Info()
                        .title("CatDogeats API")
                        .version("v1")
                        .description("API 문서"))
                .addSecurityItem(new SecurityRequirement()
                        .addList(csrf))
                .components(new Components()
                        .addSecuritySchemes(csrf,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.HEADER)
                                        .name(csrf)
                        )
                );
    }


}
