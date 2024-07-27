package com.gabriel.integration.inve.service;

import com.gabriel.integration.inve.kafka.KafkaProducer;
import com.gabriel.integration.inve.model.Supplier;
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
public class SupplierService {

	@Setter
	String port;

	@Getter
    static URL supplierURL;

	@Getter
	protected static SupplierService service = null;

    public SupplierService(){
    }


    public static SupplierService getService(String port) throws MalformedURLException {
		if (service == null) {
			service = new SupplierService();
			service.port = port;
			supplierURL = new URL("http://localhost:" + port + "/api/supplier");

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

	public Supplier get(Integer id) {
		String url = "http://localhost:" + port + "/api/supplier/" + Integer.toString(id);
		log.info("get: " + url);
		HttpHeaders headers = new HttpHeaders();
		HttpEntity<?> request = new HttpEntity<>(null, headers);
		final ResponseEntity<Supplier> response =
				getRestTemplate().exchange(url, HttpMethod.GET, request, Supplier.class);
		return response.getBody();
	}

	public Supplier[] getAll() {
		String url = "http://localhost:" + port + "/api/supplier";
		log.info("getSuppliers: " + url);
		HttpHeaders headers = new HttpHeaders();
		HttpEntity<?> request = new HttpEntity<>(null, headers);
		final ResponseEntity<Supplier[]> response =
				getRestTemplate().exchange(url, HttpMethod.GET, request, Supplier[].class);
		return response.getBody();
	}

	public Supplier create(Supplier supplier) {
		String url = "http://localhost:" + port + "/api/supplier";
		HttpHeaders headers = new HttpHeaders();
		HttpEntity<Supplier> request = new HttpEntity<>(supplier, headers);
		final ResponseEntity<Supplier> response =
				getRestTemplate().exchange(url, HttpMethod.PUT, request, Supplier.class);

		// Send a notification to Kafka when a supplier is created
		kafkaProducer.sendNotification("Supplier created: " + supplier + "ID: " +
				supplier.getId() + " Name: " + supplier.getName() +
				" Last Updated: " + supplier.getLastUpdated() + " Created: "
				+ supplier.getCreated());

		return response.getBody();
	}

	public Supplier update(Supplier supplier) {
		log.info("update: " + supplier.toString());
		String url = "http://localhost:" + port + "/api/supplier";
		HttpHeaders headers = new HttpHeaders();
		HttpEntity<Supplier> request = new HttpEntity<>(supplier, headers);
		final ResponseEntity<Supplier> response =
				getRestTemplate().exchange(url, HttpMethod.POST, request, Supplier.class);
		return response.getBody();
	}

	public void delete(Integer id) {
		log.info("delete: " + Integer.toString(id));
		String url = "http://localhost:" + port + "/api/supplier/" + Integer.toString(id);
		HttpHeaders headers = new HttpHeaders();
		HttpEntity<?> request = new HttpEntity<>(null, headers);
		final ResponseEntity<Supplier> response =
				getRestTemplate().exchange(url, HttpMethod.DELETE, request, Supplier.class);
	}
}
