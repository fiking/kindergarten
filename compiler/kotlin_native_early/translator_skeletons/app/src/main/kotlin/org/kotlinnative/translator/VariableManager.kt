package org.kotlinnative.translator

import org.kotlinnative.translator.llvm.LLVMVariable
import java.util.Stack

class VariableManager {
    private var fileVariableCollectionTree = HashMap<String, Stack<Pair<LLVMVariable, Int>>>()
    private var globalVariableCollection = HashMap<String, LLVMVariable>()

    fun getLLVMValue(variableName : String) : LLVMVariable? {
        return fileVariableCollectionTree[variableName]?.peek()?.first ?: globalVariableCollection.get(variableName)
    }

    fun pullUpwardsLevel(level: Int) {
        fileVariableCollectionTree.forEach { s, stack -> if (!stack.empty() && stack.peek().second >= level) stack.pop() else Unit }
    }

    fun addVariable(name: String, variable: LLVMVariable, level: Int) {
        val stack = fileVariableCollectionTree.getOrDefault(name, Stack<Pair<LLVMVariable, Int>>())
        stack.push(Pair(variable, level))
        fileVariableCollectionTree.put(name, stack)
    }
}