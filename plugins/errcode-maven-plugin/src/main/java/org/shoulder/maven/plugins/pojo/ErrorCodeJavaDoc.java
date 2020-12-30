package org.shoulder.maven.plugins.pojo;

/**
 * 错误码上的注释
 *
 * @author lym
 */
public class ErrorCodeJavaDoc {
    public String language = "";
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

    public void setLanguage(String language) {
        if (language.length() == 0) {
            language = "zh_CN";
        }
        this.language = language;
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