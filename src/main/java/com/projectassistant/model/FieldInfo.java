package com.projectassistant.model;

import java.util.*;

/**
 * 字段信息
 */
public class FieldInfo {

    private String name;
    private String type;
    private String visibility;
    private boolean isStatic;
    private boolean isFinal;
    private List<String> annotations = new ArrayList<>();
    private boolean hasGetter;
    private boolean hasSetter;

    // === Getters & Setters ===

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getVisibility() { return visibility; }
    public void setVisibility(String visibility) { this.visibility = visibility; }

    public boolean isStatic() { return isStatic; }
    public void setStatic(boolean aStatic) { isStatic = aStatic; }

    public boolean isFinal() { return isFinal; }
    public void setFinal(boolean aFinal) { isFinal = aFinal; }

    public List<String> getAnnotations() { return annotations; }
    public void setAnnotations(List<String> annotations) { this.annotations = annotations; }

    public boolean isHasGetter() { return hasGetter; }
    public void setHasGetter(boolean hasGetter) { this.hasGetter = hasGetter; }

    public boolean isHasSetter() { return hasSetter; }
    public void setHasSetter(boolean hasSetter) { this.hasSetter = hasSetter; }

    @Override
    public String toString() {
        return String.format("%s %s %s", visibility, type, name);
    }
}
