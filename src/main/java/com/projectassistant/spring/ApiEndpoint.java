package com.projectassistant.spring;

import java.util.*;

/**
 * API 端点 — 从 @RequestMapping 等注解解析出来的路由信息
 */
public class ApiEndpoint {
    private String httpMethod;
    private String path;
    private String controllerClass;
    private String methodName;
    private String returnType;
    private List<String> parameters = new ArrayList<>();
    private List<String> annotations = new ArrayList<>();
    private String description;
    private boolean secured;

    public String getHttpMethod() { return httpMethod; }
    public void setHttpMethod(String httpMethod) { this.httpMethod = httpMethod; }
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    public String getControllerClass() { return controllerClass; }
    public void setControllerClass(String c) { this.controllerClass = c; }
    public String getMethodName() { return methodName; }
    public void setMethodName(String m) { this.methodName = m; }
    public String getReturnType() { return returnType; }
    public void setReturnType(String r) { this.returnType = r; }
    public List<String> getParameters() { return parameters; }
    public List<String> getAnnotations() { return annotations; }
    public String getDescription() { return description; }
    public void setDescription(String d) { this.description = d; }
    public boolean isSecured() { return secured; }
    public void setSecured(boolean s) { this.secured = s; }

    @Override
    public String toString() {
        return httpMethod + " " + path + " -> " + controllerClass + "." + methodName + "()";
    }
}
