package be.webtechie.langchain4j.javafxdocschatdemo;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.time.LocalDateTime;

public class SearchAction {

    private final StringProperty timestamp;
    private final StringProperty question;
    private final StringProperty relatedLinks;
    private final StringProperty answer;
    private final BooleanProperty finished;

    public SearchAction(String question) {
        this(question, false);
    }

    public SearchAction(String question, Boolean finished) {
        this.timestamp = new SimpleStringProperty(LocalDateTime.now().toString());
        this.question = new SimpleStringProperty(question);
        this.relatedLinks = new SimpleStringProperty("");
        this.answer = new SimpleStringProperty("");
        this.finished = new SimpleBooleanProperty(finished);
    }

    public StringProperty getTimestampProperty() {
        return timestamp;
    }

    public String getQuestion() {
        return question.getValue();
    }

    public StringProperty getQuestionProperty() {
        return question;
    }

    public StringProperty getRelatedLinksProperty() {
        return relatedLinks;
    }

    public void appendRelatedLink(String link) {
        this.relatedLinks.set(this.relatedLinks.getValue() + System.lineSeparator() + link);
    }

    public String getAnswer() {
        return answer.getValue();
    }

    public StringProperty getAnswerProperty() {
        return answer;
    }

    public BooleanProperty getFinishedProperty() {
        return finished;
    }

    public void appendAnswer(String answer) {
        appendAnswer(answer, false);
    }

    public void appendAnswer(String answer, boolean finished) {
        Platform.runLater(() -> {
            this.answer.set(this.answer.getValue() + answer);
            if (finished) {
                this.finished.set(true);
            }
        });
    }
}