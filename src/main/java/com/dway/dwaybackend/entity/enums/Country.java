package com.dway.dwaybackend.entity.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Country {

    AFGHANISTAN("Afghanistan"),
    ALBANIA("Albania"),
    ALGERIA("Algeria"),
    ANDORRA("Andorra"),
    ANGOLA("Angola"),
    ARGENTINA("Argentina"),
    ARMENIA("Armenia"),
    AUSTRALIA("Australia"),
    AUSTRIA("Austria"),
    AZERBAIJAN("Azerbaijan"),
    BAHRAIN("Bahrain"),
    BANGLADESH("Bangladesh"),
    BELARUS("Belarus"),
    BELGIUM("Belgium"),
    BOLIVIA("Bolivia"),
    BOSNIA_AND_HERZEGOVINA("Bosnia and Herzegovina"),
    BRAZIL("Brazil"),
    BULGARIA("Bulgaria"),
    CAMBODIA("Cambodia"),
    CAMEROON("Cameroon"),
    CANADA("Canada"),
    CHILE("Chile"),
    CHINA("China"),
    COLOMBIA("Colombia"),
    CROATIA("Croatia"),
    CUBA("Cuba"),
    CYPRUS("Cyprus"),
    CZECH_REPUBLIC("Czech Republic"),
    DENMARK("Denmark"),
    ECUADOR("Ecuador"),
    EGYPT("Egypt"),
    ESTONIA("Estonia"),
    ETHIOPIA("Ethiopia"),
    FINLAND("Finland"),
    FRANCE("France"),
    GEORGIA("Georgia"),
    GERMANY("Germany"),
    GHANA("Ghana"),
    GREECE("Greece"),
    HUNGARY("Hungary"),
    INDIA("India"),
    INDONESIA("Indonesia"),
    IRAN("Iran"),
    IRAQ("Iraq"),
    IRELAND("Ireland"),
    ISRAEL("Israel"),
    ITALY("Italy"),
    JAPAN("Japan"),
    JORDAN("Jordan"),
    KAZAKHSTAN("Kazakhstan"),
    KENYA("Kenya"),
    KUWAIT("Kuwait"),
    KYRGYZSTAN("Kyrgyzstan"),
    LATVIA("Latvia"),
    LEBANON("Lebanon"),
    LIBYA("Libya"),
    LITHUANIA("Lithuania"),
    LUXEMBOURG("Luxembourg"),
    MALAYSIA("Malaysia"),
    MEXICO("Mexico"),
    MOLDOVA("Moldova"),
    MONGOLIA("Mongolia"),
    MOROCCO("Morocco"),
    NETHERLANDS("Netherlands"),
    NEW_ZEALAND("New Zealand"),
    NIGERIA("Nigeria"),
    NORTH_KOREA("North Korea"),
    NORWAY("Norway"),
    OMAN("Oman"),
    PAKISTAN("Pakistan"),
    PALESTINE("Palestine"),
    PERU("Peru"),
    PHILIPPINES("Philippines"),
    POLAND("Poland"),
    PORTUGAL("Portugal"),
    QATAR("Qatar"),
    ROMANIA("Romania"),
    RUSSIA("Russia"),
    SAUDI_ARABIA("Saudi Arabia"),
    SERBIA("Serbia"),
    SINGAPORE("Singapore"),
    SLOVAKIA("Slovakia"),
    SLOVENIA("Slovenia"),
    SOUTH_AFRICA("South Africa"),
    SOUTH_KOREA("South Korea"),
    SPAIN("Spain"),
    SRI_LANKA("Sri Lanka"),
    SWEDEN("Sweden"),
    SWITZERLAND("Switzerland"),
    SYRIA("Syria"),
    TAIWAN("Taiwan"),
    TAJIKISTAN("Tajikistan"),
    THAILAND("Thailand"),
    TUNISIA("Tunisia"),
    TURKEY("Turkey"),
    TURKMENISTAN("Turkmenistan"),
    UKRAINE("Ukraine"),
    UNITED_ARAB_EMIRATES("United Arab Emirates"),
    UNITED_KINGDOM("United Kingdom"),
    UNITED_STATES("United States"),
    URUGUAY("Uruguay"),
    UZBEKISTAN("Uzbekistan"),
    VENEZUELA("Venezuela"),
    VIETNAM("Vietnam"),
    YEMEN("Yemen");

    @JsonValue
    private final String displayName;

    /**
     * Allows Jackson to deserialize from the display name (e.g. "Azerbaijan" → AZERBAIJAN).
     * Without this, only the enum constant name ("AZERBAIJAN") would be accepted.
     */
    @JsonCreator
    public static Country fromDisplayName(String value) {
        for (Country country : values()) {
            if (country.displayName.equalsIgnoreCase(value)) {
                return country;
            }
        }
        throw new IllegalArgumentException("Unknown country: " + value);
    }
}