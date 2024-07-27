package com.gabriel.integration.inve.service;

import com.gabriel.integration.inve.kafka.KafkaProducer;
import com.gabriel.integration.inve.model.Status;
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
public class StatusService {

	@Setter
	String port;

	@Getter
    static URL statusURL;
	
	@Getter
	protected static StatusService service = null;

    public StatusService(){
    }


    public static StatusService getService(String port) throws MalformedURLException {
		if (service == null) {
			service = new StatusService();
			service.port = port;
			statusURL = new URL("http://localhost:" + port + "/api/status");

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

	public Status get(Integer id) {
		String url = "http://localhost:" + port + "/api/status/" + Integer.toString(id);
		log.info("get: " + url);
		HttpHeaders headers = new HttpHeaders();
		HttpEntity<?> request = new HttpEntity<>(null, headers);
		final ResponseEntity<Status> response =
				getRestTemplate().exchange(url, HttpMethod.GET, request, Status.class);
		return response.getBody();
	}

	public Status[] getAll() {
		String url = "http://localhost:" + port + "/api/status";
		log.info("getStatuss: " + url);
		HttpHeaders headers = new HttpHeaders();
		HttpEntity<?> request = new HttpEntity<>(null, headers);
		final ResponseEntity<Status[]> response =
				getRestTemplate().exchange(url, HttpMethod.GET, request, Status[].class);
		return response.getBody();
	}

	public Status create(Status status) {
		String url = "http://localhost:" + port + "/api/status";
		HttpHeaders headers = new HttpHeaders();
		HttpEntity<Status> request = new HttpEntity<>(status, headers);
		final ResponseEntity<Status> response =
				getRestTemplate().exchange(url, HttpMethod.PUT, request, Status.class);

		// Send a notification to Kafka when a status is created
		kafkaProducer.sendNotification("Status created: " + status + "ID: " +
				status.getId() + " Name: " + status.getName() +
				" Last Updated: " + status.getLastUpdated() + " Created: "
				+ status.getCreated());

		return response.getBody();
	}

	public Status update(Status status) {
		log.info("update: " + status.toString());
		String url = "http://localhost:" + port + "/api/status";
		HttpHeaders headers = new HttpHeaders();
		HttpEntity<Status> request = new HttpEntity<>(status, headers);
		final ResponseEntity<Status> response =
				getRestTemplate().exchange(url, HttpMethod.POST, request, Status.class);
		return response.getBody();
	}

	public void delete(Integer id) {
		log.info("delete: " + Integer.toString(id));
		String url = "http://localhost:" + port + "/api/status/" + Integer.toString(id);
		HttpHeaders headers = new HttpHeaders();
		HttpEntity<?> request = new HttpEntity<>(null, headers);
		final ResponseEntity<Status> response =
				getRestTemplate().exchange(url, HttpMethod.DELETE, request, Status.class);
	}
}
