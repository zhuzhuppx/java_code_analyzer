package com.projectassistant.spring;

import java.util.*;

/**
 * API 端点 — 从 @RequestMapping 等注解解析出来的路由信息
 *
 * 增强版：支持完整文档输出，含请求参数、请求体、响应类型、安全约束等
 */
public class ApiEndpoint {
    private String httpMethod;
    private String path;
    private String controllerClass;
    private String methodName;
    private String returnType;
    /** 方法参数原始列表（类型 参数名） */
    private List<String> parameters = new ArrayList<>();
    private List<String> annotations = new ArrayList<>();
    private String description;
    private boolean secured;

    // 以下为文档增强字段
    /** @RequestParam 参数 (name, type, required, defaultValue) */
    private List<ApiParam> requestParams = new ArrayList<>();
    /** @PathVariable 参数 (name, type) */
    private List<ApiParam> pathVariables = new ArrayList<>();
    /** @RequestBody 参数类型 */
    private String requestBodyType;
    /** @RequestHeader 参数 (name, type, required) */
    private List<ApiParam> requestHeaders = new ArrayList<>();
    /** @RequestMapping consumes */
    private String consumes;
    /** @RequestMapping produces */
    private String produces;
    /** 简短摘要（优先 @ApiOperation，其次方法名语义化） */
    private String summary;
    /** 是否废弃 */
    private boolean deprecated;

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

    public List<ApiParam> getRequestParams() { return requestParams; }
    public List<ApiParam> getPathVariables() { return pathVariables; }
    public String getRequestBodyType() { return requestBodyType; }
    public void setRequestBodyType(String t) { this.requestBodyType = t; }
    public List<ApiParam> getRequestHeaders() { return requestHeaders; }
    public String getConsumes() { return consumes; }
    public void setConsumes(String c) { this.consumes = c; }
    public String getProduces() { return produces; }
    public void setProduces(String p) { this.produces = p; }
    public String getSummary() { return summary; }
    public void setSummary(String s) { this.summary = s; }
    public boolean isDeprecated() { return deprecated; }
    public void setDeprecated(boolean d) { this.deprecated = d; }

    /**
     * 获取 API 的完整签名（含参数和返回类型）
     */
    public String getSignature() {
        StringBuilder sb = new StringBuilder();
        sb.append(httpMethod).append(" ").append(path);
        if (!requestParams.isEmpty() || !pathVariables.isEmpty() || requestBodyType != null) {
            sb.append(" (");
            List<String> parts = new ArrayList<>();
            for (ApiParam pv : pathVariables) {
                parts.add("{" + pv.getName() + ": " + pv.getType() + "}");
            }
            for (ApiParam rp : requestParams) {
                parts.add(rp.getName() + (rp.isRequired() ? "" : "?") + ": " + rp.getType());
            }
            if (requestBodyType != null) {
                parts.add("body: " + requestBodyType);
            }
            sb.append(String.join(", ", parts));
            sb.append(")");
        }
        sb.append(" -> ").append(returnType != null ? returnType : "void");
        return sb.toString();
    }

    @Override
    public String toString() {
        return httpMethod + " " + path + " -> " + controllerClass + "." + methodName + "()";
    }

    /**
     * API 参数 — 含类型、必填、默认值、描述
     */
    public static class ApiParam {
        private String name;
        private String type;
        private boolean required;
        private String defaultValue;
        private String description;

        public ApiParam() {}

        public ApiParam(String name, String type) {
            this.name = name;
            this.type = type;
            this.required = false;
        }

        public ApiParam(String name, String type, boolean required) {
            this.name = name;
            this.type = type;
            this.required = required;
        }

        public String getName() { return name; }
        public void setName(String n) { this.name = n; }
        public String getType() { return type; }
        public void setType(String t) { this.type = t; }
        public boolean isRequired() { return required; }
        public void setRequired(boolean r) { this.required = r; }
        public String getDefaultValue() { return defaultValue; }
        public void setDefaultValue(String d) { this.defaultValue = d; }
        public String getDescription() { return description; }
        public void setDescription(String d) { this.description = d; }

        @Override
        public String toString() {
            return (required ? "" : "?") + name + ": " + type
                    + (defaultValue != null ? " = " + defaultValue : "");
        }
    }
}
