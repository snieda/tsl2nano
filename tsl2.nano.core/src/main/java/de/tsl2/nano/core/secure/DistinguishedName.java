package de.tsl2.nano.core.secure;

import java.io.IOException;

import javax.security.auth.x500.X500Principal;

import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.util.Util;
import sun.security.x509.X500Name;

/**
 * creates an object to be used as issuer or subject for certificates. for specification see
 * <a href="http://www.ietf.org/rfc/rfc2253.txt">RFC 2253: Lightweight Directory Access Protocol (v3): UTF-8 String
 * Representation of Distinguished Names</a>, {@link X500Principal} and {@link X500Name}.
 * 
 * @author Tom
 * @version $Revision$
 */
public class DistinguishedName {

    /** CN */
    private String commonName;
    /** C */
    private String countryName;
    /** ST */
    private String stateOrProvince;
    /** L */
    private String localityName;
    /** O */
    private String organizationName;
    /** OU */
    private String organizationalUnitName;
    /** DC */
    private String domainComponent;
    /** STREET */
    private String streetAddress;
    /** E */
    private String email;
    private boolean outputSimpleValuesOnly;

    public DistinguishedName(String commonName,
            String countryName) {
        this(commonName, countryName, null);
    }
    
    public DistinguishedName(String commonName,
            String countryName,
            String stateOrProvince) {
        this(commonName, countryName, stateOrProvince, null, null, null);
    }
    
    public DistinguishedName(String commonName,
            String countryName,
            String stateOrProvince,
            String localityName,
            String organizationName,
            String organizationalUnitName) {
        this(commonName, countryName, stateOrProvince, localityName, organizationName, organizationalUnitName, null, null, null);
    }

    public DistinguishedName(String commonName,
            String countryName,
            String stateOrProvince,
            String localityName,
            String organizationName,
            String organizationalUnitName,
            String domainComponent,
            String streetAddress,
            String email) {
        this.commonName = commonName;
        this.countryName = countryName;
        this.stateOrProvince = stateOrProvince;
        this.localityName = localityName;
        this.organizationName = organizationName;
        this.organizationalUnitName = organizationalUnitName;
        this.domainComponent = domainComponent;
        this.streetAddress = streetAddress;
        this.email = email;
    }

    public DistinguishedName(String dn) {
        String[] dnParts = dn.split(",");
        for (String dnPart : dnParts) {
            dnPart = dnPart.trim();
            if (dnPart.toUpperCase().startsWith("CN=")) {
                commonName = dnPart.replaceFirst("CN=", "").trim();
            } else if (dnPart.toUpperCase().startsWith("OU=")) {
                organizationalUnitName = dnPart.replaceFirst("OU=", "").trim();
            } else if (dnPart.toUpperCase().startsWith("O=")) {
                organizationName = dnPart.replaceFirst("O=", "").trim();
            } else if (dnPart.toUpperCase().startsWith("L=")) {
                localityName = dnPart.replaceFirst("L=", "").trim();
            } else if (dnPart.toUpperCase().startsWith("C=")) {
                countryName = dnPart.replaceFirst("C=", "").trim();
            } else if (dnPart.toUpperCase().startsWith("ST=")) {
                stateOrProvince = dnPart.replaceFirst("ST=", "").trim();
            } else if (dnPart.toUpperCase().startsWith("DC=")) {
                domainComponent = dnPart.replaceFirst("DC=", "").trim();
            } else if (dnPart.toUpperCase().startsWith("STREET=")) {
                streetAddress = dnPart.replaceFirst("STREET=", "").trim();
            } else if (dnPart.toUpperCase().startsWith("E=")) {
                email = dnPart.replaceFirst("E=", "").trim();
            } else if (dnPart.toUpperCase().startsWith("EMAILADDRESS=")) {
                email = dnPart.replaceFirst("EMAILADDRESS=", "").trim();
            }
        }

    }

    /** poor hack to have the values on getters (without keynames). will be ignored on toString() */
    public void setOutputSimpleValuesOnly() {
        outputSimpleValuesOnly = true;
    }
    public String getCountryName() {
        if (countryName == null || countryName.trim().length() == 0) {
            return "";
        }
        return (outputSimpleValuesOnly ? "" : "C=") + countryName;
    }

    public String getCommonName() {
        if (commonName == null || commonName.trim().length() == 0) {
            return "";
        }
        return (outputSimpleValuesOnly ? "" : "CN=") + commonName;
    }

    public String getStateOrProvince() {
        if (stateOrProvince == null || stateOrProvince.trim().length() == 0) {
            return "";
        }
        return (outputSimpleValuesOnly ? "" : "ST=") + stateOrProvince;
    }

    public String getLocalityName() {
        if (localityName == null || localityName.trim().length() == 0) {
            return "";
        }
        return (outputSimpleValuesOnly ? "" : "L=") + localityName;
    }

    public String getOrganizationName() {
        if (organizationName == null || organizationName.trim().length() == 0) {
            return "";
        }
        return (outputSimpleValuesOnly ? "" : "O=") + organizationName;
    }

    public String getOrganizationalUnitName() {
        if (organizationalUnitName == null || organizationalUnitName.trim().length() == 0) {
            return "";
        }
        return (outputSimpleValuesOnly ? "" : "OU=") + organizationalUnitName;
    }

    public String getDomainComponent() {
        if (domainComponent == null || domainComponent.trim().length() == 0) {
            return "";
        }
        return (outputSimpleValuesOnly ? "" : "DC=") + domainComponent;
    }

    public String getStreetAddress() {
        if (streetAddress == null || streetAddress.trim().length() == 0) {
            return "";
        }
        return (outputSimpleValuesOnly ? "" : "STREET=") + streetAddress;
    }

    public String getEmail() {
        if (email == null || email.trim().length() == 0) {
            return "";
        }
        return (outputSimpleValuesOnly ? "" : "EMAILADDRESS=") + email;
    }

    public Object toX500Name() {
        try {
            return new X500Name(toString());
        } catch (IOException e) {
            ManagedException.forward(e);
            return null;
        }
    }

    public X500Principal toX500Principal() {
        return new X500Principal(toString());
    }

    @Override
    public String toString() {
        boolean outputSimpleValuesOnly = this.outputSimpleValuesOnly;
        this.outputSimpleValuesOnly = false;
        try {
            return Util.toString(",", getCommonName(), getOrganizationalUnitName(), getOrganizationName(),
                    getLocalityName(), getStateOrProvince(), getDomainComponent(), getStreetAddress(), getEmail(),
                    getCountryName());
        } finally {
            this.outputSimpleValuesOnly = outputSimpleValuesOnly;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        DistinguishedName that = (DistinguishedName) o;

        if (commonName != null ? !commonName.equals(that.commonName) : that.commonName != null)
            return false;
        if (countryName != null ? !countryName.equals(that.countryName) : that.countryName != null)
            return false;
        if (stateOrProvince != null ? !stateOrProvince.equals(that.stateOrProvince) : that.stateOrProvince != null)
            return false;
        if (localityName != null ? !localityName.equals(that.localityName) : that.localityName != null)
            return false;
        if (organizationName != null ? !organizationName.equals(that.organizationName) : that.organizationName != null)
            return false;
        if (organizationalUnitName != null ? !organizationalUnitName.equals(that.organizationalUnitName)
            : that.organizationalUnitName != null)
            return false;
        if (domainComponent != null ? !domainComponent.equals(that.domainComponent) : that.domainComponent != null)
            return false;
        if (streetAddress != null ? !streetAddress.equals(that.streetAddress) : that.streetAddress != null)
            return false;
        return !(email != null ? !email.equals(that.email) : that.email != null);

    }

    @Override
    public int hashCode() {
        int result = commonName != null ? commonName.hashCode() : 0;
        result = 31 * result + (countryName != null ? countryName.hashCode() : 0);
        result = 31 * result + (stateOrProvince != null ? stateOrProvince.hashCode() : 0);
        result = 31 * result + (localityName != null ? localityName.hashCode() : 0);
        result = 31 * result + (organizationName != null ? organizationName.hashCode() : 0);
        result = 31 * result + (organizationalUnitName != null ? organizationalUnitName.hashCode() : 0);
        result = 31 * result + (domainComponent != null ? domainComponent.hashCode() : 0);
        result = 31 * result + (streetAddress != null ? streetAddress.hashCode() : 0);
        result = 31 * result + (email != null ? email.hashCode() : 0);
        return result;
    }
}
