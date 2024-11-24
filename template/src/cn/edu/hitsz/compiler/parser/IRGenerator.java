package cn.edu.hitsz.compiler.parser;

import cn.edu.hitsz.compiler.ir.IRImmediate;
import cn.edu.hitsz.compiler.ir.IRVariable;
import cn.edu.hitsz.compiler.ir.Instruction;
import cn.edu.hitsz.compiler.lexer.Token;
import cn.edu.hitsz.compiler.parser.table.Production;
import cn.edu.hitsz.compiler.parser.table.Status;
import cn.edu.hitsz.compiler.symtab.SymbolTable;
import cn.edu.hitsz.compiler.utils.FileUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

// TODO: 实验三: 实现 IR 生成
public class IRGenerator implements ActionObserver {

    public SymbolTable symbolTable;
    private final Stack<Symbol> tokenStack = new Stack<>();
    private final List<Instruction> irList = new ArrayList<>();

    @Override
    public void whenShift(Status currentStatus, Token currentToken) {
        // TODO
        Symbol curSymbol = new Symbol(currentToken);
        if (currentToken.getText().matches("^[0-9]+$")) {
            curSymbol.setValue(IRImmediate.of(Integer.parseInt(currentToken.getText())));
        } else {
            curSymbol.setValue(IRVariable.named(currentToken.getText()));
        }
        tokenStack.push(curSymbol);
    }

    @Override
    public void whenReduce(Status currentStatus, Production production) {
        // TODO
        // Reduce 时生成相应的 IR 指令
        Symbol curNonTerminal = new Symbol(production.head());
        IRVariable valueTemp;
        Symbol lhs, rhs;

        switch (production.index()) {
            case 6 -> { // S -> id = E
                rhs = tokenStack.pop();
                tokenStack.pop(); // 弹出 "="
                lhs = tokenStack.pop();
                valueTemp = (IRVariable) lhs.getValue();
                irList.add(Instruction.createMov(valueTemp, rhs.getValue()));
            }
            case 7 -> { // S -> return E
                rhs = tokenStack.pop();
                tokenStack.pop(); // 弹出 "return"
                irList.add(Instruction.createRet(rhs.getValue()));
            }
            case 8 -> { // E -> E + A
                rhs = tokenStack.pop();
                tokenStack.pop(); // 弹出 "+"
                lhs = tokenStack.pop();
                valueTemp = IRVariable.temp();
                irList.add(Instruction.createAdd(valueTemp, lhs.getValue(), rhs.getValue()));
                curNonTerminal.setValue(valueTemp);
            }
            case 9 -> { // E -> E - A
                rhs = tokenStack.pop();
                tokenStack.pop(); // 弹出 "-"
                lhs = tokenStack.pop();
                valueTemp = IRVariable.temp();
                irList.add(Instruction.createSub(valueTemp, lhs.getValue(), rhs.getValue()));
                curNonTerminal.setValue(valueTemp);
            }
            case 11 -> { // A -> A * B
                rhs = tokenStack.pop();
                tokenStack.pop(); // 弹出 "*"
                lhs = tokenStack.pop();
                valueTemp = IRVariable.temp();
                irList.add(Instruction.createMul(valueTemp, lhs.getValue(), rhs.getValue()));
                curNonTerminal.setValue(valueTemp);
            }
            case 10, 12, 14 -> { // E -> A, A -> B, B -> id
                curNonTerminal.setValue(tokenStack.pop().getValue());
            }
            case 13 -> { // B -> ( E )
                tokenStack.pop(); // 弹出 ")"
                rhs = tokenStack.pop(); // 弹出 E
                tokenStack.pop(); // 弹出 "("
                curNonTerminal.setValue(rhs.getValue());
            }
            case 15 -> { // B -> IntConst
                rhs = tokenStack.pop();
                curNonTerminal.setValue(rhs.getValue());
            }
            default -> {
                for (int i = 0; i < production.body().size(); i++) {
                    tokenStack.pop();
                }
            }
        }
        tokenStack.push(curNonTerminal);
    }


    @Override
    public void whenAccept(Status currentStatus) {
        // TODO
        // Accept 时不需要进行动作
    }

    @Override
    public void setSymbolTable(SymbolTable table) {
        // TODO
        this.symbolTable = table;
    }

    public List<Instruction> getIR() {
        // TODO
        return irList;
    }

    public void dumpIR(String path) {
        FileUtils.writeLines(path, getIR().stream().map(Instruction::toString).toList());
    }
}

