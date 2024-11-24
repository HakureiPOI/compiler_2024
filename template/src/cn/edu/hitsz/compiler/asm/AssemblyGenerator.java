package cn.edu.hitsz.compiler.asm;

import cn.edu.hitsz.compiler.NotImplementedException;
import cn.edu.hitsz.compiler.ir.*;
import cn.edu.hitsz.compiler.utils.FileUtils;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;


/**
 * TODO: 实验四: 实现汇编生成
 * <br>
 * 在编译器的整体框架中, 代码生成可以称作后端, 而前面的所有工作都可称为前端.
 * <br>
 * 在前端完成的所有工作中, 都是与目标平台无关的, 而后端的工作为将前端生成的目标平台无关信息
 * 根据目标平台生成汇编代码. 前后端的分离有利于实现编译器面向不同平台生成汇编代码. 由于前后
 * 端分离的原因, 有可能前端生成的中间代码并不符合目标平台的汇编代码特点. 具体到本项目你可以
 * 尝试加入一个方法将中间代码调整为更接近 risc-v 汇编的形式, 这样会有利于汇编代码的生成.
 * <br>
 * 为保证实现上的自由, 框架中并未对后端提供基建, 在具体实现时可自行设计相关数据结构.
 *
 * @see AssemblyGenerator#run() 代码生成与寄存器分配
 */
public class AssemblyGenerator {

    /**
     * 存放经过预处理后的中间指令
     */
    private final List<Instruction> instructions = new ArrayList<>();

    /**
     * 变量与寄存器双向map
     */
    BMap<IRValue, Register> registerMap = new BMap<>();

    /**
     * 生成的汇编指令列表
     */
    private final List<String> asmInstructions = new ArrayList<>(List.of(".text"));


    /**
     * 寄存器
     */
    enum Register {
        t0, t1, t2, t3, t4, t5, t6
    }

    /**
     * 加载前端提供的中间代码
     * <br>
     * 视具体实现而定, 在加载中或加载后会生成一些在代码生成中会用到的信息. 如变量的引用
     * 信息. 这些信息可以通过简单的映射维护, 或者自行增加记录信息的数据结构.
     *
     * @param originInstructions 前端提供的中间代码
     */
    public void loadIR(List<Instruction> originInstructions) {
        // TODO: 读入前端提供的中间代码并生成所需要的信息
        // 遍历中间代码
        for (Instruction instruction : originInstructions) {
            InstructionKind instructionKind = instruction.getKind();

            if (instructionKind.isReturn()) {
                // 如果是RET指令，添加并退出处理
                instructions.add(instruction);
                break;
            } else if (instructionKind.isUnary()) {
                // 如果是单操作数指令，直接添加
                instructions.add(instruction);
            } else if (instructionKind.isBinary()) {
                processBinaryInstruction(instruction);
            }
        }
    }

    // 处理 Binary 类型的指令
    private void processBinaryInstruction(Instruction instruction) {
        IRValue lhs = instruction.getLHS();
        IRValue rhs = instruction.getRHS();
        IRVariable result = instruction.getResult();
        InstructionKind kind = instruction.getKind();

        if (lhs.isImmediate() && rhs.isImmediate()) {
            // 两个操作数都是立即数
            int immediateResult = calculateImmediateResult(kind, (IRImmediate) lhs, (IRImmediate) rhs);
            instructions.add(Instruction.createMov(result, IRImmediate.of(immediateResult)));
        } else if (lhs.isImmediate() && rhs.isIRVariable()) {
            // 左立即数和右变量
            handleLeftImmediate(kind, lhs, rhs, result);
        } else if (lhs.isIRVariable() && rhs.isImmediate()) {
            // 左变量和右立即数
            handleRightImmediate(kind, lhs, rhs, result);
        } else {
            // 两个操作数均为变量
            instructions.add(instruction);
        }
    }

    // 计算两个立即数的结果
    private int calculateImmediateResult(InstructionKind kind, IRImmediate lhs, IRImmediate rhs) {
        return switch (kind) {
            case ADD -> lhs.getValue() + rhs.getValue();
            case SUB -> lhs.getValue() - rhs.getValue();
            case MUL -> lhs.getValue() * rhs.getValue();
            default -> throw new IllegalArgumentException("Unsupported binary operation: " + kind);
        };
    }

    // 处理左操作数为立即数的指令
    private void handleLeftImmediate(InstructionKind kind, IRValue lhs, IRValue rhs, IRVariable result) {
        switch (kind) {
            case ADD -> instructions.add(Instruction.createAdd(result, rhs, lhs));
            case SUB, MUL -> {
                IRVariable temp = IRVariable.temp();
                instructions.add(Instruction.createMov(temp, lhs));
                if (kind == InstructionKind.SUB) {
                    instructions.add(Instruction.createSub(result, temp, rhs));
                } else {
                    instructions.add(Instruction.createMul(result, temp, rhs));
                }
            }
            default -> throw new IllegalArgumentException("Unsupported binary operation: " + kind);
        }
    }

    // 处理右操作数为立即数的指令
    private void handleRightImmediate(InstructionKind kind, IRValue lhs, IRValue rhs, IRVariable result) {
        switch (kind) {
            case ADD, SUB -> instructions.add(Instruction.createAdd(result, lhs, rhs));
            case MUL -> {
                IRVariable temp = IRVariable.temp();
                instructions.add(Instruction.createMov(temp, rhs));
                instructions.add(Instruction.createMul(result, lhs, temp));
            }
            default -> throw new IllegalArgumentException("Unsupported binary operation: " + kind);
        }
    }


    /**
     * 分配寄存器
     */
    public void VariableToRegister(IRValue operand, int currentIndex) {
        // 立即数无需分配寄存器
        if (operand.isImmediate()) {
            return;
        }

        // 如果变量已经有寄存器分配，直接返回
        if (registerMap.containsKey(operand)) {
            return;
        }

        // 尝试分配空闲寄存器
        for (Register register : Register.values()) {
            if (!registerMap.containsValue(register)) {
                registerMap.put(operand, register);
                return;
            }
        }

        // 无空闲寄存器，寻找不再使用的变量的寄存器
        Register reusedRegister = findReusableRegister(currentIndex);
        if (reusedRegister != null) {
            registerMap.replace(operand, reusedRegister);
        } else {
            // 无可用寄存器时，抛出异常
            throw new RuntimeException("No enough registers!");
        }
    }

    // 查找可重用的寄存器，从当前指令开始，查找不再使用的变量对应的寄存器。
    private Register findReusableRegister(int currentIndex) {
        Set<Register> unusedRegisters = new HashSet<>(Arrays.asList(Register.values()));

        // 遍历剩余指令，移除活跃变量的寄存器
        for (int i = currentIndex; i < instructions.size(); i++) {
            Instruction instruction = instructions.get(i);
            for (IRValue operand : instruction.getAllOperands()) {
                Register register = registerMap.getByKey(operand);
                unusedRegisters.remove(register);
            }
        }

        // 如果有未使用的寄存器，返回其中一个
        return unusedRegisters.isEmpty() ? null : unusedRegisters.iterator().next();
    }


    /**
     * 执行代码生成.
     * <br>
     * 根据理论课的做法, 在代码生成时同时完成寄存器分配的工作. 若你觉得这样的做法不好,
     * 也可以将寄存器分配和代码生成分开进行.
     * <br>
     * 提示: 寄存器分配中需要的信息较多, 关于全局的与代码生成过程无关的信息建议在代码生
     * 成前完成建立, 与代码生成的过程相关的信息可自行设计数据结构进行记录并动态维护.
     */
    public void run() {
        int currentIndex = 0;

        for (Instruction instruction : instructions) {
            InstructionKind kind = instruction.getKind();
            String asmCode = null;

            switch (kind) {
                case ADD, SUB, MUL -> asmCode = generateBinaryOperation(instruction, kind, currentIndex);
                case MOV -> asmCode = generateMovOperation(instruction, currentIndex);
                case RET -> {
                    asmCode = generateReturnOperation(instruction);
                    asmInstructions.add(asmCode); // 添加 RET 指令的汇编代码
                    return; // 直接退出，避免多余处理
                }
                default -> throw new IllegalArgumentException("Unsupported instruction kind: " + kind);
            }

            if (asmCode != null) {
                asmCode += String.format("\t\t# %s", instruction); // 添加注释
                asmInstructions.add(asmCode);
            }

            currentIndex++;
        }
    }


    // 生成二元操作的汇编代码
    private String generateBinaryOperation(Instruction instruction, InstructionKind kind, int currentIndex) {
        IRValue lhs = instruction.getLHS();
        IRValue rhs = instruction.getRHS();
        IRVariable result = instruction.getResult();

        // 为操作数和结果分配寄存器
        VariableToRegister(lhs, currentIndex);
        VariableToRegister(rhs, currentIndex);
        VariableToRegister(result, currentIndex);

        Register lhsReg = registerMap.getByKey(lhs);
        Register rhsReg = rhs.isImmediate() ? null : registerMap.getByKey(rhs);
        Register resultReg = registerMap.getByKey(result);

        // 根据操作类型生成汇编代码
        return switch (kind) {
            case ADD -> rhs.isImmediate()
                    ? String.format("\taddi %s, %s, %s", resultReg, lhsReg, rhs)
                    : String.format("\tadd %s, %s, %s", resultReg, lhsReg, rhsReg);
            case SUB -> rhs.isImmediate()
                    ? String.format("\tsubi %s, %s, %s", resultReg, lhsReg, rhs)
                    : String.format("\tsub %s, %s, %s", resultReg, lhsReg, rhsReg);
            case MUL -> String.format("\tmul %s, %s, %s", resultReg, lhsReg, rhsReg);
            default -> throw new IllegalArgumentException("Unsupported binary operation: " + kind);
        };
    }

    // 生成MOV指令的汇编代码
    private String generateMovOperation(Instruction instruction, int currentIndex) {
        IRValue source = instruction.getFrom();
        IRVariable result = instruction.getResult();

        // 为操作数和结果分配寄存器
        VariableToRegister(source, currentIndex);
        VariableToRegister(result, currentIndex);

        Register sourceReg = source.isImmediate() ? null : registerMap.getByKey(source);
        Register resultReg = registerMap.getByKey(result);

        // 根据是否为立即数生成汇编代码
        return source.isImmediate()
                ? String.format("\tli %s, %s", resultReg, source)
                : String.format("\tmv %s, %s", resultReg, sourceReg);
    }

    // 生成 RET 指令的汇编代码
    private String generateReturnOperation(Instruction instruction) {
        IRValue returnValue = instruction.getReturnValue();

        // 分配寄存器
        VariableToRegister(returnValue, instructions.size() - 1);
        Register returnValueReg = registerMap.getByKey(returnValue);

        // 生成RET指令并附加注释
        return String.format("\tmv a0, %s\t\t# %s", returnValueReg, instruction);
    }



    /**
     * 输出汇编代码到文件
     *
     * @param path 输出文件路径
     */
    public void dump(String path) {
        // TODO: 输出汇编代码到文件
        FileUtils.writeLines(path, asmInstructions);
    }
}

