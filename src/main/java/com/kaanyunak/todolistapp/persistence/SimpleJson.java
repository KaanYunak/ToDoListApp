package com.kaanyunak.todolistapp.persistence;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SimpleJson {
    private SimpleJson() {
    }

    public static Object parse(String text) {
        return new Parser(text == null ? "" : text).parse();
    }

    public static String stringify(Object value) {
        StringBuilder builder = new StringBuilder();
        write(value, builder, 0);
        return builder.toString();
    }

    @SuppressWarnings("unchecked")
    private static void write(Object value, StringBuilder builder, int indent) {
        if (value == null) {
            builder.append("null");
        } else if (value instanceof String text) {
            writeString(text, builder);
        } else if (value instanceof Number || value instanceof Boolean) {
            builder.append(value);
        } else if (value instanceof Map<?, ?> rawMap) {
            Map<String, Object> map = (Map<String, Object>) rawMap;
            builder.append("{");
            if (!map.isEmpty()) {
                builder.append("\n");
                int index = 0;
                for (Map.Entry<String, Object> entry : map.entrySet()) {
                    indent(builder, indent + 2);
                    writeString(entry.getKey(), builder);
                    builder.append(": ");
                    write(entry.getValue(), builder, indent + 2);
                    if (++index < map.size()) {
                        builder.append(",");
                    }
                    builder.append("\n");
                }
                indent(builder, indent);
            }
            builder.append("}");
        } else if (value instanceof Iterable<?> iterable) {
            builder.append("[");
            List<Object> values = new ArrayList<>();
            for (Object item : iterable) {
                values.add(item);
            }
            if (!values.isEmpty()) {
                builder.append("\n");
                for (int i = 0; i < values.size(); i++) {
                    indent(builder, indent + 2);
                    write(values.get(i), builder, indent + 2);
                    if (i + 1 < values.size()) {
                        builder.append(",");
                    }
                    builder.append("\n");
                }
                indent(builder, indent);
            }
            builder.append("]");
        } else {
            writeString(String.valueOf(value), builder);
        }
    }

    private static void indent(StringBuilder builder, int count) {
        builder.append(" ".repeat(Math.max(0, count)));
    }

    private static void writeString(String text, StringBuilder builder) {
        builder.append('"');
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            switch (ch) {
                case '"' -> builder.append("\\\"");
                case '\\' -> builder.append("\\\\");
                case '\n' -> builder.append("\\n");
                case '\r' -> builder.append("\\r");
                case '\t' -> builder.append("\\t");
                default -> {
                    if (ch < 0x20) {
                        builder.append(String.format("\\u%04x", (int) ch));
                    } else {
                        builder.append(ch);
                    }
                }
            }
        }
        builder.append('"');
    }

    private static final class Parser {
        private final String text;
        private int index;

        private Parser(String text) {
            this.text = text;
        }

        private Object parse() {
            skipWhitespace();
            if (index >= text.length()) {
                return new LinkedHashMap<String, Object>();
            }
            Object value = parseValue();
            skipWhitespace();
            return value;
        }

        private Object parseValue() {
            skipWhitespace();
            if (index >= text.length()) {
                throw error("Unexpected end of JSON");
            }
            char ch = text.charAt(index);
            if (ch == '"') {
                return parseString();
            }
            if (ch == '{') {
                return parseObject();
            }
            if (ch == '[') {
                return parseArray();
            }
            if (ch == 't' && match("true")) {
                return Boolean.TRUE;
            }
            if (ch == 'f' && match("false")) {
                return Boolean.FALSE;
            }
            if (ch == 'n' && match("null")) {
                return null;
            }
            if (ch == '-' || Character.isDigit(ch)) {
                return parseNumber();
            }
            throw error("Unexpected character '" + ch + "'");
        }

        private Map<String, Object> parseObject() {
            expect('{');
            Map<String, Object> map = new LinkedHashMap<>();
            skipWhitespace();
            if (peek('}')) {
                expect('}');
                return map;
            }
            while (true) {
                skipWhitespace();
                String key = parseString();
                skipWhitespace();
                expect(':');
                Object value = parseValue();
                map.put(key, value);
                skipWhitespace();
                if (peek('}')) {
                    expect('}');
                    break;
                }
                expect(',');
            }
            return map;
        }

        private List<Object> parseArray() {
            expect('[');
            List<Object> list = new ArrayList<>();
            skipWhitespace();
            if (peek(']')) {
                expect(']');
                return list;
            }
            while (true) {
                list.add(parseValue());
                skipWhitespace();
                if (peek(']')) {
                    expect(']');
                    break;
                }
                expect(',');
            }
            return list;
        }

        private String parseString() {
            expect('"');
            StringBuilder builder = new StringBuilder();
            while (index < text.length()) {
                char ch = text.charAt(index++);
                if (ch == '"') {
                    return builder.toString();
                }
                if (ch == '\\') {
                    if (index >= text.length()) {
                        throw error("Invalid escape");
                    }
                    char escape = text.charAt(index++);
                    switch (escape) {
                        case '"' -> builder.append('"');
                        case '\\' -> builder.append('\\');
                        case '/' -> builder.append('/');
                        case 'b' -> builder.append('\b');
                        case 'f' -> builder.append('\f');
                        case 'n' -> builder.append('\n');
                        case 'r' -> builder.append('\r');
                        case 't' -> builder.append('\t');
                        case 'u' -> builder.append(parseUnicode());
                        default -> throw error("Unsupported escape \\" + escape);
                    }
                } else {
                    builder.append(ch);
                }
            }
            throw error("Unterminated string");
        }

        private char parseUnicode() {
            if (index + 4 > text.length()) {
                throw error("Invalid unicode escape");
            }
            String hex = text.substring(index, index + 4);
            index += 4;
            return (char) Integer.parseInt(hex, 16);
        }

        private Number parseNumber() {
            int start = index;
            if (peek('-')) {
                index++;
            }
            while (index < text.length() && Character.isDigit(text.charAt(index))) {
                index++;
            }
            boolean decimal = false;
            if (peek('.')) {
                decimal = true;
                index++;
                while (index < text.length() && Character.isDigit(text.charAt(index))) {
                    index++;
                }
            }
            if (index < text.length() && (text.charAt(index) == 'e' || text.charAt(index) == 'E')) {
                decimal = true;
                index++;
                if (index < text.length() && (text.charAt(index) == '+' || text.charAt(index) == '-')) {
                    index++;
                }
                while (index < text.length() && Character.isDigit(text.charAt(index))) {
                    index++;
                }
            }
            String number = text.substring(start, index);
            return decimal ? Double.parseDouble(number) : Long.parseLong(number);
        }

        private void skipWhitespace() {
            while (index < text.length() && Character.isWhitespace(text.charAt(index))) {
                index++;
            }
        }

        private boolean match(String expected) {
            if (text.startsWith(expected, index)) {
                index += expected.length();
                return true;
            }
            return false;
        }

        private boolean peek(char expected) {
            return index < text.length() && text.charAt(index) == expected;
        }

        private void expect(char expected) {
            if (index >= text.length() || text.charAt(index) != expected) {
                throw error("Expected '" + expected + "'");
            }
            index++;
        }

        private IllegalArgumentException error(String message) {
            return new IllegalArgumentException(message + " at position " + index);
        }
    }
}
