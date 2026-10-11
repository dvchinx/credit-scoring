package com.florez.backend.config;

import java.net.http.HttpClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/** Cliente HTTP compartido hacia el servicio de ML (scoring y catálogo de modelos). */
@Configuration
@EnableConfigurationProperties(MlServiceProperties.class)
public class MlServiceClientConfig {

    @Bean
    RestClient mlServiceRestClient(MlServiceProperties properties) {
        // HTTP/1.1 explícito: por defecto el HttpClient del JDK intenta HTTP/2 con "Upgrade: h2c" sobre
        // http://, y uvicorn (servidor del servicio de ML) no soporta h2c y descarta el cuerpo del POST.
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(properties.connectTimeout())
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.readTimeout());

        return RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory)
                .build();
    }
}
