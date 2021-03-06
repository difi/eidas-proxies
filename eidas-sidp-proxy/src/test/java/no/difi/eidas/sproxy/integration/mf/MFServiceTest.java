package no.difi.eidas.sproxy.integration.mf;

import no.difi.eidas.idpproxy.integrasjon.dsf.restapi.PersonLookupResult;
import no.difi.eidas.sproxy.config.ConfigProvider;
import no.difi.eidas.sproxy.domain.attribute.AttributesConfigProvider;
import no.difi.eidas.sproxy.integration.eidas.response.EidasResponse;
import no.difi.eidas.sproxy.integration.fileconfig.attribute.Attribute;
import no.difi.eidas.sproxy.integration.fileconfig.attribute.Country;
import no.difi.eidas.sproxy.integration.fileconfig.attribute.PidType;
import no.difi.eidas.sproxy.web.AuthState;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class MFServiceTest {
    private static final String CLIENT_ID_HEADER = "Client-Id";
    private static final String TEST_PID = "05068907693";
    private static final String TEST_EIDAS_PERSON_IDENTIFIER = "CE/NO/05061989";
    private static final String TEST_SHORT_EIDAS_PERSON_IDENTIFIER = "05061989";
    private static final String TEST_EIDAS_PERSON_BIRTHDATE = "19550215";
    private static final String EIDAS_PERSON_IDENTIFIER_ATTRIBUTE_NAME = "PersonIdentifier";
    private static final String MF_URL = "http://localhost:8080/eidas/entydig";
    @Mock
    private ConfigProvider configProvider;

    @Mock
    private AttributesConfigProvider attributesConfigProvider;

    @Mock
    private AuthState authState;

    @Mock
    private RestTemplate restTemplate;

    private MFService mfService;

    @Before
    public void before() {
        reset(configProvider, attributesConfigProvider, authState);
        mfService = new MFService(attributesConfigProvider, () -> authState, configProvider, restTemplate);
        when(authState.getCountryCode()).thenReturn("CE");
        when(configProvider.getMfGatewayUrl()).thenReturn("http://localhost:8080");
        when(configProvider.getMfGatewayUsername()).thenReturn("user");
        when(configProvider.getMfGatewayPassword()).thenReturn("password");
    }

    @Test
    public void noCountryConfigWithNoMatchTest() {
        when(attributesConfigProvider.getCountry(any())).thenReturn(Optional.empty());
        Map<String, String> attributes = new HashMap<>();
        HttpEntity<String> entity = new HttpEntity<>(createHttpHeaders());
        String expectedUrl = MF_URL + "?foedselsdato=19550215&landkode=CEA&utenlandskPersonIdentifikasjon=test";
        when(restTemplate.exchange(expectedUrl, HttpMethod.GET, entity, String.class)).thenReturn(new ResponseEntity<>(null, HttpStatus.NOT_FOUND));

        attributes.put(EIDAS_PERSON_IDENTIFIER_ATTRIBUTE_NAME, "test");
        EidasResponse searchParameters = EidasResponse.builder().dateOfBirth("1955-02-15")
                .attributes(attributes)
                .build();
        PersonLookupResult lookup = mfService.lookup(searchParameters);
        verifyUnsuccessfulLookup(lookup);
    }

    @Test
    public void noCountryConfigWithMatchTest() {
        when(attributesConfigProvider.getCountry(any())).thenReturn(Optional.empty());
        String expectedUrl = MF_URL + "?foedselsdato=19550215&landkode=CEA&utenlandskPersonIdentifikasjon=05061989";
        HttpEntity<String> entity = new HttpEntity<>(createHttpHeaders());
        when(restTemplate.exchange(expectedUrl, HttpMethod.GET, entity, String.class)).thenReturn(new ResponseEntity<>(TEST_PID, HttpStatus.OK));
        Map<String, String> attributes = new HashMap<>();
        attributes.put("PersonIdentifier", TEST_EIDAS_PERSON_IDENTIFIER);
        EidasResponse searchParameters = EidasResponse.builder().dateOfBirth("1955-02-15")
                .attributes(attributes)
                .build();

        PersonLookupResult lookup = mfService.lookup(searchParameters);
        verifyRestTemplateArgument(expectedUrl);
        verifySuccessfulLookup(lookup);
    }

    @Test
    public void noCountryConfigWithMatchMFMockTest() {
        when(attributesConfigProvider.getCountry(any())).thenReturn(Optional.empty());
        String expectedUrl = MF_URL + "?foedselsdato=19550215&landkode=CEA&utenlandskPersonIdentifikasjon=05061989";

        when(configProvider.eIDASIdentifierDnumbers()).thenReturn(new HashMap<String, String>() {{
            this.put(TEST_SHORT_EIDAS_PERSON_IDENTIFIER, TEST_PID);
        }});
        Map<String, String> attributes = new HashMap<>();
        attributes.put("PersonIdentifier", TEST_EIDAS_PERSON_IDENTIFIER);
        EidasResponse searchParameters = EidasResponse.builder().dateOfBirth("1955-02-15")
                .attributes(attributes)
                .build();

        PersonLookupResult lookup = mfService.lookup(searchParameters);
        verifyRestTemplateArgument(expectedUrl);
        verifySuccessfulLookup(lookup);
    }

    @Test
    public void countryConfigWithNoSpecialExtractionRulesNoMatchTest() {
        String expectedUrl = MF_URL + "?foedselsdato=19550215&landkode=CEA&utenlandskPersonIdentifikasjon=test";
        HttpEntity<String> entity = new HttpEntity<>(createHttpHeaders());
        when(restTemplate.exchange(expectedUrl, HttpMethod.GET, entity, String.class)).thenReturn(new ResponseEntity<>(null, HttpStatus.NOT_FOUND));
        Map<String, String> attributes = new HashMap<>();
        attributes.put("PersonIdentifier", "test");
        EidasResponse searchParameters = EidasResponse.builder().dateOfBirth("1955-02-15")
                .attributes(attributes)
                .build();

        PersonLookupResult lookup = mfService.lookup(searchParameters);
        verifyRestTemplateArgument(expectedUrl);
        verifyUnsuccessfulLookup(lookup);
    }

    @Test
    public void countryConfigWithNoSpecialExtractionRulesMatchTest() {
        countryConfigWithGivenEidasIdTest(TEST_EIDAS_PERSON_IDENTIFIER,
                "?foedselsdato=19550215&landkode=CEA&utenlandskPersonIdentifikasjon=05061989");
    }

    @Test
    public void countryConfigWithShortEidasIdTest() {
        countryConfigWithGivenEidasIdTest(TEST_SHORT_EIDAS_PERSON_IDENTIFIER,
                "?foedselsdato=19550215&landkode=CEA&utenlandskPersonIdentifikasjon=05061989");
    }


    public void countryConfigWithGivenEidasIdTest(String eidasIdentifikator, String url) {
        when(attributesConfigProvider.getCountry(any())).thenReturn(Optional.of(new Country("CE", "Test", null, null, null, null)));
        String expectedUrl = MF_URL + url;
        HttpEntity<String> entity = new HttpEntity<>(createHttpHeaders());
        when(restTemplate.exchange(expectedUrl, HttpMethod.GET, entity, String.class)).thenReturn(new ResponseEntity<>(TEST_PID, HttpStatus.OK));
        Map<String, String> attributes = new HashMap<>();
        attributes.put("PersonIdentifier", eidasIdentifikator);
        EidasResponse searchParameters = EidasResponse.builder().dateOfBirth("1955-02-15")
                .attributes(attributes)
                .build();

        PersonLookupResult lookup = mfService.lookup(searchParameters);
        verifyRestTemplateArgument(expectedUrl);
        verifySuccessfulLookup(lookup);
    }

    @Test
    public void countryConfigItalyWithEidasIdTest() {
        when(authState.getCountryCode()).thenReturn("IT");
        List<Attribute> countryAttributes = Collections.singletonList(new Attribute("TaxReference", true));
        when(attributesConfigProvider.getCountry(any())).thenReturn(Optional.of(new Country("IT", "Italy", countryAttributes, "TaxReference", null, "(.*)")));
        String expectedUrl = MF_URL + "?foedselsdato=19550215&landkode=ITA&utenlandskPersonIdentifikasjon=05061989";
        HttpEntity<String> entity = new HttpEntity<>(createHttpHeaders());
        when(restTemplate.exchange(expectedUrl, HttpMethod.GET, entity, String.class)).thenReturn(new ResponseEntity<>(TEST_PID, HttpStatus.OK));
        Map<String, String> attributes = new HashMap<>();
        attributes.put("TaxReference", TEST_SHORT_EIDAS_PERSON_IDENTIFIER);
        EidasResponse searchParameters = EidasResponse.builder().dateOfBirth("1955-02-15")
                .attributes(attributes)
                .build();

        PersonLookupResult lookup = mfService.lookup(searchParameters);
        verifyRestTemplateArgument(expectedUrl);
        verifySuccessfulLookup(lookup);
    }

    @Test
    public void countryConfigWithExtractionAttributeNullNoMatchTest() {
        when(attributesConfigProvider.getCountry(any())).thenReturn(Optional.of(new Country("CE", "Test", null, "test", null, null)));
        String expectedUrl = MF_URL + "?foedselsdato=19550215&landkode=CEA&utenlandskPersonIdentifikasjon=test";
        HttpEntity<String> entity = new HttpEntity<>(createHttpHeaders());
        when(restTemplate.exchange(expectedUrl, HttpMethod.GET, entity, String.class)).thenReturn(new ResponseEntity<>(null, HttpStatus.NOT_FOUND));
        Map<String, String> attributes = new HashMap<>();
        attributes.put("PersonIdentifier", "test");
        EidasResponse searchParameters = EidasResponse.builder().dateOfBirth("1955-02-15")
                .attributes(attributes)
                .build();

        PersonLookupResult lookup = mfService.lookup(searchParameters);
        verifyRestTemplateArgument(expectedUrl);
        verifyUnsuccessfulLookup(lookup);
    }

    @Test
    public void countryConfigWithExtractionAttributeNullFallbackMatchTest() {
        when(attributesConfigProvider.getCountry(any())).thenReturn(Optional.of(new Country("CE", "Test", null, "test", null, null)));
        String expectedUrl = MF_URL + "?foedselsdato=19550215&landkode=CEA&utenlandskPersonIdentifikasjon=05061989";
        HttpEntity<String> entity = new HttpEntity<>(createHttpHeaders());
        when(restTemplate.exchange(expectedUrl, HttpMethod.GET, entity, String.class)).thenReturn(new ResponseEntity<>(TEST_PID, HttpStatus.OK));
        Map<String, String> attributes = new HashMap<>();
        attributes.put("PersonIdentifier", TEST_EIDAS_PERSON_IDENTIFIER);
        EidasResponse searchParameters = EidasResponse.builder().dateOfBirth("1955-02-15")
                .attributes(attributes)
                .build();

        PersonLookupResult lookup = mfService.lookup(searchParameters);
        verifyRestTemplateArgument(expectedUrl);
        verifySuccessfulLookup(lookup);
    }

    @Test
    public void countryConfigWithExtractionAttributeNotNullNoMatchTest() {
        when(attributesConfigProvider.getCountry(any())).thenReturn(Optional.of(new Country("CE", "Test", null, "test", null, null)));
        String expectedUrl = MF_URL + "?foedselsdato=19550215&landkode=CEA&utenlandskPersonIdentifikasjon=test";
        HttpEntity<String> entity = new HttpEntity<>(createHttpHeaders());
        when(restTemplate.exchange(expectedUrl, HttpMethod.GET, entity, String.class)).thenReturn(new ResponseEntity<>(null, HttpStatus.NOT_FOUND));

        Map<String, String> attributes = new HashMap<>();
        attributes.put("test", "test");
        EidasResponse searchParameters = EidasResponse.builder().dateOfBirth("1955-02-15")
                .attributes(attributes)
                .build();

        PersonLookupResult lookup = mfService.lookup(searchParameters);
        verifyRestTemplateArgument(expectedUrl);
        verifyUnsuccessfulLookup(lookup);
    }

    @Test
    public void countryConfigWithExtractionAttributeMatchTest() {
        when(attributesConfigProvider.getCountry(any())).thenReturn(Optional.of(new Country("CE", "Test", null, "test", null, null)));
        String expectedUrl = MF_URL + "?foedselsdato=19550215&landkode=CEA&utenlandskPersonIdentifikasjon=05061989";

        HttpEntity<String> entity = new HttpEntity<>(createHttpHeaders());
        when(restTemplate.exchange(expectedUrl, HttpMethod.GET, entity, String.class)).thenReturn(new ResponseEntity<>(TEST_PID, HttpStatus.OK));

        Map<String, String> attributes = new HashMap<>();
        attributes.put("PersonIdentifier", "test");
        attributes.put("test", TEST_EIDAS_PERSON_IDENTIFIER);
        EidasResponse searchParameters = EidasResponse.builder().dateOfBirth("1955-02-15")
                .attributes(attributes)
                .build();

        PersonLookupResult lookup = mfService.lookup(searchParameters);
        verifyRestTemplateArgument(expectedUrl);
        verifySuccessfulLookup(lookup);
    }

    @Test
    public void countryConfigWithInvalidRegexTest() {
        when(attributesConfigProvider.getCountry(any())).thenReturn(Optional.of(new Country("CE", "Test", null, "test", null, "(.*)")));
        Map<String, String> attributes = new HashMap<>();
        attributes.put("PersonIdentifier", "test");
        attributes.put("test", "test");
        EidasResponse searchParameters = EidasResponse.builder().dateOfBirth("1955-02-15")
                .attributes(attributes)
                .build();

        PersonLookupResult lookup = mfService.lookup(searchParameters);
        verifyUnsuccessfulLookup(lookup);
    }

    @Test
    public void countryConfigWithExtractionAttributeAndRegExNoMatchTest() {
        when(attributesConfigProvider.getCountry(any())).thenReturn(Optional.of(new Country("CE", "Test", null, "test", null, "(.*)")));
        String expectedUrl = MF_URL + "?foedselsdato=19550215&landkode=CEA&utenlandskPersonIdentifikasjon=test";
        HttpEntity<String> entity = new HttpEntity<>(createHttpHeaders());
        when(restTemplate.exchange(expectedUrl, HttpMethod.GET, entity, String.class)).thenReturn(new ResponseEntity<>(null, HttpStatus.NOT_FOUND));

        Map<String, String> attributes = new HashMap<>();
        attributes.put("PersonIdentifier", "test");
        attributes.put("test", "test");
        EidasResponse searchParameters = EidasResponse.builder().dateOfBirth("1955-02-15")
                .attributes(attributes)
                .build();

        PersonLookupResult lookup = mfService.lookup(searchParameters);
        verifyRestTemplateArgument(expectedUrl);
        verifyUnsuccessfulLookup(lookup);
    }

    @Test
    public void countryConfigWithExtractionAttributeAndRegExMatchTest() {
        when(attributesConfigProvider.getCountry(any())).thenReturn(Optional.of(new Country("CE", "Test", null, "test", null, "(.*)")));

        String expectedUrl = MF_URL + "?foedselsdato=19550215&landkode=CEA&utenlandskPersonIdentifikasjon=CE/NO/05061989";
        HttpEntity<String> entity = new HttpEntity<>(createHttpHeaders());
        when(restTemplate.exchange(expectedUrl, HttpMethod.GET, entity, String.class)).thenReturn(new ResponseEntity<>(TEST_PID, HttpStatus.OK));

        Map<String, String> attributes = new HashMap<>();
        attributes.put("PersonIdentifier", "test");
        attributes.put("test", TEST_EIDAS_PERSON_IDENTIFIER);
        EidasResponse searchParameters = EidasResponse.builder().dateOfBirth("1955-02-15")
                .attributes(attributes)
                .build();

        PersonLookupResult lookup = mfService.lookup(searchParameters);
        verifyRestTemplateArgument(expectedUrl);
        verifySuccessfulLookup(lookup);
    }

    @Test
    public void countryConfigWithExtractionAttributeAndRegExAndPidTypeNoMatchTest() {
        when(attributesConfigProvider.getCountry(any())).thenReturn(Optional.of(new Country("CE", "Test", null, "test", PidType.SOCIAL_SECURITY_NUMBER, "(.*)")));
        String expectedUrl = MF_URL + "?foedselsdato=19550215&landkode=CEA&utenlandskPersonIdentifikasjon=05068907693";
        HttpEntity<String> entity = new HttpEntity<>(createHttpHeaders());
        when(restTemplate.exchange(expectedUrl, HttpMethod.GET, entity, String.class)).thenReturn(new ResponseEntity<>(null, HttpStatus.NOT_FOUND));

        Map<String, String> attributes = new HashMap<>();
        attributes.put("PersonIdentifier", "test");
        attributes.put("test", TEST_PID);
        EidasResponse searchParameters = EidasResponse.builder().dateOfBirth("1955-02-15")
                .attributes(attributes)
                .build();

        PersonLookupResult lookup = mfService.lookup(searchParameters);
        verifyRestTemplateArgument(expectedUrl);
        verifyUnsuccessfulLookup(lookup);
    }

    @Test
    public void testUsingMockWhenMFResponseThrowsException() {
        when(attributesConfigProvider.getCountry(any())).thenReturn(Optional.of(new Country("CE", "Test", null, "test", PidType.SOCIAL_SECURITY_NUMBER, "(.*)")));
        String expectedUrl = MF_URL + "?foedselsdato=19550215&landkode=CEA&utenlandskPersonIdentifikasjon=05068907693";
        HttpEntity<String> entity = new HttpEntity<>(createHttpHeaders());
        when(restTemplate.exchange(expectedUrl, HttpMethod.GET, entity, String.class)).thenThrow(new RestClientException("MF is down"));

        Map<String, String> attributes = new HashMap<>();
        attributes.put("PersonIdentifier", "test");
        attributes.put("test", TEST_PID);
        EidasResponse searchParameters = EidasResponse.builder().dateOfBirth("1955-02-15")
                .attributes(attributes)
                .build();

        PersonLookupResult lookup = mfService.lookup(searchParameters);
        verifyRestTemplateArgument(expectedUrl);
        verifyUnsuccessfulLookup(lookup);
    }

    @Test
    public void countryConfigWithExtractionAttributeAndRegExAndPidTypeMatchTest() {
        when(attributesConfigProvider.getCountry(any())).thenReturn(Optional.of(new Country("CE", "Test", null, "test", PidType.SOCIAL_SECURITY_NUMBER, "(.*)")));
        String expectedUrl = MF_URL + "?foedselsdato=19550215&landkode=CEA&utenlandskPersonIdentifikasjon=CE/NO/05061989";
        HttpEntity<String> entity = new HttpEntity<>(createHttpHeaders());
        when(restTemplate.exchange(expectedUrl, HttpMethod.GET, entity, String.class)).thenReturn(new ResponseEntity<>(TEST_PID, HttpStatus.OK));

        Map<String, String> attributes = new HashMap<>();
        attributes.put("PersonIdentifier", "test");
        attributes.put("test", TEST_EIDAS_PERSON_IDENTIFIER);
        EidasResponse searchParameters = EidasResponse.builder().dateOfBirth("1955-02-15")
                .attributes(attributes)
                .build();

        PersonLookupResult lookup = mfService.lookup(searchParameters);
        verifyRestTemplateArgument(expectedUrl);
        verifySuccessfulLookup(lookup);
    }

    private void verifySuccessfulLookup(PersonLookupResult lookup) {
        assertTrue(lookup.person().isPresent());
        assertEquals(TEST_PID, lookup.person().get().fødselsnummer());
        assertEquals(PersonLookupResult.Status.OK, lookup.status());
    }

    private void verifyUnsuccessfulLookup(PersonLookupResult lookup) {
        assertFalse(lookup.person().isPresent());
        assertEquals(PersonLookupResult.Status.MULTIPLEFOUND, lookup.status());
    }

    private void verifyRestTemplateArgument(String expectedUrl) {
        ArgumentCaptor<String> argCaptor = ArgumentCaptor.forClass(String.class);
        verify(restTemplate).exchange(argCaptor.capture(), any(), any(), eq(String.class));
        assertEquals(expectedUrl, argCaptor.getValue());
    }

    @Test
    public void verifyISOCountryCodeCovertionCRreturnsCRI() {
        assertEquals("CRI", mfService.convertCountryCode("CR"));
    }

    @Test //this is Demo country
    public void verifyISOCountryCodeCovertionCEreturnsCEA() {
        assertEquals("CEA", mfService.convertCountryCode("CE"));
    }

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    @Test
    public void shouldThrowRuntimeExceptionWhenCountryCodeIsIllegal() throws RuntimeException {
        expectedEx.expect(RuntimeException.class);
        expectedEx.expectMessage("Couldn't find 3-letter country code for XX");
        mfService.convertCountryCode("XX");
    }

    private HttpHeaders createHttpHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        headers.setBasicAuth(configProvider.getMfGatewayUsername(), configProvider.getMfGatewayPassword());
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON_UTF8));
        headers.add(CLIENT_ID_HEADER, "eidas-sidp");
        return headers;
    }

}
