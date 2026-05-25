package com.topik.topikai.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.topik.topikai.dto.HanjaBankDto;
import com.topik.topikai.service.HanjaCharacterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;

@Component
public class HanjaDataInitializer implements ApplicationRunner {

    @Autowired
    private HanjaCharacterService hanjaCharacterService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void run(ApplicationArguments args) throws Exception {
        ClassPathResource resource = new ClassPathResource("hanja-bank.json");
        if (!resource.exists()) {
            return;
        }

        try (InputStream in = resource.getInputStream()) {
            HanjaBankDto bank = objectMapper.readValue(in, HanjaBankDto.class);
            if (bank.getCharacters() == null || bank.getCharacters().isEmpty()) {
                return;
            }
            // Always sync from JSON on startup (upsert by externalId)
            hanjaCharacterService.upsertAll(bank.getCharacters());
        }
    }
}
