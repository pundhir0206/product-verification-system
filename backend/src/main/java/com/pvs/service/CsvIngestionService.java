package com.pvs.service;


import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import com.opencsv.CSVReader;
import com.pvs.entity.Product;
import com.pvs.entity.UploadJob;
import com.pvs.repository.ProductRepository;
import com.pvs.repository.UploadJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class CsvIngestionService {

	private static final List<DateTimeFormatter> DATE_FORMATS = List.of(DateTimeFormatter.ISO_LOCAL_DATE,
			DateTimeFormatter.ofPattern("dd-MM-yyyy"), DateTimeFormatter.ofPattern("dd/MM/yyyy"),
			DateTimeFormatter.ofPattern("MM/dd/yyyy"), DateTimeFormatter.ofPattern("yyyy/MM/dd"));

	private static final Set<String> REQUIRED_HEADERS = Set.of("wid", "ean", "manufacturing_date", "expiry_date");
	@PersistenceContext
	private EntityManager entityManager;

	private final UploadJobRepository uploadJobRepository;
	private final ProductRepository productRepository;

	@Value("${app.csv.batch-size:500}")
	private int batchSize;

	public UploadJob createJob(String fileName, String createdBy) {

		UploadJob job = UploadJob.builder().id(UUID.randomUUID()).fileName(fileName).status(UploadJob.JobStatus.PENDING)
				.createdBy(createdBy).build();

		return uploadJobRepository.save(job);
	}

	@Async("csvIngestionExecutor")
	public void processAsync(UUID jobId, Path csvFilePath) {

		UploadJob job = uploadJobRepository.findById(jobId).orElseThrow();

		job.setStatus(UploadJob.JobStatus.PROCESSING);
		uploadJobRepository.save(job);

		long totalRows = 0;
		long processedRows = 0;
		long insertedRows = 0;
		long updatedRows = 0;
		long failedRows = 0;

		try (CSVReader reader = new CSVReader(new FileReader(csvFilePath.toFile()))) {

			String[] header = reader.readNext();
			validateHeader(header);

			Map<String, Integer> columnIndex = indexOfHeaders(header);

			List<Product> batch = new ArrayList<>(batchSize);

			String[] line;
			long rowNumber = 0;

			while ((line = reader.readNext()) != null) {

				rowNumber++;
				totalRows++;

				try {

					String wid = safeGet(line, columnIndex.get("wid"));
					String ean = safeGet(line, columnIndex.get("ean"));

					LocalDate manufacturingDate = parseDate(safeGet(line, columnIndex.get("manufacturing_date")));

					LocalDate expiryDate = parseDate(safeGet(line, columnIndex.get("expiry_date")));

					if (wid == null || wid.isBlank()) {
						throw new IllegalArgumentException("Missing WID");
					}

					if (ean == null || ean.isBlank()) {
						throw new IllegalArgumentException("Missing EAN");
					}

					Product product = Product.builder().wid(wid.trim()).ean(ean.trim())
							.manufacturingDate(manufacturingDate).expiryDate(expiryDate).build();

					batch.add(product);

					if (batch.size() >= batchSize) {

						BatchResult result = flushBatch(batch);

						processedRows += batch.size();
						insertedRows += result.inserted();
						updatedRows += result.updated();

						job.setProcessedRows(processedRows);
						job.setInsertedRows(insertedRows);
						job.setUpdatedRows(updatedRows);
						job.setFailedRows(failedRows);

						uploadJobRepository.save(job);

						batch.clear();
					}

				} catch (Exception ex) {

					failedRows++;

					log.warn("Job {} : skipping row {} : {}", jobId, rowNumber, ex.getMessage());
				}
			}

			if (!batch.isEmpty()) {

				BatchResult result = flushBatch(batch);

				processedRows += batch.size();
				insertedRows += result.inserted();
				updatedRows += result.updated();

				job.setProcessedRows(processedRows);
				job.setInsertedRows(insertedRows);
				job.setUpdatedRows(updatedRows);
				job.setFailedRows(failedRows);
			}

			job.setTotalRows(totalRows);
			job.setStatus(UploadJob.JobStatus.COMPLETED);
			job.setUpdatedAt(LocalDateTime.now());

			uploadJobRepository.save(job);

			log.info("Upload Job {} completed. total={}, inserted={}, updated={}, failed={}", jobId, totalRows,
					insertedRows, updatedRows, failedRows);

		} catch (Exception ex) {

			log.error("CSV upload failed for job {}", jobId, ex);

			job.setStatus(UploadJob.JobStatus.FAILED);
			job.setErrorMessage(ex.getMessage());
			job.setTotalRows(totalRows);
			job.setProcessedRows(processedRows);
			job.setInsertedRows(insertedRows);
			job.setUpdatedRows(updatedRows);
			job.setFailedRows(failedRows);
			job.setUpdatedAt(LocalDateTime.now());

			uploadJobRepository.save(job);

		} finally {

			try {
				Files.deleteIfExists(csvFilePath);
			} catch (Exception ex) {
				log.warn("Unable to delete temp file {}", csvFilePath, ex);
			}
		}
	}



	@Transactional
	protected BatchResult flushBatch(List<Product> batch) {

		if (batch.isEmpty()) {
			return new BatchResult(0, 0);
		}

		List<String> wids = batch.stream().map(Product::getWid).toList();

		Map<String, Product> existingProducts = productRepository.findAllByWidIn(wids).stream()
				.collect(Collectors.toMap(Product::getWid, Function.identity()));

		List<Product> productsToSave = new ArrayList<>(batch.size());

		long inserted = 0;
		long updated = 0;

		LocalDateTime now = LocalDateTime.now();

		for (Product incoming : batch) {

			Product existing = existingProducts.get(incoming.getWid());

			if (existing == null) {

				incoming.setCreatedAt(now);
				incoming.setUpdatedAt(now);

				productsToSave.add(incoming);
				inserted++;

			} else {

				existing.setEan(incoming.getEan());
				existing.setManufacturingDate(incoming.getManufacturingDate());
				existing.setExpiryDate(incoming.getExpiryDate());
				existing.setUpdatedAt(now);

				productsToSave.add(existing);
				updated++;
			}
		}

		productRepository.saveAll(productsToSave);
		entityManager.flush();
		entityManager.clear();

		return new BatchResult(inserted, updated);
	}

	private record BatchResult(long inserted, long updated) {
	}

	private void validateHeader(String[] header) {

		if (header == null || header.length == 0) {
			throw new IllegalArgumentException("Empty CSV file");
		}

		Set<String> headers = new java.util.HashSet<>();

		for (String h : header) {
			headers.add(h.trim().toLowerCase());
		}

		if (!headers.containsAll(REQUIRED_HEADERS)) {
			throw new IllegalArgumentException("CSV must contain columns: WID, EAN, Manufacturing_Date, Expiry_Date");
		}
	}

	private Map<String, Integer> indexOfHeaders(String[] header) {

		Map<String, Integer> map = new HashMap<>();

		for (int i = 0; i < header.length; i++) {
			map.put(header[i].trim().toLowerCase(), i);
		}

		return map;
	}

	private String safeGet(String[] row, Integer index) {

		if (index == null || index >= row.length) {
			return null;
		}

		return row[index];
	}

	private LocalDate parseDate(String value) {

		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException("Missing date");
		}

		String trimmed = value.trim();

		for (DateTimeFormatter formatter : DATE_FORMATS) {

			try {
				return LocalDate.parse(trimmed, formatter);
			} catch (DateTimeParseException ignored) {
			}
		}

		throw new IllegalArgumentException("Invalid date format: " + value);
	}
}