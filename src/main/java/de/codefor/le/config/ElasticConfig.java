package de.codefor.le.config;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;

import com.google.common.base.Strings;

@Configuration
public class ElasticConfig {
	private static final Logger logger = LoggerFactory.getLogger(ElasticConfig.class);

	@Bean
	public ElasticsearchOperations elasticsearchTemplate() {
		return new ElasticsearchTemplate(client());
	}

	@Bean
	public Client client() {
		Settings settings = Settings.builder().put("cluster.name", "policeticker")
				//.put("network.bind_host", "0")
				//.put("client.transport.ignore_cluster_name", "true")
				//.put("client.transport.nodes_sampler_interval", "5s")
				//.put("client.transport.ping_timeout", "5s")
				//.put("client.transport.sniff", "false")
				//.put("network.host", "localhost")
				.build();
		TransportClient client = new PreBuiltTransportClient(settings);
		String elastURL = System.getenv("ELASTICSEARCH_URL");
		String host = "127.0.0.1";
		int port = 9300;
		if (!Strings.isNullOrEmpty(elastURL)) {
			String[] parts = elastURL.split(":");
			if (parts.length == 2) {
				host = parts[0];
				try {
					port = Integer.valueOf(parts[1]);
				} catch (NumberFormatException e) {
					logger.error(e.getMessage(), e);
				}
			}
		}
		try {
			logger.info("Using {}:{} to connect to Elasticsearch", host, port);
			TransportAddress address = new InetSocketTransportAddress(InetAddress.getByName(host), port);
			client.addTransportAddress(address);
		} catch (UnknownHostException e) {
			logger.error(e.getMessage(), e);
		}
		return client;
	}
}
