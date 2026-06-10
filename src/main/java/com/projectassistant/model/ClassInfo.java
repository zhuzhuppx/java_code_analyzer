package com.projectassistant.model;

import java.util.*;

/**
 * 类/接口/枚举/记录 的完整描述
 */
public class ClassInfo {

    /** 类全限定名 */
    private String fullyQualifiedName;

    /** 包名 */
    private String packageName;

    /** 简单类名 */
    private String simpleName;

    /** 类型: class / interface / enum / record / annotation */
    private String type;

    /** 可见性: public / private / protected / package-private */
    private String visibility;

    /** 文件路径 */
    private String sourceFilePath;

    /** 所属模块 */
    private String moduleName;

    /** 是否是抽象类 */
    private boolean isAbstract;

    /** 是否有 final 修饰 */
    private boolean isFinal;

    /** 是否是静态内部类 */
    private boolean isStatic;

    /** 继承的父类 */
    private String superClassName;

    /** 实现的接口 */
    private List<String> interfaces = new ArrayList<>();

    /** 泛型参数 */
    private List<String> typeParameters = new ArrayList<>();

    /** 所有字段 */
    private List<FieldInfo> fields = new ArrayList<>();

    /** 所有方法 */
    private List<MethodInfo> methods = new ArrayList<>();

    /** 所有注解 */
    private List<String> annotations = new ArrayList<>();

    /** 内部类 */
    private List<ClassInfo> innerClasses = new ArrayList<>();

    /** 行数统计 */
    private int lineCount;

    /** 代码行数（除去注释和空行） */
    private int codeLineCount;

    /** 复杂度（基于方法的 cyclomatic complexity 汇总） */
    private int totalComplexity;

    // === Getters & Setters ===

    public String getFullyQualifiedName() { return fullyQualifiedName; }
    public void setFullyQualifiedName(String fullyQualifiedName) { this.fullyQualifiedName = fullyQualifiedName; }

    public String getPackageName() { return packageName; }
    public void setPackageName(String packageName) { this.packageName = packageName; }

    public String getSimpleName() { return simpleName; }
    public void setSimpleName(String simpleName) { this.simpleName = simpleName; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getVisibility() { return visibility; }
    public void setVisibility(String visibility) { this.visibility = visibility; }

    public String getSourceFilePath() { return sourceFilePath; }
    public void setSourceFilePath(String sourceFilePath) { this.sourceFilePath = sourceFilePath; }

    public String getModuleName() { return moduleName; }
    public void setModuleName(String moduleName) { this.moduleName = moduleName; }

    public boolean isAbstract() { return isAbstract; }
    public void setAbstract(boolean anAbstract) { isAbstract = anAbstract; }

    public boolean isFinal() { return isFinal; }
    public void setFinal(boolean aFinal) { isFinal = aFinal; }

    public boolean isStatic() { return isStatic; }
    public void setStatic(boolean aStatic) { isStatic = aStatic; }

    public String getSuperClassName() { return superClassName; }
    public void setSuperClassName(String superClassName) { this.superClassName = superClassName; }

    public List<String> getInterfaces() { return interfaces; }
    public void setInterfaces(List<String> interfaces) { this.interfaces = interfaces; }

    public List<String> getTypeParameters() { return typeParameters; }
    public void setTypeParameters(List<String> typeParameters) { this.typeParameters = typeParameters; }

    public List<FieldInfo> getFields() { return fields; }
    public void setFields(List<FieldInfo> fields) { this.fields = fields; }

    public List<MethodInfo> getMethods() { return methods; }
    public void setMethods(List<MethodInfo> methods) { this.methods = methods; }

    public List<String> getAnnotations() { return annotations; }
    public void setAnnotations(List<String> annotations) { this.annotations = annotations; }

    public List<ClassInfo> getInnerClasses() { return innerClasses; }
    public void setInnerClasses(List<ClassInfo> innerClasses) { this.innerClasses = innerClasses; }

    public int getLineCount() { return lineCount; }
    public void setLineCount(int lineCount) { this.lineCount = lineCount; }

    public int getCodeLineCount() { return codeLineCount; }
    public void setCodeLineCount(int codeLineCount) { this.codeLineCount = codeLineCount; }

    public int getTotalComplexity() { return totalComplexity; }
    public void setTotalComplexity(int totalComplexity) { this.totalComplexity = totalComplexity; }

    @Override
    public String toString() {
        return String.format("%s %s%s%s", visibility == null ? "" : visibility,
                type == null ? "class" : type, fullyQualifiedName,
                superClassName != null ? " extends " + superClassName : "");
    }
}
