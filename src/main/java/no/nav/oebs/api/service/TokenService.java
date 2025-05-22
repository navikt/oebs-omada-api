package no.nav.oebs.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.oebs.api.Application;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;


@Service
public class TokenService {

    public static String STATUS = "";
    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    @Value("${MM_USER}")
    private String templateUserName;

    @Value("${MM_PASSWORD}")
    private String templatePassword;

    @Value("${MM_GRANT_TYPE}")
    private String templateGrantType;

    @Value("${MM_URL_TOKEN}")
    private String templateUrlToken;

    public String genererToken() throws Exception {

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        // headers.set("User-Agent", "Oebs-MainManger-api/0.0.4");

        MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
        requestBody.add("username", templateUserName);
        requestBody.add("password", templatePassword);
        requestBody.add("grant_type", templateGrantType);

        String url = templateUrlToken;

        ResponseEntity<String> responseEntity = restTemplate.postForEntity(url, requestBody, String.class);

        if (responseEntity.getStatusCode().equals(HttpStatus.OK)) {
            String jsonText = responseEntity.getBody();
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(jsonText);
            String accessToken = jsonNode.get("access_token").asText();

            STATUS = "OK";
            return accessToken;
        }
        else {
            return responseEntity.getBody();
        }
    }
}

