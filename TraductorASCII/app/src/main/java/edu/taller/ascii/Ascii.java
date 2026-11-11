package edu.taller.ascii;

import java.util.regex.Pattern;

final class Ascii {
    private static final Pattern SEPARATOR = Pattern.compile("\\s*[,;]\\s*|\\s+");
    private static final Pattern DECIMAL = Pattern.compile("[0-9]+");
    private Ascii() {}

    static String encode(String value, String separator) {
        if (value.isEmpty()) throw new IllegalArgumentException("emptyText");
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c > 127) throw new IllegalArgumentException("nonAscii");
            if (i > 0) result.append(separator);
            result.append((int)c);
        }
        return result.toString();
    }

    static String decode(String value) {
        value = value.trim();
        if (value.isEmpty()) throw new IllegalArgumentException("emptyCodes");
        StringBuilder result = new StringBuilder();
        for (String token : SEPARATOR.split(value, -1)) {
            if (!DECIMAL.matcher(token).matches()) throw new IllegalArgumentException("invalid");
            int code;
            try { code = Integer.parseInt(token); }
            catch (NumberFormatException ex) { throw new IllegalArgumentException("range"); }
            if (code > 127) throw new IllegalArgumentException("range");
            result.append((char)code);
        }
        return result.toString();
    }

    // A visible representation for control codes; clipboard retains the decoded value.
    static String display(String value) {
        StringBuilder out = new StringBuilder();
        for (char c : value.toCharArray()) {
            if (c == '\n' || c == '\t') out.append(c);
            else if (c < 32 || c == 127) out.appendCodePoint(c == 127 ? 0x2421 : 0x2400 + c);
            else out.append(c);
        }
        if (!value.isEmpty() && value.trim().isEmpty()) {
            return out.toString().replace(" ", "␠").replace("\t", "␉").replace("\n", "␊");
        }
        return out.toString();
    }

    static String art(char c, char ink) {
        if (c < 33 || c > 126) throw new IllegalArgumentException("art");
        String row = new String(new char[9]).replace('\0', ink);
        return row + "\n" + ink + "       " + ink + "\n" + ink + "   " + c
                + "   " + ink + "\n" + ink + "       " + ink + "\n" + row;
    }

    static String csv(String mode, String input, String result) {
        return "modo,entrada,resultado\r\n" + cell(mode) + "," + cell(input) + "," + cell(result) + "\r\n";
    }

    private static String cell(String value) {
        // Avoid interpreting user input as a formula when the CSV is opened in a spreadsheet.
        String trimmed = value.trim();
        if ((!trimmed.isEmpty() && "=+-@".indexOf(trimmed.charAt(0)) >= 0)
                || value.startsWith("\t") || value.startsWith("\r") || value.startsWith("\n")) {
            value = "'" + value;
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
