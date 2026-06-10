package com.projectassistant.model;

import java.util.*;

/**
 * 方法信息
 */
public class MethodInfo {

    private String name;
    private String returnType;
    private String visibility;
    private boolean isAbstract;
    private boolean isStatic;
    private boolean isFinal;
    private boolean isSynchronized;
    private boolean isConstructor;
    private boolean isOverride;
    private List<String> parameters = new ArrayList<>();
    private List<String> parameterNames = new ArrayList<>();
    private List<String> exceptions = new ArrayList<>();
    private List<String> annotations = new ArrayList<>();
    private int lineCount;
    private int cyclomaticComplexity;
    private String signature;

    // 方法体内调用的其他方法
    private List<String> calledMethods = new ArrayList<>();

    // 方法体内访问的字段
    private List<String> accessedFields = new ArrayList<>();

    // === Getters & Setters ===

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getReturnType() { return returnType; }
    public void setReturnType(String returnType) { this.returnType = returnType; }

    public String getVisibility() { return visibility; }
    public void setVisibility(String visibility) { this.visibility = visibility; }

    public boolean isAbstract() { return isAbstract; }
    public void setAbstract(boolean anAbstract) { isAbstract = anAbstract; }

    public boolean isStatic() { return isStatic; }
    public void setStatic(boolean aStatic) { isStatic = aStatic; }

    public boolean isFinal() { return isFinal; }
    public void setFinal(boolean aFinal) { isFinal = aFinal; }

    public boolean isSynchronized() { return isSynchronized; }
    public void setSynchronized(boolean aSynchronized) { isSynchronized = aSynchronized; }

    public boolean isConstructor() { return isConstructor; }
    public void setConstructor(boolean constructor) { isConstructor = constructor; }

    public boolean isOverride() { return isOverride; }
    public void setOverride(boolean override) { isOverride = override; }

    public List<String> getParameters() { return parameters; }
    public void setParameters(List<String> parameters) { this.parameters = parameters; }

    public List<String> getParameterNames() { return parameterNames; }
    public void setParameterNames(List<String> parameterNames) { this.parameterNames = parameterNames; }

    public List<String> getExceptions() { return exceptions; }
    public void setExceptions(List<String> exceptions) { this.exceptions = exceptions; }

    public List<String> getAnnotations() { return annotations; }
    public void setAnnotations(List<String> annotations) { this.annotations = annotations; }

    public int getLineCount() { return lineCount; }
    public void setLineCount(int lineCount) { this.lineCount = lineCount; }

    public int getCyclomaticComplexity() { return cyclomaticComplexity; }
    public void setCyclomaticComplexity(int cyclomaticComplexity) { this.cyclomaticComplexity = cyclomaticComplexity; }

    public String getSignature() { return signature; }
    public void setSignature(String signature) { this.signature = signature; }

    public List<String> getCalledMethods() { return calledMethods; }
    public void setCalledMethods(List<String> calledMethods) { this.calledMethods = calledMethods; }

    public List<String> getAccessedFields() { return accessedFields; }
    public void setAccessedFields(List<String> accessedFields) { this.accessedFields = accessedFields; }

    @Override
    public String toString() {
        return String.format("%s %s(%s)", returnType, name, String.join(", ", parameters));
    }
}
