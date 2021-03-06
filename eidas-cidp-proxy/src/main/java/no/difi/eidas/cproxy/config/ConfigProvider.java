package no.difi.eidas.cproxy.config;

import com.google.common.base.MoreObjects;
import no.difi.eidas.cproxy.saml.SAMLUtil;
import no.difi.eidas.idpproxy.integrasjon.Urls;
import org.opensaml.common.xml.SAMLConstants;
import org.opensaml.saml2.metadata.IDPSSODescriptor;
import org.opensaml.saml2.metadata.SPSSODescriptor;
import org.opensaml.saml2.metadata.provider.FilesystemMetadataProvider;
import org.opensaml.saml2.metadata.provider.MetadataProvider;
import org.opensaml.saml2.metadata.provider.MetadataProviderException;
import org.opensaml.xml.parse.BasicParserPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

@Component
@Scope("singleton")
public class ConfigProvider {

    @Value("${auditlog.dir}")
    private String auditLogDir;
    @Value("${auditlog.file}")
    private String auditLogFile;
    @Value("${auditlog.dataSeparator}")
    private String auditLogDataSeparator;
    @Value("${saml.instantIssueTimeToLive}")
    private int instantIssueTimeToLive;
    @Value("${saml.instantIssueTimeSkew}")
    private int instantIssueTimeSkew;
    @Value("${eventlog.jms.queuename}")
    private String jmsQueueName;
    @Value("${eventlog.jms.url}")
    private String jmsUrl;

    @Value("${saml.sp.entityid}")
    private String spEntityID;
    @Value("${saml.idp.entityid}")
    private String idpEntityID;
    @Value("${saml.idp.metadata.file}")
    private String idpMetadataProviderFile;
    @Value("${saml.sp.metadata.file}")
    private String spMetadataProviderFile;

    @Value("${cidpproxy.url}")
    private String cidpProxyUrl;

    @Value("${cidpproxy.keystore.location}")
    private String cIDPProxyKeystoreLocation;
    @Value("${cidpproxy.keystore.type}")
    private String cIDPProxyKeystoreType;
    @Value("${cidpproxy.keystore.alias}")
    private String cIDPProxyKeystoreAlias;
    @Value("${cidpproxy.keystore.password}")
    private String cIDPProxyKeystorePassword;
    @Value("${cidpproxy.keystore.privatekeyPassword}")
    private String cIDPProxyKeystorePrivatekeyPassword;
    @Value("${mf.gateway.url}")
    private String mfGatewayUrl;
    @Value("${mf.gateway.username}")
    private String mfGatewayUsername;
    @Value("${mf.gateway.password}")
    private String mfGatewayPassword;
    @Value("${mf.gateway.timeout}")
    private int mfGatewayTimeout;
    @Value("${mf.gateway.retryCount}")
    private int mfRetryCount;
    @Value("${message.path}")
    private String messagePath;
    @Value("${users.test:06045000883.HANNE/BREI/1950-04-06}")
    private String mockUsers;

    private static final Logger log = LoggerFactory.getLogger(ConfigProvider.class);

    public Map<String, String> mockUsers() {
        return parseTestUsers(mockUsers);
    }

    private MetadataProvider idpMetadataProvider;
    private MetadataProvider spMetadataProvider;
    private IDPSSODescriptor idpssoDescriptor;
    private SPSSODescriptor spssoDescriptor;

    public String getMfGatewayUrl() {
        return mfGatewayUrl;
    }

    public String getMfGatewayUsername() {
        return mfGatewayUsername;
    }

    public String getMfGatewayPassword() {
        return mfGatewayPassword;
    }

    public String auditLogDir() {
        return auditLogDir;
    }

    public String auditLogFile() {
        return auditLogFile;
    }

    public String auditLogDataSeparator() {
        return auditLogDataSeparator;
    }

    public int instantIssueTimeToLive() {
        return instantIssueTimeToLive;
    }

    public int instantIssueTimeSkew() {
        return instantIssueTimeSkew;
    }

    public String jmsQueueName() {
        return jmsQueueName;
    }

    public String jmsUrl() {
        return jmsUrl;
    }

    public String spEntityID() {
        return spEntityID;
    }

    public String idpEntityID() {
        return idpEntityID;
    }

    public String cIDPProxyKeystoreLocation() {
        return cIDPProxyKeystoreLocation;
    }

    public String cIDPProxyKeystoreType() {
        return cIDPProxyKeystoreType;
    }

    public String cIDPProxyKeystoreAlias() {
        return cIDPProxyKeystoreAlias;
    }

    public String cIDPProxyKeystorePassword() {
        return cIDPProxyKeystorePassword;
    }

    public String cIDPProxyKeystorePrivatekeyPassword() {
        return cIDPProxyKeystorePrivatekeyPassword;
    }

    public URL cidpProxyAuthUrl() {
        return endpointUrl(cidpProxyUrl, "/auth", "cidpProxyAuthUrl");
    }

    public Map<String, String> parseTestUsers(String testUsers) {
        Map<String,String> map = new HashMap<>();
        for (String entry: testUsers.split(",")) {
            if (entry.length() == 0) {
                continue;
            }
            String[] split = entry.split(":");
            String personIdentifikator = split[0];
            log.debug("Testuser: key: -" + personIdentifikator + "- value: " + split[1]);
            map.put(personIdentifikator, split[1]);
        }
        return map;
    }

    public URL cidpProxyMetadataUrl() {
        return endpointUrl(cidpProxyUrl, "/", "cidpMetadatUrl");
    }

    private URL url(String value, String description) {
        try {
            return Urls.url(value);
        } catch (Urls.UrlMalformed e) {
            throw new RuntimeException(String.format("Wrong format for URL: %s for %s", value, description), e);
        }
    }

    protected String concatUrl(String base, String resource) {
        return base.replaceAll("/\\z", "") + "/" + resource.replaceAll("\\A/", "");
    }

    protected URL endpointUrl(String base, String endpoint, String description) {
        return url(concatUrl(base, endpoint), description);
    }

    private MetadataProvider loadMetadataProvider(final String filename) {
        try {
            final FilesystemMetadataProvider provider = new FilesystemMetadataProvider(new File(filename));
            provider.setParserPool(new BasicParserPool());
            provider.initialize();
            return provider;
        } catch (MetadataProviderException e) {
            throw new RuntimeException(e);
        }
    }

    public final MetadataProvider getIDPMetadataProvider() {
        if (!(idpMetadataProvider == null)) {
            return idpMetadataProvider;
        } else {
            idpMetadataProvider = loadMetadataProvider(idpMetadataProviderFile);
            return idpMetadataProvider;
        }
    }

    public final IDPSSODescriptor getIDPSSODescriptor() {
        if (!(idpssoDescriptor == null)) {
            return idpssoDescriptor;
        } else {
            try {
                idpssoDescriptor = getIDPMetadataProvider().getEntityDescriptor(idpEntityID()).getIDPSSODescriptor(SAMLConstants.SAML20P_NS);
                return idpssoDescriptor;
            } catch (MetadataProviderException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public final MetadataProvider getSPMetadataProvider() {
        if (!(spMetadataProvider == null)) {
            return spMetadataProvider;
        } else {
            spMetadataProvider = loadMetadataProvider(spMetadataProviderFile);
            return spMetadataProvider;
        }
    }

    public final SPSSODescriptor getSPSSODescriptor() {
        if (!(spssoDescriptor == null)) {
            return spssoDescriptor;
        } else {
            try {
                spssoDescriptor = getSPMetadataProvider().getEntityDescriptor(spEntityID()).getSPSSODescriptor(SAMLConstants.SAML20P_NS);
                return spssoDescriptor;
            } catch (MetadataProviderException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public String toString() {
        return MoreObjects
                .toStringHelper(this)
                .add("auditLogDir", auditLogDir)
                .add("auditLogFile", auditLogFile)
                .add("auditLogDataSeparator", auditLogDataSeparator)
                .add("instantIssueTimeToLive", instantIssueTimeToLive)
                .add("instantIssueTimeSkew", instantIssueTimeSkew)
                .add("jmsQueueName", jmsQueueName)
                .add("jmsUrl", jmsUrl)
                .add("idpPMetadataProvider", idpMetadataProvider)
                .add("idpPMetadataProvider", spMetadataProvider)
                .toString();
    }

    public String getArtifactResolutionService() {
        return SAMLUtil.resolveSOAPArtifactResolutionServiceLocation(getIDPSSODescriptor());
    }

    public String getSingleSignOnService() {
        return SAMLUtil.resolveSingleSignOnServiceLocation(getIDPSSODescriptor());
    }

    public String getAssertionConsumerService() {
        return SAMLUtil.resolveAssertionConsumerServiceLocation(getSPSSODescriptor());
    }
    
    public String getIDPSingleLogoutService(){
    	return SAMLUtil.resolveIDPSingleSignoutLocation(getIDPSSODescriptor());
    }

    public String getMessagePath(){
        return messagePath;
    }

}
