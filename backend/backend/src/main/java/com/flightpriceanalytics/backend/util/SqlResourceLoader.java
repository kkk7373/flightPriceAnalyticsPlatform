package com.flightpriceanalytics.backend.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component 
public class SqlResourceLoader {

    public String loadSql(String fileName) throws IOException {
        return new ClassPathResource("sql/" + fileName)
            .getContentAsString(StandardCharsets.UTF_8);
    }

}
