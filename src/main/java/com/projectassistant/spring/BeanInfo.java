package com.projectassistant.spring;

import java.util.*;

/**
 * Bean 信息 — 描述一个 Spring Bean 及其依赖关系
 */
public class BeanInfo {
    private String className;          // 类全限定名
    private String simpleName;         // 简单类名
    private String beanName;           // @Bean/@Service("xx") 指定的名称，或默认
    private String role;               // controller / service / repository / component / configuration
    private String scope;              // singleton / prototype / request / session
    private boolean isPrimary;         // @Primary
    private boolean isLazy;            // @Lazy
    private final List<InjectionPoint> injections = new ArrayList<>();  // 注入的依赖
    private final List<String> injectedBy = new ArrayList<>();          // 被谁注入（反向）

    // ============ 构造 & 工厂 ============

    public BeanInfo(String className, String simpleName, String role) {
        this.className = className;
        this.simpleName = simpleName;
        this.role = role;
        this.beanName = simpleName;
        // 首字母小写作为默认 bean name
        if (beanName != null && beanName.length() > 0) {
            this.beanName = Character.toLowerCase(beanName.charAt(0)) + beanName.substring(1);
        }
    }

    // ============ 内部类：注入点 ============

    public static class InjectionPoint {
        private String fieldName;        // 字段名
        private String targetType;       // 注入的目标类型
        private String targetBeanName;   // 解析到的 bean 名称
        private String injectionType;    // field / constructor / setter
        private String annotation;       // Autowired / Resource / Inject
        private String qualifier;        // @Qualifier("xxx")

        public InjectionPoint(String fieldName, String targetType, String injectionType, String annotation) {
            this.fieldName = fieldName;
            this.targetType = targetType;
            this.injectionType = injectionType;
            this.annotation = annotation;
        }

        public String getFieldName() { return fieldName; }
        public String getTargetType() { return targetType; }
        public String getTargetBeanName() { return targetBeanName; }
        public void setTargetBeanName(String name) { this.targetBeanName = name; }
        public String getInjectionType() { return injectionType; }
        public String getAnnotation() { return annotation; }
        public String getQualifier() { return qualifier; }
        public void setQualifier(String q) { this.qualifier = q; }
    }

    // ============ getter/setter ============

    public String getClassName() { return className; }
    public String getSimpleName() { return simpleName; }
    public String getBeanName() { return beanName; }
    public void setBeanName(String beanName) { this.beanName = beanName; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }
    public boolean isPrimary() { return isPrimary; }
    public void setPrimary(boolean primary) { isPrimary = primary; }
    public boolean isLazy() { return isLazy; }
    public void setLazy(boolean lazy) { isLazy = lazy; }
    public List<InjectionPoint> getInjections() { return injections; }
    public List<String> getInjectedBy() { return injectedBy; }

    public void addInjection(InjectionPoint ip) { this.injections.add(ip); }
    public void addInjectedBy(String bean) { if (!injectedBy.contains(bean)) injectedBy.add(bean); }
}
