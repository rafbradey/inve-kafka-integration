package com.gabriel.integration.inve.service;

import com.gabriel.integration.inve.model.Inventory;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
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
public class InventoryService {

	@Setter @Getter
	String port;

	@Getter
	static URL inventoryURL;

	@Getter
	static String inventoryPort;


	@Getter
	protected static InventoryService service= null;

    public InventoryService() throws MalformedURLException {
    }

    public static InventoryService getService(String port) throws MalformedURLException {
		if(service == null){
			service = new InventoryService();
			service.port = port;
			inventoryURL = new URL("http://localhost:" + port + "/api/inventory");
			inventoryPort = port;
		}
		return service;
	}

	RestTemplate restTemplate = null;
	public RestTemplate getRestTemplate() {
		if(restTemplate == null) {
		restTemplate = new RestTemplate();
			List<HttpMessageConverter<?>> messageConverters = new ArrayList<>();
			MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
			converter.setSupportedMediaTypes(Collections.singletonList(MediaType.APPLICATION_JSON));
			messageConverters.add(converter);
			restTemplate.setMessageConverters(messageConverters);
		}
		return restTemplate;
	}

	public Inventory get(Integer id) {
		String url =  "http://localhost:" + port + "/api/inventory" + "/" + Integer.toString(id);
		log.info("get: "  + url);
		HttpHeaders headers = new HttpHeaders();
		HttpEntity request = new HttpEntity<>(null, headers);
		final ResponseEntity<Inventory> response =
		getRestTemplate().exchange(url, HttpMethod.GET, request, Inventory.class);
		return response.getBody();
	}

	public Inventory[] getAll() {
		String url =  "http://localhost:" + port + "/api/inventory";
		log.info("getInventorys: " + url);
		HttpHeaders headers = new HttpHeaders();
		HttpEntity request = new HttpEntity<>(null, headers);
		final ResponseEntity<Inventory[]> response =
		getRestTemplate().exchange(url, HttpMethod.GET, request, Inventory[].class);
		Inventory[] inventorys = response.getBody();
		return inventorys;
	}

	public Inventory create(Inventory inventory) {
		String url =  "http://localhost:" + port + "/api/inventory";
		HttpHeaders headers = new HttpHeaders();
		HttpEntity<Inventory> request = new HttpEntity<>(inventory, headers);
		final ResponseEntity<Inventory> response =
		getRestTemplate().exchange(url, HttpMethod.PUT, request, Inventory.class);
		return response.getBody();
	}
	public Inventory update(Inventory inventory) {
		log.info("update: " + inventory.toString());
		String url =  "http://localhost:" + port + "/api/inventory";
		HttpHeaders headers = new HttpHeaders();
		HttpEntity<Inventory> request = new HttpEntity<>(inventory, headers);
		final ResponseEntity<Inventory> response =
		getRestTemplate().exchange(url, HttpMethod.POST, request, Inventory.class);
		return response.getBody();
	}

	public void delete(Integer id){
		log.info("delete: " + Integer.toString(id));
		String url =  "http://localhost:" + port + "/api/inventory" + " / " + Integer.toString(id);
		HttpHeaders headers = new HttpHeaders();
		HttpEntity<Inventory> request = new HttpEntity<>(null, headers);
		final ResponseEntity<Inventory> response =
		getRestTemplate().exchange(url, HttpMethod.DELETE, request, Inventory.class);
	}
}
