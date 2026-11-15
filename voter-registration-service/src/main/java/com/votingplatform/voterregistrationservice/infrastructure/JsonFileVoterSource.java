package com.votingplatform.voterregistrationservice.infrastructure;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.votingplatform.voterregistrationservice.application.port.RawVoter;
import com.votingplatform.voterregistrationservice.application.port.VoterSource;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public final class JsonFileVoterSource implements VoterSource {
    private final ObjectMapper objectMapper;
    private final String location;

    public JsonFileVoterSource(ObjectMapper objectMapper,
                               @Value("${voters.file:classpath:voters.json}") String location) {
        this.objectMapper = objectMapper;
        this.location = location;
    }

    @Override
    public List<RawVoter> readAll() {
        try (InputStream input = openStream()) {
            return objectMapper.readValue(input, new TypeReference<>() { });
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read voters file: " + location, exception);
        }
    }

    private InputStream openStream() throws IOException {
        if (location.startsWith("classpath:")) {
            return new ClassPathResource(location.substring("classpath:".length())).getInputStream();
        }
        return Files.newInputStream(Path.of(location));
    }
}
