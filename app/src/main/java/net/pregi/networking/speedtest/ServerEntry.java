package net.pregi.networking.speedtest;

public class ServerEntry {
    private long id;
    protected void setId(long value) {
        id = value;
    }
    public long getId() {
        return id;
    }

    private String name;
    protected void setName(String value) {
        name = value;
    }
    public String getName() {
        return name;
    }

    private String sponsor;
    protected void setSponsor(String value) {
        sponsor = value;
    }
    public String getSponsor() {
        return sponsor;
    }

    private String country;
    protected void setCountry(String value) {
        country = value;
    }
    protected void setCountry(String value, String code) {
        country = value;
        countryCode = code;
    }
    public String getCountry() {
        return country;
    }

    private String countryCode;
    protected void setCountryCode(String value) {
        countryCode = value;
    }
    public String getCountryCode() {
        return countryCode;
    }

    private String host;
    protected void setHost(String value) {
        host = value;
    }
    public String getHost() {
        return host;
    }

    private int port = 80;
    protected void setPort(int value) {
        port = value;
    }
    public int getPort() {
        return port;
    }

    private String url;
    protected void setUrl(String value) {
        url = value;
    }
    public String getUrl() {
        return url;
    }

    public ServerEntry () {

    }

    public ServerEntry(long id, String name, String sponsor, String country, String countryCode, String host, int port, String url) {
        this.id = id;
        this.name = name;
        this.sponsor = sponsor;
        this.country = country;
        this.countryCode = countryCode;
        this.host = host;
        this.port = port;
        this.url = url;
    }
}
