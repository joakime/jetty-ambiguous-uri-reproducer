package jetty12;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class Jetty12Test {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    public void testFooBarBazEndpoint() {
        URI url = UriComponentsBuilder.fromHttpUrl("http://localhost:" + port + "/foo=bar%2Fbaz=baz")
                .encode()
                .build()
                .toUri();

        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

        //FIXME: This should be false
        assertTrue(response.toString().contains("Bad Request"));

        //FIXME: Line below should pass as before
//        assertTrue(response.toString().contains("%2F"));
    }

}