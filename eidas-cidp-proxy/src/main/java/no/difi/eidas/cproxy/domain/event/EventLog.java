package no.difi.eidas.cproxy.domain.event;

import no.difi.eidas.cproxy.domain.node.NodeAttributes;
import no.difi.eidas.cproxy.domain.node.NodeAuthnRequest;
import no.difi.eidas.idpproxy.SubjectBasicAttribute;
import no.idporten.domain.log.LogEntry;
import no.idporten.domain.log.LogEntryLogType;

import no.idporten.log.event.EventLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EventLog {

    private final EventLogger eventLogger;
    static final LogEntryLogType EIDAS_ATTRIBUTE_CONSENT_OK = new LogEntryLogType("EIDAS_ATTRIBUTE_CONSENT_OK ", "eIDAS outgoing authentication (via c-proxy) attribute consent OK");
    static final LogEntryLogType EIDAS_ATTRIBUTE_CONSENT_REJECTED = new LogEntryLogType("EIDAS_ATTRIBUTE_CONSENT_REJECTED", "eIDAS outgoing authentication (via c-proxy) attribute consent rejected");

    @Autowired
    public EventLog(EventLogger eventLogger) {
        this.eventLogger = eventLogger;
    }

    public void attributeConsentOk(NodeAuthnRequest nodeAuthnRequest, NodeAttributes response) {
        logConsent(EIDAS_ATTRIBUTE_CONSENT_OK, nodeAuthnRequest, response);
    }

    public void attributeConsentRejected(NodeAuthnRequest nodeAuthnRequest, NodeAttributes response) {
        logConsent(EIDAS_ATTRIBUTE_CONSENT_REJECTED, nodeAuthnRequest, response);
    }

    protected void logConsent(LogEntryLogType logType, NodeAuthnRequest nodeAuthnRequest, NodeAttributes response) {
        LogEntry logEntry = new LogEntry(logType);
        logEntry.setPersonIdentifier(response.get(SubjectBasicAttribute.PersonIdentifier).value().orNull());
        logEntry.setIssuer(nodeAuthnRequest.spCountry());
        logEntry.setOnBehalfOf(onBehalfOf(nodeAuthnRequest.spInstitution(), nodeAuthnRequest.spApplication()));
        eventLogger.log(logEntry);
    }

    protected String onBehalfOf(String spInstitution, String spApplication) {
        if (hasValue(spInstitution)) {
            return spInstitution;
        }
        if (hasValue(spApplication)) {
            return spApplication;
        }
        return null;
    }

    protected boolean hasValue(String s) {
        return s != null && s.length() > 0 && !s.matches("(NA|NOT AVAILABLE|Unknown)");
    }

}
