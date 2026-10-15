package com.securetravels.crm.marketing;

import com.securetravels.crm.common.config.AppProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class MetaClientConfig {

    @Bean
    public RestClient metaRestClient(RestClient.Builder builder, AppProperties props) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(props.getMeta().getConnectTimeout());
        factory.setReadTimeout(props.getMeta().getReadTimeout());
        return builder.requestFactory(factory).build();
    }
}
