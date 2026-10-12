package com.docs_similarity.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import com.docs_similarity.config.AppProperties;
import com.docs_similarity.exception.ApiException;
import com.docs_similarity.model.DocumentInput;

/**
 * Reads the .txt files of the project's {@code sample-documents} folder, so the dashboard can
 * load the labelled sample set with one click. The files are analysed exactly like uploads.
 */
@Service
public class SampleDocumentService {

    private final AppProperties properties;

    public SampleDocumentService(AppProperties properties) {
        this.properties = properties;
    }

    public List<DocumentInput> loadSamples() {
        Path folder = findSamplesFolder();
        try (Stream<Path> files = Files.list(folder)) {
            List<Path> textFiles = files
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".txt"))
                    .sorted()
                    .toList();
            List<DocumentInput> samples = new ArrayList<>();
            for (Path file : textFiles) {
                samples.add(new DocumentInput(file.getFileName().toString(),
                        Files.readString(file, StandardCharsets.UTF_8)));
            }
            if (samples.isEmpty()) {
                throw ApiException.notFound("The sample folder " + folder + " contains no .txt files.");
            }
            return samples;
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read sample documents from " + folder, e);
        }
    }

    /**
     * The configured folder is tried first. Because the backend can be started from the
     * backend folder (Maven) or from the project root (an IDE), both relative locations are tried.
     */
    private Path findSamplesFolder() {
        Set<Path> candidates = new LinkedHashSet<>();
        candidates.add(Path.of(properties.samplesDir()));
        candidates.add(Path.of("sample-documents"));
        candidates.add(Path.of("..", "sample-documents"));
        for (Path candidate : candidates) {
            if (Files.isDirectory(candidate)) {
                return candidate.toAbsolutePath().normalize();
            }
        }
        throw ApiException.notFound("Sample documents folder not found. Looked in: " + candidates
                + " (relative to " + Path.of("").toAbsolutePath() + ").");
    }
}
