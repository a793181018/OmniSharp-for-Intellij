package com.omnisharp.intellij.projectstructure.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 解决方案文件标记器，负责将.sln文件内容转换为标记序列
 */
public class SolutionFileTokenizer {

    /**
     * 标记类型枚举
     */
    public enum TokenType {
        SECTION_START,
        SECTION_END,
        KEYWORD,
        IDENTIFIER,
        STRING,
        NUMBER,
        GUID,
        EQUAL,
        COMMA,
        SEMICOLON,
        LPAREN,
        RPAREN,
        COMMENT,
        WHITESPACE,
        NEWLINE,
        EOF
    }

    /**
     * 表示解决方案文件中的单个标记
     */
    public static class Token {
        private final TokenType type;
        private final String value;
        private final int line;
        private final int column;

        public Token(TokenType type, String value, int line, int column) {
            this.type = type;
            this.value = value;
            this.line = line;
            this.column = column;
        }

        public TokenType getType() {
            return type;
        }

        public String getValue() {
            return value;
        }

        public int getLine() {
            return line;
        }

        public int getColumn() {
            return column;
        }

        @Override
        public String toString() {
            return "Token{" +
                    "type=" + type +
                    ", value='" + value + '\'' +
                    ", line=" + line +
                    ", column=" + column +
                    '}';
        }
    }

    // GUID正则表达式
    private static final Pattern GUID_PATTERN = Pattern.compile("\\{[0-9A-Fa-f]{8}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{12}\\}");
    // 关键字列表
    private static final List<String> KEYWORDS = List.of(
            "Microsoft Visual Studio Solution File",
            "Project",
            "EndProject",
            "Global",
            "EndGlobal",
            "GlobalSection",
            "EndGlobalSection",
            "SolutionConfigurationPlatforms",
            "ProjectConfigurationPlatforms",
            "Project",
            "Solution",
            "Debug",
            "Release",
            "Any CPU",
            "x86",
            "x64"
    );

    /**
     * 将解决方案文件转换为标记序列
     * @param solutionFilePath 解决方案文件路径
     * @return 标记列表
     * @throws IOException 如果文件读取失败
     */
    public List<Token> tokenize(Path solutionFilePath) throws IOException {
        List<Token> tokens = new ArrayList<>();

        try (BufferedReader reader = Files.newBufferedReader(solutionFilePath)) {
            String line;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                tokenizeLine(line, lineNumber, tokens);
                // 添加换行符标记
                tokens.add(new Token(TokenType.NEWLINE, "\n", lineNumber, line.length() + 1));
            }
        }

        // 添加文件结束标记
        tokens.add(new Token(TokenType.EOF, "", 0, 0));
        return tokens;
    }

    /**
     * 标记化一行文本
     */
    private void tokenizeLine(String line, int lineNumber, List<Token> tokens) {
        int position = 0;
        int length = line.length();

        while (position < length) {
            char current = line.charAt(position);

            if (Character.isWhitespace(current)) {
                // 处理空白字符
                int start = position;
                while (position < length && Character.isWhitespace(line.charAt(position))) {
                    position++;
                }
                String whitespace = line.substring(start, position);
                tokens.add(new Token(TokenType.WHITESPACE, whitespace, lineNumber, start + 1));
            } else if (current == '\'') {
                // 处理字符串
                int start = position;
                position++;
                StringBuilder stringBuilder = new StringBuilder("\'");
                
                while (position < length && line.charAt(position) != '\'') {
                    if (line.charAt(position) == '\\' && position + 1 < length) {
                        stringBuilder.append(line.charAt(position)); // 添加转义字符
                        position++;
                    }
                    stringBuilder.append(line.charAt(position));
                    position++;
                }
                
                if (position < length) {
                    stringBuilder.append("\'");
                    position++;
                }
                
                tokens.add(new Token(TokenType.STRING, stringBuilder.toString(), lineNumber, start + 1));
            } else if (current == '#') {
                // 处理注释
                int start = position;
                position = length; // 注释到行尾
                String comment = line.substring(start);
                tokens.add(new Token(TokenType.COMMENT, comment, lineNumber, start + 1));
            } else if (current == '=') {
                tokens.add(new Token(TokenType.EQUAL, "=", lineNumber, position + 1));
                position++;
            } else if (current == ',') {
                tokens.add(new Token(TokenType.COMMA, ",", lineNumber, position + 1));
                position++;
            } else if (current == ';') {
                tokens.add(new Token(TokenType.SEMICOLON, ";", lineNumber, position + 1));
                position++;
            } else if (current == '(') {
                tokens.add(new Token(TokenType.LPAREN, "(", lineNumber, position + 1));
                position++;
            } else if (current == ')') {
                tokens.add(new Token(TokenType.RPAREN, ")", lineNumber, position + 1));
                position++;
            } else if (current == '{') {
                // 检查是否是GUID
                String remainingLine = line.substring(position);
                Matcher guidMatcher = GUID_PATTERN.matcher(remainingLine);
                if (guidMatcher.find() && guidMatcher.start() == 0) {
                    String guid = guidMatcher.group();
                    tokens.add(new Token(TokenType.GUID, guid, lineNumber, position + 1));
                    position += guid.length();
                } else {
                    tokens.add(new Token(TokenType.SECTION_START, "{", lineNumber, position + 1));
                    position++;
                }
            } else if (current == '}') {
                tokens.add(new Token(TokenType.SECTION_END, "}", lineNumber, position + 1));
                position++;
            } else if (Character.isDigit(current)) {
                // 处理数字
                int start = position;
                while (position < length && Character.isDigit(line.charAt(position))) {
                    position++;
                }
                String number = line.substring(start, position);
                tokens.add(new Token(TokenType.NUMBER, number, lineNumber, start + 1));
            } else if (Character.isLetter(current) || current == '.') {
                // 处理标识符或关键字
                int start = position;
                while (position < length && (Character.isLetterOrDigit(line.charAt(position)) || 
                       line.charAt(position) == '.' || line.charAt(position) == ' ' || 
                       line.charAt(position) == '-')) {
                    position++;
                }
                String identifier = line.substring(start, position).trim();
                
                if (isKeyword(identifier)) {
                    tokens.add(new Token(TokenType.KEYWORD, identifier, lineNumber, start + 1));
                } else {
                    tokens.add(new Token(TokenType.IDENTIFIER, identifier, lineNumber, start + 1));
                }
            } else {
                // 未知字符，作为标识符处理
                int start = position;
                while (position < length && !Character.isWhitespace(line.charAt(position)) &&
                       !isSpecialCharacter(line.charAt(position))) {
                    position++;
                }
                String unknown = line.substring(start, position);
                tokens.add(new Token(TokenType.IDENTIFIER, unknown, lineNumber, start + 1));
            }
        }
    }

    /**
     * 检查字符是否为特殊字符
     */
    private boolean isSpecialCharacter(char c) {
        return c == '=' || c == ',' || c == ';' || c == '(' || c == ')' || c == '{' || c == '}' || c == '\'' || c == '#';
    }

    /**
     * 检查字符串是否为关键字
     */
    private boolean isKeyword(String text) {
        return KEYWORDS.contains(text);
    }

    /**
     * 跳过空白字符和注释标记
     */
    public List<Token> skipWhitespaceAndComments(List<Token> tokens, int currentIndex) {
        List<Token> result = new ArrayList<>();
        for (int i = currentIndex; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            if (token.getType() != TokenType.WHITESPACE && 
                token.getType() != TokenType.COMMENT &&
                token.getType() != TokenType.NEWLINE) {
                result.add(token);
            }
        }
        return result;
    }
}