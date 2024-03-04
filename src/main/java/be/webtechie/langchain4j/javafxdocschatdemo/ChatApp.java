package be.webtechie.langchain4j.javafxdocschatdemo;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ChatApp extends Application {

    private static final Logger LOGGER = LogManager.getLogger(ChatApp.class);

    private static final double WIDTH = 1600;
    private static final double HEIGHT = 1200;
    private static final ObservableList<SearchAction> data = FXCollections.observableArrayList();
    private static final DocsAnswerService docsAnswerService = new DocsAnswerService();
    private final TableView<SearchAction> table = new TableView<>();
    private final TextArea relatedLinks = new TextArea();
    private final TextArea lastAnswer = new TextArea();

    @Override
    public void start(Stage stage) {
        LOGGER.info("Starting...");

        var holder = new VBox();
        holder.setStyle("-fx-padding: 15px;");

        Label label = new Label("What is your question?");
        label.setStyle("-fx-font-size: 25px;");

        TextField input = new TextField();
        input.setOnAction(e -> doSearch(input.getText()));
        input.setMinWidth(500);
        input.setStyle("-fx-font-size: 16px");

        Button search = new Button("Search");
        search.setOnAction(e -> doSearch(input.getText()));
        search.setStyle("-fx-font-size: 16px");

        var inputHolder = new HBox(input, search);
        inputHolder.setStyle("-fx-padding: 0 0 25px 0");
        inputHolder.setSpacing(10);

        TableColumn<SearchAction, String> timestamp = new TableColumn<>("Timestamp");
        timestamp.setCellValueFactory(cellData -> cellData.getValue().getTimestampProperty());
        timestamp.setMinWidth(250);
        TableColumn<SearchAction, String> question = new TableColumn<>("Question");
        question.setCellValueFactory(cellData -> cellData.getValue().getQuestionProperty());
        question.setMinWidth(250);
        TableColumn<SearchAction, String> answer = new TableColumn<>("Answer");
        answer.setCellValueFactory(cellData -> cellData.getValue().getAnswerProperty());
        answer.setMinWidth(300);
        TableColumn<SearchAction, Boolean> finished = new TableColumn<>("Finished");
        finished.setCellValueFactory(cellData -> cellData.getValue().getFinishedProperty());
        finished.setMinWidth(50);

        table.getColumns().addAll(timestamp, question, answer, finished);
        table.setItems(data);
        table.setStyle("-fx-padding: 0 25px 0 0");
        table.setPrefHeight(HEIGHT);
        table.setMinWidth(900);

        relatedLinks.setWrapText(true);
        relatedLinks.setMinHeight(150);
        relatedLinks.setStyle("-fx-font-size: 12px");

        lastAnswer.setWrapText(true);
        lastAnswer.setPrefHeight(HEIGHT);
        lastAnswer.setStyle("-fx-font-size: 16px");

        var labelLinks = new Label("Related links");
        labelLinks.setStyle("-fx-font-size: 25px");

        var labelAnswer = new Label("Answer");
        labelAnswer.setStyle("-fx-font-size: 25px");

        holder.getChildren().addAll(label, inputHolder,
                new HBox(table, new VBox(labelLinks, relatedLinks, labelAnswer, lastAnswer)));

        Scene scene = new Scene(holder, WIDTH, HEIGHT);

        stage.setTitle("Docs Chat");
        stage.setScene(scene);
        stage.centerOnScreen();
        stage.show();

        stage.setOnCloseRequest(event -> {
            LOGGER.info("Closing application...");
            Platform.exit();
            System.exit(0);
        });

        data.add(new SearchAction("Application started", true));

        var initAction = new SearchAction("Initializing search engine, please stand by...");
        data.add(initAction);
        relatedLinks.textProperty().bind(initAction.getRelatedLinksProperty());
        lastAnswer.textProperty().bind(initAction.getAnswerProperty());
        new Thread(() -> docsAnswerService.init(initAction)).start();
    }

    private void doSearch(String question) {
        if (question.isEmpty()) {
            return;
        }

        var searchAction = new SearchAction(question);
        data.add(searchAction);
        relatedLinks.textProperty().bind(searchAction.getRelatedLinksProperty());
        lastAnswer.textProperty().bind(searchAction.getAnswerProperty());
        new Thread(() -> docsAnswerService.ask(searchAction)).start();
    }
}