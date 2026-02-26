package org.balinhui.fpa;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.balinhui.fpa.info.SystemInfo;
import org.balinhui.fpa.nativeapis.DwmAPI;
import org.balinhui.fpa.ui.Buttons;
import org.balinhui.fpa.util.Config;

public class SettingScreen {
    private static final Logger logger = LogManager.getLogger(SettingScreen.class);

    public static Stage settingWindow;

    public static Button apply;
    public static Button cancel;
    public static Button ok;

    public static ChoiceBox<Config.AudioAPI> audioAPIChoice;
    public static ChoiceBox<Config.Location> locationChoice;

    /**
     * 储存设置中的值，如果点击确定、应用，就将这些值储存到本地
     */
    private static Config.AudioAPI audioAPI;
    private static Config.Location location;

    public static void init(Stage mainWindow, Action action) {
        settingWindow = createSettingWindow(mainWindow, action);
    }

    private static Stage createSettingWindow(Stage mainWindow, Action action) {
        logger.trace("初始化设置面板");
        Stage instance = new Stage();
        VBox root = new VBox(10);
        Scene scene = new Scene(root);

        if (SystemInfo.systemInfo.version >= DwmAPI.SUPPORT_API_VERSION) {
            //root.setStyle("-fx-background-color: transparent");
            //scene.setFill(Color.TRANSPARENT);
            instance.initStyle(StageStyle.UNIFIED);
        }
        instance.setTitle(Resources.StringRes.setting_item);
        instance.initModality(Modality.APPLICATION_MODAL);//设置为应用模态
        instance.initOwner(mainWindow);

        instance.setWidth(500);
        instance.setHeight(350);
        instance.setResizable(false);
        instance.getIcons().addAll(
                Resources.ImageRes.fpa16,
                Resources.ImageRes.fpa32,
                Resources.ImageRes.fpa64,
                Resources.ImageRes.fpa128,
                Resources.ImageRes.fpa256,
                Resources.ImageRes.fpa
        );


        root.setPadding(new Insets(10));
        ButtonBar buttonBar = new ButtonBar();

        VBox content = new VBox(10);
        VBox.setVgrow(content, Priority.ALWAYS);

        {
            //##################################ITEM1##################################

            Label audioAPI = new Label("选择音频API");
            audioAPI.setFont(Resources.FontRes.yahei_small_font);

            Region spacer1 = new Region();
            HBox.setHgrow(spacer1, Priority.ALWAYS);

            audioAPIChoice = new ChoiceBox<>();
            audioAPIChoice.getItems().addAll(Config.AudioAPI.DIRECT_SOUND, Config.AudioAPI.WASAPI);
            audioAPIChoice.setStyle(
                    "-fx-background-color: white;" +
                            "-fx-background-radius: 5;" +
                            "-fx-border-color: #ddd;" +
                            "-fx-border-radius: 5;" +
                            "-fx-padding: 1;" +
                            "-fx-font-size: 11px;"
            );
            audioAPIChoice.getSelectionModel().selectedIndexProperty().addListener(
                    (observable, oldValue, newValue) -> {
                        int index = newValue.intValue();
                        setAudioAPI(audioAPIChoice.getItems().get(index));
                    }
            );

            HBox item1 = new HBox(10);
            item1.setPrefWidth(Double.MAX_VALUE);
            item1.getChildren().addAll(audioAPI, spacer1, audioAPIChoice);

            //##################################ITEM2##################################

            Label location = new Label("选择歌词位置");
            location.setFont(Resources.FontRes.yahei_small_font);

            Region spacer2 = new Region();
            HBox.setHgrow(spacer2, Priority.ALWAYS);

            locationChoice = new ChoiceBox<>();
            locationChoice.getItems().addAll(Config.Location.LEFT, Config.Location.CENTER, Config.Location.RIGHT);
            locationChoice.setStyle(
                    "-fx-background-color: white;" +
                            "-fx-background-radius: 5;" +
                            "-fx-border-color: #ddd;" +
                            "-fx-border-radius: 5;" +
                            "-fx-padding: 1;" +
                            "-fx-font-size: 11px;"
            );
            locationChoice.getSelectionModel().selectedIndexProperty().addListener(
                    (observable, oldValue, newValue) -> {
                        int index = newValue.intValue();
                        setLocation(locationChoice.getItems().get(index));
                    }
            );

            HBox item2 = new HBox(10);
            item2.setPrefWidth(Double.MAX_VALUE);
            item2.getChildren().addAll(location, spacer2, locationChoice);

            content.getChildren().addAll(item1, item2);
        }

        {
            apply = new Button(Resources.StringRes.apply_button_name);
            Buttons.setLightOrDark(false, Buttons.Color.BLUE, apply);
            apply.setPrefWidth(80);
            apply.setFont(Resources.FontRes.yahei_super_small_font);
            apply.setOnAction(actionEvent ->
                action.clickSettingApplyButton(audioAPI, location)
            );

            cancel = new Button(Resources.StringRes.cancel_button_name);
            Buttons.setLightOrDark(false, Buttons.Color.WHITE, cancel);
            cancel.setPrefWidth(80);
            cancel.setFont(Resources.FontRes.yahei_super_small_font);
            cancel.setOnAction(actionEvent -> settingWindow.close());

            ok = new Button(Resources.StringRes.ok_button_name);
            Buttons.setLightOrDark(false, Buttons.Color.WHITE, ok);
            ok.setPrefWidth(80);
            ok.setFont(Resources.FontRes.yahei_super_small_font);
            ok.setOnAction(actionEvent ->
                    action.clickSettingOKButton(audioAPI, location)
            );

            buttonBar.getButtons().addAll(ok, cancel, apply);
        }


        root.getChildren().addAll(content, buttonBar);

        instance.setScene(scene);

        return instance;
    }

    public static void setAudioAPI(Config.AudioAPI newAudioAPI) {
        audioAPI = newAudioAPI;
    }

    public static void setLocation(Config.Location newLocation) {
        location = newLocation;
    }

    public static void readConfig() {
        audioAPI = Config.AudioAPI.valueOf(Config.api());
        location = Config.Location.valueOf(Config.location());
        audioAPIChoice.setValue(Config.AudioAPI.valueOf(Config.api()));
        locationChoice.setValue(Config.Location.valueOf(Config.location()));
    }
}
