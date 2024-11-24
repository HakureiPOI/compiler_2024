package cn.edu.hitsz.compiler.lexer;

import cn.edu.hitsz.compiler.NotImplementedException;
import cn.edu.hitsz.compiler.symtab.SymbolTable;
import cn.edu.hitsz.compiler.utils.FileUtils;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.StreamSupport;

/**
 * TODO: 实验一: 实现词法分析
 * <br>
 * 你可能需要参考的框架代码如下:
 *
 * @see Token 词法单元的实现
 * @see TokenKind 词法单元类型的实现
 */
public class LexicalAnalyzer {
    private final SymbolTable symbolTable;
    public String sourceString = "";
    public List<Token> tokenList = new ArrayList<>();
    public LexicalAnalyzer(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
    }


    /**
     * 从给予的路径中读取并加载文件内容
     *
     * @param path 路径
     */
    public void loadFile(String path) {
        // TODO: 词法分析前的缓冲区实现
        // 可自由实现各类缓冲区
        // 或直接采用完整读入方法
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String textLine = br.readLine();
            while (textLine != null) {
                sourceString += textLine;
                textLine = br.readLine();
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 执行词法分析, 准备好用于返回的 token 列表 <br>
     * 需要维护实验一所需的符号表条目, 而得在语法分析中才能确定的符号表条目的成员可以先设置为 null
     */
    public void run() {
        // 将源字符串转换为字符数组
        char[] word = sourceString.toCharArray();
        int i = 0;

        while (i < sourceString.length()) {
            char ch = word[i];

            // 跳过空白字符
            if (isWhitespace(ch)) {
                i++;
                continue;
            }

            // 判断是否为单字符标点符号
            if (isSinglePunctuation(ch)) {
                if (ch == ';') {
                    tokenList.add(Token.simple("Semicolon"));
                } else {
                    tokenList.add(Token.simple(String.valueOf(ch)));
                }
                i++;
                continue;
            }

            // 判断是否为字母开头的标识符
            if (Character.isLetter(ch)) {
                i = processLetter(i, word);
                continue;
            }

            // 判断是否为数字
            if (Character.isDigit(ch)) {
                i = processDigit(i, word);
                continue;
            }

            // 处理非法字符
            i++;
        }

        tokenList.add(Token.eof());
    }

    // 检查空白字符
    private boolean isWhitespace(char ch) {
        return ch == ' ' || ch == '\r' || ch == '\n' || ch == '\t';
    }

    // 检查单字符标点符号
    private boolean isSinglePunctuation(char ch) {
        Set<Character> punctuations = Set.of(',', ';', '=', '+', '-', '*', '/', '(', ')');
        return punctuations.contains(ch);
    }

    // 处理字母标识符
    private int processLetter(int i, char[] word) {
        int start = i;
        while (i < word.length && Character.isLetter(word[i])) {
            i++;
        }
        String key = sourceString.substring(start, i);

        if (TokenKind.isAllowed(key)) {
            tokenList.add(Token.simple(key));
        } else {
            tokenList.add(Token.normal("id", key));
            if (!symbolTable.has(key)) {
                symbolTable.add(key);
            }
        }
        return i;
    }

    // 处理数字常量
    private int processDigit(int i, char[] word) {
        int start = i;
        while (i < word.length && Character.isDigit(word[i])) {
            i++;
        }
        String digit = sourceString.substring(start, i);
        tokenList.add(Token.normal("IntConst", digit));
        return i;
    }


    /**
     * 获得词法分析的结果, 保证在调用了 run 方法之后调用
     *
     * @return Token 列表
     */
    public Iterable<Token> getTokens() {
        // TODO: 从词法分析过程中获取 Token 列表
        // 词法分析过程可以使用 Stream 或 Iterator 实现按需分析
        // 亦可以直接分析完整个文件
        // 总之实现过程能转化为一列表即可
        return tokenList;
    }

    public void dumpTokens(String path) {
        FileUtils.writeLines(
                path,
                StreamSupport.stream(getTokens().spliterator(), false).map(Token::toString).toList()
        );
    }
}
