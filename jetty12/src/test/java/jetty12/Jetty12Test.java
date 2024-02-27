package jetty12;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class Jetty12Test
{
    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    public enum UriConstruction
    {
        SPRING,
        SPRING_NOENCODE,
        URI_DISTINCT_PARAMS,
        URI_SSP_PARAMS,
        URI_SINGLE_PARAM
    }

    private URI newURI(UriConstruction uriConstruction) throws URISyntaxException
    {
        return switch (uriConstruction)
        {
            case SPRING ->
                // URI using Spring techniques - This doesn't work, as this encodes the URI again.
                UriComponentsBuilder.fromHttpUrl("http://localhost:" + port + "/foo=bar%2Fbaz=baz")
                    .encode()
                    .build()
                    .toUri();
            case SPRING_NOENCODE ->
                // URI using Spring techniques, skipping .encode() - This doesn't work, as this STILL encodes the `%` in the URI again.
                UriComponentsBuilder.fromHttpUrl("http://localhost:" + port + "/foo=bar%2Fbaz=baz")
                    .build()
                    .toUri();
            case URI_DISTINCT_PARAMS ->
                // URI in distinct parts - This doesn't work, as the path param is encoded going into URI
                new URI("http", null, "localhost", port, "/foo=bar%2Fbaz=baz", null, null);
            case URI_SSP_PARAMS ->
                // URI using scheme specific part - This doesn't work, as the ssp is encoded going into the URI.
                new URI("http", "//localhost:" + port + "/foo=bar%2Fbaz=baz", null);
            case URI_SINGLE_PARAM ->
                // This works, as you are providing the ENTIRE URI in one string.
                new URI("http://localhost:" + port + "/foo=bar%2Fbaz=baz");
        };
    }

    @ParameterizedTest
    @EnumSource(UriConstruction.class)
    public void testFooBarBazEndpointHttpClient(UriConstruction uriConstruction) throws IOException, InterruptedException, URISyntaxException
    {
        URI uri = newURI(uriConstruction);
        assertThat("Oops, your URI technique is encoding the percent sign.", uri.toASCIIString(), not(containsString("%25")));

        HttpClient httpClient = HttpClient.newBuilder().build();
        HttpRequest httpRequest = HttpRequest.newBuilder()
            .uri(uri)
            .POST(HttpRequest.BodyPublishers.noBody())
            .build();
        HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertThat(httpResponse.body(), not(containsString("Bad Request")));
        assertThat(httpResponse.body(), containsString("%2F"));
    }

    @ParameterizedTest
    @EnumSource(UriConstruction.class)
    public void testFooBarBazEndpointJersey(UriConstruction uriConstruction) throws URISyntaxException
    {
        URI uri = newURI(uriConstruction);
        assertThat("Oops, your URI technique is encoding the percent sign.", uri.toASCIIString(), not(containsString("%25")));

        ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);
        assertThat(response.toString(), not(containsString("Bad Request")));
        assertThat(response.toString(), containsString("%2F"));
    }
}