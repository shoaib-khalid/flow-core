package com.kalsym.flowcore.configs;

import com.kalsym.flowcore.VersionHolder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
 *
 * @author Sarosh
 */
@Configuration
@EnableSwagger2
public class SwaggerConfig {

    @Bean
    public Docket productApi() {
        return new Docket(DocumentationType.SWAGGER_2)
                .select()
                //.paths(PathSelectors.any())
                .apis(RequestHandlerSelectors.basePackage("com.kalsym.flowcore"))
                .build()
                .apiInfo(apiInfo());
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder().title("KBOT flow-core.")
                .description("Manages conversations. Please read provided docs for detailed description.\n"
                        + "https://www.dropbox.com/s/qukb6gmmf1dg0wj/KBot%20Technical%20Specification%20Document.docx?dl=0")
                .termsOfServiceUrl("http://www.apache.org/licenses/LICENSE-2.0")
                .license("Apache 2.0")
                .licenseUrl("http://www.apache.org/licenses/LICENSE-2.0").version(VersionHolder.VERSION).build();
    }
}