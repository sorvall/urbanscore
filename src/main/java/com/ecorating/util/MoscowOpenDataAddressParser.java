package com.ecorating.util;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Locale;

/** Общие адреса из полей data.mos.ru (ObjectAddress и др.) и запрос в Nominatim. */
public final class MoscowOpenDataAddressParser {

    private MoscowOpenDataAddressParser() {}

    public static String firstStreetAddressFromCells(JsonNode cells) {
        JsonNode arr = cells.path("ObjectAddress");
        if (arr.isArray()) {
            for (JsonNode o : arr) {
                if (o != null && o.isObject()) {
                    String a = o.path("Address").asText("").trim();
                    if (!a.isEmpty()) {
                        return a;
                    }
                }
            }
        }
        return textOrEmpty(cells, "Location", "Address");
    }

    /**
     * Полный адрес из data.mos.ru часто не находится в Nominatim; оставляем улицу, номер и «Москва».
     */
    public static String nominatimQueryFromOfficialAddress(String raw) {
        if (raw == null) {
            return "";
        }
        String s = raw.trim();
        if (s.isEmpty()) {
            return "";
        }
        s = s.replaceFirst(
                "(?iu)^Российская\\s+Федерация,\\s*город\\s+Москва,\\s*внутригородская\\s+территория\\s+муниципальный\\s+округ\\s+[^,]+,\\s*",
                ""
        );
        s = s.replaceFirst("(?iu)^город\\s+Москва,\\s*", "");
        s = s.replaceAll("(?iu),\\s*строение\\s+[0-9а-яА-ЯёЁ/\\-]+", "");
        s = s.replaceAll("(?iu)\\bдом\\s*", "");
        s = s.replace(',', ' ');
        s = s.replaceAll("\\s+", " ").trim();
        String lower = s.toLowerCase(Locale.ROOT);
        if (!lower.contains("москва")) {
            s = s + " Москва";
        }
        return s;
    }

    private static String textOrEmpty(JsonNode cells, String... keys) {
        for (String key : keys) {
            if (cells.has(key) && cells.get(key).isTextual()) {
                String t = cells.get(key).asText("").trim();
                if (!t.isEmpty()) {
                    return t;
                }
            }
        }
        return "";
    }
}
