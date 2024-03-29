package io.github.lc.oss.commons.web.services;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.http.io.SocketConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

public class HttpService {
    private static final StringHttpMessageConverter UTF_8_CONVERTER = new StringHttpMessageConverter(StandardCharsets.UTF_8);

    protected static final int DEFAULT_TIMEOUT = 30 * 1000;

    @Autowired(required = false)
    private CustomResponseErrorHandler customResponseErrorHandler;

    public <T> ResponseEntity<T> call(HttpMethod method, String url, Map<String, String> headers, Class<T> responseType, Object body) {
        HttpHeaders requestHeaders = new HttpHeaders();
        if (headers != null) {
            headers.forEach((k, v) -> requestHeaders.add(k, v));
        }

        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException ex) {
            throw new RuntimeException("Invalid URL", ex);
        }

        return this.createRestTemplate().exchange(uri, method, new HttpEntity<>(body, requestHeaders), responseType);
    }

    protected ClientHttpRequestFactory createRequestFactory() {
        /*
         * Long standing Java bug - PATCH isn't supported by default :(
         *
         * Note: HttpClient is not reusable :(
         */
        SocketConfig socketConfig = SocketConfig.custom(). //
                setSoTimeout(this.getTimeout(), TimeUnit.MILLISECONDS). //
                build();

        PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager();
        connManager.setDefaultSocketConfig(socketConfig);

        HttpClient client = HttpClientBuilder.create(). //
                disableRedirectHandling(). //
                setConnectionManager(connManager). //
                build();
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(client);
        factory.setConnectTimeout(this.getTimeout());
        return factory;
    }

    protected RestTemplate createRestTemplate() {
        RestTemplate rest = new RestTemplate();
        ResponseErrorHandler errorHandler = this.getCustomResponseErrorHandler();
        if (errorHandler != null) {
            rest.setErrorHandler(errorHandler);
        }
        ClientHttpRequestFactory factory = this.createRequestFactory();
        if (factory != null) {
            rest.setRequestFactory(factory);
        }

        /*
         * Spring 5.2+ "bug" - encoding headers are no longer supplied so JSON strings
         * get ISO_8859_1 instead of UTF-8 - which is a bug since JSON is UTF-8...
         */
        rest.getMessageConverters().add(0, HttpService.UTF_8_CONVERTER);
        return rest;
    }

    public int getTimeout() {
        return HttpService.DEFAULT_TIMEOUT;
    }

    protected ResponseErrorHandler getCustomResponseErrorHandler() {
        return this.customResponseErrorHandler;
    }
}
