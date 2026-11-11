package edu.taller.ascii;

public final class AsciiTest {
    private static int count;
    private static void eq(String expected, String actual) {
        if (!expected.equals(actual)) throw new AssertionError("Esperado: " + expected + " / obtenido: " + actual);
        count++;
    }
    private static void rejects(String expected, Runnable run) {
        try { run.run(); }
        catch (IllegalArgumentException e) { eq(expected, e.getMessage()); return; }
        throw new AssertionError("Se esperaba un error: " + expected);
    }
    public static void main(String[] args) {
        eq("72 79 76 65", Ascii.encode("HOLA", " "));
        eq("65, 32, 90", Ascii.encode("A Z", ", "));
        eq("32", Ascii.encode(" ", " "));
        eq("HOLA", Ascii.decode(" 72, 79; 76\n65 "));
        eq("A\tZ\n", Ascii.decode("65 9 90 10"));
        eq("␀␡", Ascii.display(Ascii.decode("0 127")));
        eq("␠", Ascii.display(Ascii.decode("32")));
        for (int i = 0; i <= 127; i++) {
            String original = String.valueOf((char)i);
            eq(Integer.toString(i), Ascii.encode(original, "; "));
            eq(original, Ascii.decode(Integer.toString(i)));
        }
        rejects("emptyText", () -> Ascii.encode("", " "));
        rejects("emptyCodes", () -> Ascii.decode(" \n "));
        rejects("nonAscii", () -> Ascii.encode("niño 😀", " "));
        rejects("invalid", () -> Ascii.decode("72,,79"));
        rejects("invalid", () -> Ascii.decode("-1"));
        rejects("invalid", () -> Ascii.decode("72x"));
        rejects("range", () -> Ascii.decode("128"));
        rejects("range", () -> Ascii.decode("9999999999999999999999"));
        eq("#########\n#       #\n#   A   #\n#       #\n#########", Ascii.art('A', '#'));
        eq("modo,entrada,resultado\r\n\"texto_a_ascii\",\"A,\"\"B\",\"65 44 34 66\"\r\n",
            Ascii.csv("texto_a_ascii", "A,\"B", "65 44 34 66"));
        eq("modo,entrada,resultado\r\n\"modo\",\"'=1+1\",\"A\nB\"\r\n", Ascii.csv("modo", "=1+1", "A\nB"));
        System.out.println(count + " comprobaciones correctas.");
    }
}
