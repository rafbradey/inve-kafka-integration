package com.gabriel.integration.inve.service;

import com.gabriel.integration.inve.kafka.KafkaProducer;
import com.gabriel.integration.inve.model.Category;
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
public class CategoryService {

	@Setter
	String port;

	@Getter
    static URL categoryURL;

	@Getter
	protected static CategoryService service = null;

    public CategoryService(){
    }


    public static CategoryService getService(String port) throws MalformedURLException {
		if (service == null) {
			service = new CategoryService();
			service.port = port;
			categoryURL = new URL("http://localhost:" + port + "/api/category");

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

	public Category get(Integer id) {
		String url = "http://localhost:" + port + "/api/category/" + Integer.toString(id);
		log.info("get: " + url);
		HttpHeaders headers = new HttpHeaders();
		HttpEntity<?> request = new HttpEntity<>(null, headers);
		final ResponseEntity<Category> response =
				getRestTemplate().exchange(url, HttpMethod.GET, request, Category.class);
		return response.getBody();
	}

	public Category[] getAll() {
		String url = "http://localhost:" + port + "/api/category";
		log.info("getCategorys: " + url);
		HttpHeaders headers = new HttpHeaders();
		HttpEntity<?> request = new HttpEntity<>(null, headers);
		final ResponseEntity<Category[]> response =
				getRestTemplate().exchange(url, HttpMethod.GET, request, Category[].class);
		return response.getBody();
	}

	public Category create(Category category) {
		String url = "http://localhost:" + port + "/api/category";
		HttpHeaders headers = new HttpHeaders();
		HttpEntity<Category> request = new HttpEntity<>(category, headers);
		final ResponseEntity<Category> response =
				getRestTemplate().exchange(url, HttpMethod.PUT, request, Category.class);

		// Send a notification to Kafka when a category is created
		kafkaProducer.sendNotification("Category created: " + category + "ID: " +
				category.getId() + " Name: " + category.getName() +
				" Last Updated: " + category.getLastUpdated() + " Created: "
				+ category.getCreated());

		return response.getBody();
	}

	public Category update(Category category) {
		log.info("update: " + category.toString());
		String url = "http://localhost:" + port + "/api/category";
		HttpHeaders headers = new HttpHeaders();
		HttpEntity<Category> request = new HttpEntity<>(category, headers);
		final ResponseEntity<Category> response =
				getRestTemplate().exchange(url, HttpMethod.POST, request, Category.class);
		return response.getBody();
	}

	public void delete(Integer id) {
		log.info("delete: " + Integer.toString(id));
		String url = "http://localhost:" + port + "/api/category/" + Integer.toString(id);
		HttpHeaders headers = new HttpHeaders();
		HttpEntity<?> request = new HttpEntity<>(null, headers);
		final ResponseEntity<Category> response =
				getRestTemplate().exchange(url, HttpMethod.DELETE, request, Category.class);
	}
}
