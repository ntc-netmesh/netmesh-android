package net.pregi.networking.speedtest.provider.ookla;

import net.pregi.networking.speedtest.ServerEntry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.math.BigDecimal;
import java.nio.charset.Charset;

import okhttp3.HttpUrl;

public class OoklaServerEntry extends ServerEntry {
    private static Charset ISO_8859_1 = Charset.forName("ISO_8859_1");

    /** <p>Prepares the bytes for a request header. Does not include the final CRLF that should end a header.</p> */
    private static byte[] prepareRequest(String type, HttpUrl uri) {
        StringBuilder request = new StringBuilder();
        String query = uri.encodedQuery();

        request.append(type);
        request.append(' ');
        request.append(uri.encodedPath());
        if (query != null) {
            request.append('?');
            request.append(query);
        }
        request.append(" HTTP/1.1\r\n");

        request.append("Host: ");
        request.append(uri.host());
        request.append(':');
        request.append(uri.port());
        request.append("\r\n");

        //request.append("User-Agent: Java/1.7\r\n");
        //request.append("Accept-Language: en-US,en\r\n");
        return request.toString().getBytes(ISO_8859_1);
    }

    // I suspect these aren't essential in this app's core operation.
    // They're allowed to be null in case the received JSON's format changes.

    private int distance;
    public int getDistance() {
        return distance;
    }

    private BigDecimal latitude;
    public BigDecimal getLatitude() {
        return latitude;
    }

    private BigDecimal longitude;
    public BigDecimal getLongitude() {
        return longitude;
    }

    private Boolean preferred;
    public Boolean getPreferred() {
        return preferred;
    }

    private Boolean httpsFunctional;
    public Boolean getHttpsFunctional() {
        return httpsFunctional;
    }

    public OoklaServerEntry(ObjectNode serverEntry) {
        // A variable for nodes we're allowing to be null,
        //      in case the JSON changes.
        // The others are critical to the operation of the app.
        JsonNode n;

        setId(serverEntry.get("id").asLong(-1));
        distance = serverEntry.get("distance").intValue();
        setUrl(serverEntry.get("url").textValue());
        setName(serverEntry.get("name").textValue());

        setSponsor(((n=serverEntry.get("sponsor")) != null) ? n.textValue() : "");
        latitude = ((n=serverEntry.get("lat")) != null) ? n.isBigDecimal() ? n.decimalValue() : new BigDecimal(n.asText()) : null;
        longitude = ((n=serverEntry.get("lon")) != null) ? n.isBigDecimal() ? n.decimalValue() : new BigDecimal(n.asText()) : null;
        setCountry(((n=serverEntry.get("country")) != null) ? n.textValue() : "");
        setCountryCode(((n=serverEntry.get("cc")) != null) ? n.textValue() : "");
        preferred = ((n=serverEntry.get("preferred")) != null) ? n.asBoolean() : null;
        httpsFunctional = ((n=serverEntry.get("https_functional")) != null) ? n.asBoolean() : null;

        String targetHost = serverEntry.get("host").textValue();
        int colon = targetHost.indexOf(':');
        if (colon != -1) {
            setHost(targetHost.substring(0, colon));
            setPort(Integer.parseInt(targetHost.substring(colon+1)));
        } else {
            setHost(targetHost);
        }
    }

    public OoklaServerEntry(long id, String host, int port, String sponsor, int distance) {
        setId(id);
        setHost(host);
        setPort(port);
        setSponsor(sponsor);

        this.distance = distance;
    }
}
