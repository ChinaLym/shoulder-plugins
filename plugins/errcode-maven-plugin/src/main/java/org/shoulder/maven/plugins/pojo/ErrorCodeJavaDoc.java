package org.shoulder.maven.plugins.pojo;

import org.apache.commons.lang3.StringUtils;

/**
 * 错误码上的注释
 *
 * @author lym
 */
public class ErrorCodeJavaDoc {
    public String language = "zh_CN";
    public String errorCode = "";
    public String description = "";
    public String suggestion = "";

    public String getLanguage() {
        return language;
    }

    public String getDescription() {
        return description;
    }

    public String getSuggestion() {
        return suggestion;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setLanguage(String language) {
        this.language = StringUtils.isBlank(language) ? "zh_CN" : language;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setSuggestion(String suggestion) {
        this.suggestion = suggestion;
    }

    public ErrorCodeJavaDoc() {
    }

    public ErrorCodeJavaDoc(String language, String description, String suggestion) {
        this.language = language;
        this.description = description;
        this.suggestion = suggestion;
    }

    @Override
    public String toString() {
        return "{'description='" + description + '\'' +
                ", suggestion='" + suggestion + '\'' +
                '}';
    }
}