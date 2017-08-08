package com.example.demo;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

@RestController
public class FileDownloadController {
	static final Logger log = LoggerFactory.getLogger(FileDownloadController.class);
	private final DownloadHandler downloadHandler;

	public FileDownloadController(DownloadHandler downloadHandler) {
		this.downloadHandler = downloadHandler;
	}

	@GetMapping(path = "/")
	public ResponseEntity<ResponseBodyEmitter> download() throws Exception {
		ResponseBodyEmitter bodyEmitter = new ResponseBodyEmitter(
				TimeUnit.MINUTES.toMillis(1) /* timeout */);
		log.info("Start emitting...");
		downloadHandler.download(bodyEmitter);
		log.info("End emitting...");
		return ResponseEntity //
				.status(HttpStatus.OK) //
				.contentType(MediaType.APPLICATION_OCTET_STREAM) //
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=abc.csv") //
				.body(bodyEmitter);
	}

	@Component
	@Transactional(readOnly = true)
	static class DownloadHandler {
		static final Logger log = LoggerFactory.getLogger(DownloadHandler.class);
		final JdbcTemplate jdbcTemplate;

		public DownloadHandler(JdbcTemplate jdbcTemplate) {
			this.jdbcTemplate = jdbcTemplate;
		}

		@Async
		public void download(ResponseBodyEmitter emitter) {
			log.info("Start downloading...");
			this.jdbcTemplate.query("SELECT id FROM demo", rs -> {
				Integer id = rs.getInt("id");
				log.trace("id,{}", id);
				try {
					emitter.send("id," + id + "\n");
				}
				catch (IOException e) {
					log.info("IO Exception!");
					emitter.completeWithError(e);
				}
			});
			log.info("End downloading...");
			emitter.complete();
		}
	}
}
