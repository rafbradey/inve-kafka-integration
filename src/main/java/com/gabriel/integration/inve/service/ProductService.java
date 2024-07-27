package com.gabriel.integration.inve.service;

import com.gabriel.integration.inve.kafka.KafkaProducer;
import com.gabriel.integration.inve.model.Product;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class ProductService {

	@Setter
	String port;

	@Getter
	URL productURL = new URL("http://localhost:8080/api/product");


	@Getter
	protected static ProductService service = null;

    public ProductService() throws MalformedURLException {
    }

    public static ProductService getService(String port) throws MalformedURLException {
		if (service == null) {
			service = new ProductService();
			service.port = port;
		}
		return service;
	}

	@Autowired
	private KafkaProducer kafkaProducer; // Inject KafkaProducer

	RestTemplate restTemplate = null;

	public RestTemplate getRestTemplate() {
		if (restTemplate == null) {
			restTemplate = new RestTemplate();
			List<HttpMessageConverter<?>> messageConverters = new ArrayList<>();
			MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
			converter.setSupportedMediaTypes(Collections.singletonList(MediaType.APPLICATION_JSON));
			messageConverters.add(converter);
			restTemplate.setMessageConverters(messageConverters);
		}
		return restTemplate;
	}

	public Product get(Integer id) {
		String url = "http://localhost:" + port + "/api/product/" + Integer.toString(id);
		log.info("get: " + url);
		HttpHeaders headers = new HttpHeaders();
		HttpEntity<?> request = new HttpEntity<>(null, headers);
		final ResponseEntity<Product> response =
				getRestTemplate().exchange(url, HttpMethod.GET, request, Product.class);
		return response.getBody();
	}

	public Product[] getAll() {
		String url = "http://localhost:" + port + "/api/product";
		log.info("getProducts: " + url);
		HttpHeaders headers = new HttpHeaders();
		HttpEntity<?> request = new HttpEntity<>(null, headers);
		final ResponseEntity<Product[]> response =
				getRestTemplate().exchange(url, HttpMethod.GET, request, Product[].class);
		return response.getBody();
	}

	public Product create(Product product) {
		String url = "http://localhost:" + port + "/api/product";
		HttpHeaders headers = new HttpHeaders();
		HttpEntity<Product> request = new HttpEntity<>(product, headers);
		final ResponseEntity<Product> response =
				getRestTemplate().exchange(url, HttpMethod.PUT, request, Product.class);

		// Send a notification to Kafka when a product is created
		kafkaProducer.sendNotification("Product created: " + product + "ID: " +
				product.getId() + " Name: " + product.getName() +
				" Last Updated: " + product.getLastUpdated() + " Created: "
				+ product.getCreated());

		return response.getBody();
	}

	public Product update(Product product) {
		log.info("update: " + product.toString());
		String url = "http://localhost:" + port + "/api/product";
		HttpHeaders headers = new HttpHeaders();
		HttpEntity<Product> request = new HttpEntity<>(product, headers);
		final ResponseEntity<Product> response =
				getRestTemplate().exchange(url, HttpMethod.POST, request, Product.class);
		return response.getBody();
	}

	public void delete(Integer id) {
		log.info("delete: " + Integer.toString(id));
		String url = "http://localhost:" + port + "/api/product/" + Integer.toString(id);
		HttpHeaders headers = new HttpHeaders();
		HttpEntity<?> request = new HttpEntity<>(null, headers);
		final ResponseEntity<Product> response =
				getRestTemplate().exchange(url, HttpMethod.DELETE, request, Product.class);
	}
}
