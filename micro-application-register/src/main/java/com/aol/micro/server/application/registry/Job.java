package com.aol.micro.server.application.registry;

import java.util.Date;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.aol.micro.server.rest.client.nio.AsyncRestClient;
import com.aol.micro.server.rest.jackson.JacksonUtil;

@Component
public class Job {

	private final Logger logger = LoggerFactory.getLogger(getClass());
	private final AsyncRestClient rest = new AsyncRestClient(100,2000);
	private final String apiUrl;
	private final ApplicationRegisterImpl app;
	private final String uuid = UUID.randomUUID().toString();

	@Autowired
	public Job(@Value("${service.registry.url:null}") String apiUrl, ApplicationRegisterImpl app) {

		this.apiUrl = apiUrl;
		this.app = app;
		
	}

	@Scheduled(fixedDelayString = "${service.registry.delay:300000}")
	public synchronized void schedule() {
		if(app.getApplication()!=null && apiUrl!=null)
			app.getApplication().forEach(moduleEntry -> sendPing(moduleEntry));
	}

	private void sendPing(RegisterEntry moduleEntry) {
		final RegisterEntry entry = moduleEntry.withTime(new Date())
												.withUuid(uuid);
		try {

			logger.info("Posting {} to " + apiUrl + "/service-registry/register", JacksonUtil.serializeToJson(entry));
			rest.post(apiUrl + "/service-registry/register", JacksonUtil.serializeToJson(entry)).join();
		} catch (Exception e) {
			logger.warn("Failed posting {} to {}/service-registry/register", JacksonUtil.serializeToJson(entry), apiUrl);

		}
	}
}
