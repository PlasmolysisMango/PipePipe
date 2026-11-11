package com.plasmolysismango.aacg;

import java.util.ArrayList;
import java.util.List;

/** A literal-only JavaScript tokenizer for the site's inline configuration scripts. */
final class AacgJsTokenizer {
    static final class Token {
        final String text;
        final boolean quoted;

        Token(final String text, final boolean quoted) {
            this.text = text;
            this.quoted = quoted;
        }
    }

    private AacgJsTokenizer() {
    }

    static List<Token> tokenize(final String script) {
        if (script == null || !isValidUtf16(script)) {
            throw new IllegalArgumentException("Script is not valid UTF-16");
        }
        final List<Token> tokens = new ArrayList<>();
        int i = 0;
        while (i < script.length()) {
            final int codePoint = script.codePointAt(i);
            final int size = Character.charCount(codePoint);
            if (Character.isWhitespace(codePoint) || codePoint == 0xFEFF
                    || codePoint == 0x2028 || codePoint == 0x2029) {
                i += size;
                continue;
            }
            if (script.startsWith("//", i) || script.startsWith("<!--", i)) {
                while (i < script.length()) {
                    final int r = script.codePointAt(i);
                    if (r == '\r' || r == '\n' || r == 0x2028 || r == 0x2029) {
                        break;
                    }
                    i += Character.charCount(r);
                }
                continue;
            }
            if (script.startsWith("/*", i)) {
                final int end = script.indexOf("*/", i + 2);
                if (end < 0) {
                    throw new IllegalArgumentException("Unterminated script comment");
                }
                i = end + 2;
                continue;
            }
            if (script.charAt(i) == '\'' || script.charAt(i) == '"') {
                final StringResult result = readString(script, i);
                tokens.add(new Token(result.value, true));
                i = result.end;
                continue;
            }
            // Reject lexical forms that could hide executable assignments or string decoys.
            if (script.charAt(i) == '`' || script.charAt(i) == '/' || script.charAt(i) == '\\') {
                throw new IllegalArgumentException("Unsupported script syntax '"
                        + script.charAt(i) + "'");
            }
            int end = i + size;
            if (jsWord(codePoint)) {
                while (end < script.length()) {
                    final int r = script.codePointAt(end);
                    if (!jsWord(r)) {
                        break;
                    }
                    end += Character.charCount(r);
                }
            }
            tokens.add(new Token(script.substring(i, end), false));
            i = end;
        }
        return tokens;
    }

    private static final class StringResult {
        final String value;
        final int end;

        StringResult(final String value, final int end) {
            this.value = value;
            this.end = end;
        }
    }

    private static StringResult readString(final String script, final int start) {
        final StringBuilder value = new StringBuilder();
        final char quote = script.charAt(start);
        int i = start + 1;
        while (i < script.length()) {
            final char c = script.charAt(i);
            i++;
            if (c == quote) {
                return new StringResult(value.toString(), i);
            }
            if (c == '\n' || c == '\r') {
                throw new IllegalArgumentException("Unescaped newline in script string");
            }
            if (c != '\\') {
                value.append(c);
                continue;
            }
            if (i >= script.length()) {
                throw new IllegalArgumentException("Unterminated script string");
            }
            final char escape = script.charAt(i);
            i++;
            switch (escape) {
                case '\n':
                    break;
                case '\r':
                    if (i < script.length() && script.charAt(i) == '\n') {
                        i++;
                    }
                    break;
                case 'b':
                    value.append('\b');
                    break;
                case 'f':
                    value.append('\f');
                    break;
                case 'n':
                    value.append('\n');
                    break;
                case 'r':
                    value.append('\r');
                    break;
                case 't':
                    value.append('\t');
                    break;
                case 'v':
                    value.append('\u000B');
                    break;
                case '0':
                    if (i < script.length() && script.charAt(i) >= '0'
                            && script.charAt(i) <= '9') {
                        throw new IllegalArgumentException(
                                "Unsupported legacy numeric escape");
                    }
                    value.append('\0');
                    break;
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                    throw new IllegalArgumentException("Unsupported legacy numeric escape");
                case 'x':
                    value.append((char) readHex(script, i, 2));
                    i += 2;
                    break;
                case 'u':
                    final int code = readHex(script, i, 4);
                    i += 4;
                    char decoded = (char) code;
                    if (Character.isHighSurrogate(decoded)) {
                        if (i + 6 <= script.length() && script.startsWith("\\u", i)) {
                            final int low = readHex(script, i + 2, 4);
                            if (Character.isLowSurrogate((char) low)) {
                                value.append(decoded).append((char) low);
                                i += 6;
                                break;
                            }
                        }
                        throw new IllegalArgumentException(
                                "Unpaired surrogate in script string");
                    }
                    if (Character.isLowSurrogate(decoded)) {
                        throw new IllegalArgumentException(
                                "Unpaired surrogate in script string");
                    }
                    value.append(decoded);
                    break;
                default:
                    final int r = script.codePointAt(i - 1);
                    i += Character.charCount(r) - 1;
                    if (r != 0x2028 && r != 0x2029) {
                        value.appendCodePoint(r);
                    }
                    break;
            }
        }
        throw new IllegalArgumentException("Unterminated script string");
    }

    private static int readHex(final String script, final int start, final int length) {
        if (start + length > script.length()) {
            throw new IllegalArgumentException("Incomplete hexadecimal string escape");
        }
        int result = 0;
        for (int i = start; i < start + length; i++) {
            final int digit = Character.digit(script.charAt(i), 16);
            if (digit < 0) {
                throw new IllegalArgumentException("Invalid hexadecimal string escape");
            }
            result = result * 16 + digit;
        }
        return result;
    }

    private static boolean jsWord(final int r) {
        return Character.isLetter(r) || Character.isDigit(r)
                || Character.getType(r) == Character.NON_SPACING_MARK
                || Character.getType(r) == Character.COMBINING_SPACING_MARK
                || Character.getType(r) == Character.ENCLOSING_MARK
                || r == '_' || r == '$' || r == 0x200C || r == 0x200D;
    }

    static boolean isIdentifier(final Token token) {
        if (token.quoted) {
            return true;
        }
        final int first = token.text.codePointAt(0);
        return first == '_' || first == '$' || Character.isLetter(first);
    }

    private static boolean isValidUtf16(final String value) {
        for (int i = 0; i < value.length(); i++) {
            final char c = value.charAt(i);
            if (Character.isHighSurrogate(c)) {
                if (i + 1 >= value.length() || !Character.isLowSurrogate(value.charAt(i + 1))) {
                    return false;
                }
                i++;
            } else if (Character.isLowSurrogate(c)) {
                return false;
            }
        }
        return true;
    }
}
