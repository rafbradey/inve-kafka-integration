package com.gabriel.integration.inve.service;

import com.gabriel.integration.inve.kafka.KafkaProducer;
import com.gabriel.integration.inve.model.Storage;
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
public class StorageService {

	@Setter
	String port;

	@Getter
    static URL storageURL;

	@Getter
	protected static StorageService service = null;

    public StorageService(){
    }


    public static StorageService getService(String port) throws MalformedURLException {
		if (service == null) {
			service = new StorageService();
			service.port = port;
			storageURL = new URL("http://localhost:" + port + "/api/storage");

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

	public Storage get(Integer id) {
		String url = "http://localhost:" + port + "/api/storage/" + Integer.toString(id);
		log.info("get: " + url);
		HttpHeaders headers = new HttpHeaders();
		HttpEntity<?> request = new HttpEntity<>(null, headers);
		final ResponseEntity<Storage> response =
				getRestTemplate().exchange(url, HttpMethod.GET, request, Storage.class);
		return response.getBody();
	}

	public Storage[] getAll() {
		String url = "http://localhost:" + port + "/api/storage";
		log.info("getStorages: " + url);
		HttpHeaders headers = new HttpHeaders();
		HttpEntity<?> request = new HttpEntity<>(null, headers);
		final ResponseEntity<Storage[]> response =
				getRestTemplate().exchange(url, HttpMethod.GET, request, Storage[].class);
		return response.getBody();
	}

	public Storage create(Storage storage) {
		String url = "http://localhost:" + port + "/api/storage";
		HttpHeaders headers = new HttpHeaders();
		HttpEntity<Storage> request = new HttpEntity<>(storage, headers);
		final ResponseEntity<Storage> response =
				getRestTemplate().exchange(url, HttpMethod.PUT, request, Storage.class);

		// Send a notification to Kafka when a storage is created
		kafkaProducer.sendNotification("Storage created: " + storage + "ID: " +
				storage.getId() + " Name: " + storage.getName() +
				" Last Updated: " + storage.getLastUpdated() + " Created: "
				+ storage.getCreated());

		return response.getBody();
	}

	public Storage update(Storage storage) {
		log.info("update: " + storage.toString());
		String url = "http://localhost:" + port + "/api/storage";
		HttpHeaders headers = new HttpHeaders();
		HttpEntity<Storage> request = new HttpEntity<>(storage, headers);
		final ResponseEntity<Storage> response =
				getRestTemplate().exchange(url, HttpMethod.POST, request, Storage.class);
		return response.getBody();
	}

	public void delete(Integer id) {
		log.info("delete: " + Integer.toString(id));
		String url = "http://localhost:" + port + "/api/storage/" + Integer.toString(id);
		HttpHeaders headers = new HttpHeaders();
		HttpEntity<?> request = new HttpEntity<>(null, headers);
		final ResponseEntity<Storage> response =
				getRestTemplate().exchange(url, HttpMethod.DELETE, request, Storage.class);
	}
}
