/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.blankedv.sx4monitorfx;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.util.List;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;

import static de.blankedv.sx4monitorfx.SXnetClientThread.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.util.Duration;

/**
 *
 * @author mblank
 */
public class SX4Monitor extends Application {

    public static final int INVALID_INT = -1;

    public static SXnetClientThread client;
    public static BooleanProperty globalPower = new SimpleBooleanProperty(false);
    public static final int[] sxData = new int[SXMAX + 1];

    private static final ArrayList<LocoControl> allLocoControls = new ArrayList<>();
    private static int currentLocoControlID = 0;
    private static final ArrayList<AccessoryControl> allAccessoryControls = new ArrayList<>();
    private static int currentAccessoryControlID = 0;

    private static final int COLS = 3;    // number of rows of tableViews
    private static final ObservableList<SXValue> col1Data = FXCollections.observableArrayList();
    private static final ObservableList<SXValue> col2Data = FXCollections.observableArrayList();
    private static final ObservableList<SXValue> col3Data = FXCollections.observableArrayList();
    private static final List<Integer> channelsOfInterest = Collections.synchronizedList(new ArrayList<>());

    private final TableView[] tableViewLBData = {new TableView(), new TableView(), new TableView()};

    @Override
    public void start(Stage theStage) throws InterruptedException {

        final String version = "v0.3 - 13 Dec 2018";
        theStage.setTitle("SX4 Monitor");

        final Parameters params = getParameters();
        final List<String> parameters = params.getRaw();

        // load the image
        Image green = new Image("/de/blankedv/sx4monitorfx/res/greendot.png");
        Image red = new Image("/de/blankedv/sx4monitorfx/res/reddot.png");
        ImageView ivInfo = new ImageView(new Image("/de/blankedv/sx4monitorfx/res/info.png"));

        ImageView ivPowerState = new ImageView();
        globalPower.set(false);
        if (globalPower.get()) {
            ivPowerState.setImage(green);
        } else {
            ivPowerState.setImage(red);
        }

        // simple displays ImageView the image as is
        for (String s : parameters) {
            System.out.println("param: " + s);
        }



        theStage.setOnCloseRequest((WindowEvent e) -> {
            // needed for stopping lanbahn thread.
            Platform.exit();
            System.exit(0);
        });

        BorderPane bp = new BorderPane();
        bp.setPadding(new Insets(5, 5, 5, 5));

        HBox statusbar = new HBox(8);
        Label status = new Label("ready");
        status.textProperty().bind(connString);
        HBox.setHgrow(status, Priority.ALWAYS);
        statusbar.getChildren().add(status);

        GridPane optionsPane = new GridPane();
        setColumnConstraints(optionsPane);
        optionsPane.add(new Label("Power:"), 0, 0);

        ivPowerState.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            // toggle global power
            if (globalPower.get()) {
                client.send("SETPOWER 0");
                globalPower.set(false);
            } else {
                client.send("SETPOWER 1");
                globalPower.set(true);
            } 
            event.consume();
        });

        globalPower.addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
            if (newValue) {
                ivPowerState.setImage(green);
            } else {
                ivPowerState.setImage(red);
            }
        });

        optionsPane.add(ivPowerState, 1, 0);

        Button btnAddChan = new Button("+Monitor");
        optionsPane.add(btnAddChan, 2, 0);
        btnAddChan.setOnAction((ActionEvent event) -> {
            List<Integer> choices = new ArrayList<>();
            for (int i = 0; i <= SXMAX; i++) {
                choices.add(i);
            }
            ChoiceDialog<Integer> dialog = new ChoiceDialog<>(80, choices);
            dialog.setTitle("+ Adresse ?");
            dialog.setHeaderText("weitere Adresse monitoren:");
            dialog.setContentText("Auswahl:");
            Optional<Integer> result = dialog.showAndWait();
            result.ifPresent(addr -> {
                System.out.println("ausgewählte Adresse: " + addr);
                if (!channelsOfInterest.contains(addr)) {
                    channelsOfInterest.add(addr);
                    // if this channel was not used before, its data must be ==0
                    (getSXDataList(addr)).add(new SXValue(addr, 0));
                    client.send("R " + addr); // just to be sure ...
                }
            });
        });

        Button btnControlChan = new Button("+Control");
        optionsPane.add(btnControlChan, 3, 0);
        btnControlChan.setOnAction((ActionEvent event) -> {
            currentAccessoryControlID++;
            AccessoryControl ac = new AccessoryControl(currentAccessoryControlID);
            ac.updateUI();
            allAccessoryControls.add(ac);
        });

        Button btnLoco = new Button("+Loco");
        optionsPane.add(btnLoco, 4, 0);
        btnLoco.setOnAction((ActionEvent event) -> {
            currentLocoControlID++;
            LocoControl lc = new LocoControl(currentLocoControlID);
            lc.updateUI();
            allLocoControls.add(lc);
        });

        optionsPane.add(ivInfo, 6, 0);
        ivInfo.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            Utils.showInfoAlert("Info", "SX4Monitor\nhttps://opensx.net/sx4 ", "Programm version:" + version);
            event.consume();
        });

        optionsPane.setPadding(new Insets(3, 3, 3, 3));

        //Group root = new Group();
        Scene theScene = new Scene(bp, 600, 500);
        theStage.setScene(theScene);

        for (int i = 0; i < COLS; i++) {
            TableColumn<SXValue, String> chanCol = new TableColumn<>("ADR");
            // chanCol.setPrefWidth(70);
            TableColumn<SXValue, String> dataCol = new TableColumn<>("D");
            TableColumn<SXValue, String> bitsCol = new TableColumn<>("12345678");
            //dataCol.setPrefWidth(50);
            tableViewLBData[i].getColumns().setAll(chanCol, dataCol, bitsCol);

            tableViewLBData[i].setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
            chanCol.setMaxWidth(1f * Integer.MAX_VALUE * 18); // 30% width
            chanCol.setStyle("-fx-alignment: CENTER;");
            dataCol.setMaxWidth(1f * Integer.MAX_VALUE * 18); // 70% width
            dataCol.setStyle("-fx-alignment: CENTER;");
            bitsCol.setMaxWidth(1f * Integer.MAX_VALUE * 64); // 70% width
            bitsCol.setStyle("-fx-alignment: CENTER;");

            tableViewLBData[i].setCenterShape(true);
            chanCol.setCellValueFactory(new PropertyValueFactory<>("channel"));
            dataCol.setCellValueFactory(new PropertyValueFactory<>("data"));
            bitsCol.setCellValueFactory(new PropertyValueFactory<>("bits"));
        }

        tableViewLBData[0].setItems(new SortedList<>(col1Data.sorted()));
        tableViewLBData[1].setItems(new SortedList<>(col2Data.sorted()));
        tableViewLBData[2].setItems(new SortedList<>(col3Data.sorted()));

        bp.setTop(optionsPane); // getMenuBar());

        HBox hbCenter = new HBox(5);
        hbCenter.getChildren().addAll(tableViewLBData[0], tableViewLBData[1], tableViewLBData[2]);
        bp.setCenter(hbCenter);

        bp.setBottom(statusbar);   // add the status line at the bottom

        theScene.widthProperty().addListener((ObservableValue<? extends Number> observableValue, Number oldSceneWidth, Number newSceneWidth) -> {
            //System.out.println("Width: " + newSceneWidth);
        });

        theScene.heightProperty().addListener((ObservableValue<? extends Number> observableValue, Number oldSceneHeight, Number newSceneHeight) -> {
            //System.out.println("Height: " + newSceneHeight);
        });

        theStage.show();

        client = new SXnetClientThread();
        client.start();

        // prepare Alert for Program termination in case of loss of connection
        Alert alert = new Alert(AlertType.ERROR);
        String s = "Programm wird beendet";
        alert.setContentText(s);
        alert.showingProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                System.exit(1);
            }
        });
        if (shutdownFlag) {   // no connection from the beginning
            alert.setTitle("Fehler: Keine Verbindung zu SX4 möglich!");
            alert.setHeaderText("Netzwerk überprüfen!");
            alert.show();  // sofort anzeigen
        } else {
            // check connection every few seconds 
            alert.setTitle("Fehler: Verbindung verloren!");
            alert.setHeaderText("keine Verbindung mehr zu SX4...");
            Timeline threeSeconds = new Timeline(new KeyFrame(Duration.seconds(3), (ActionEvent event) -> {
                // check if connection is still alive
                // this is run on UI Thread (in contrast to "the good old java Timer" ...)
                if ((System.currentTimeMillis() - timeOfLastMsgReceived) > 10 * 1000) {
                    alert.show();
                }
            }));
            threeSeconds.setCycleCount(Timeline.INDEFINITE);
            threeSeconds.play();
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }

    public static void locoControlClosing(LocoControl lc) {
        allLocoControls.remove(lc);
        // enable acc-controls with this address (if any)
        for (AccessoryControl ac : allAccessoryControls) {
            if (lc.address == ac.address) {
                ac.updateUI();
            }
        }
    }

    public static void accessoryControlClosing(AccessoryControl ac) {
        allAccessoryControls.remove(ac);
    }


    public static void updatePower(int data) {
        if (data == 0) {
            globalPower.set(false);
        } else {
            globalPower.set(true);
        }
    }

    // new data received from SXnetClientThread
    public static void update(int addr, int data) {
        sxData[addr] = data;  // update the sxData
        //System.out.println("update "+addr+" "+data);
        // update all controls
        Platform.runLater(() -> {
            allLocoControls.stream().filter((lc) -> (lc.getAddress() == addr)).forEachOrdered((lc) -> {
                lc.updateUI();  // TODO data value ignored currently
             });
            allAccessoryControls.stream().filter((ac) -> (ac.getAddress() == addr)).forEachOrdered((ac) -> {
                ac.updateUI();
            });
        });

        // update this lists for display the data
        for (SXValue sxv : getSXDataList(addr)) {
            if (sxv.channel == addr) {
                //lv.data = data;
                //exists = true;
                getSXDataList(addr).remove(sxv);
                break;
            }
        }
        // then create new
        // ! update of the data only does not trigger "change" of
        // ObservableList lanbahnData !
        if ((data != 0) || channelsOfInterest.contains(addr)) {
            getSXDataList(addr).add(new SXValue(addr, data));
            if (!channelsOfInterest.contains(addr)) {
                channelsOfInterest.add(addr);
            }
        }
    }

    public static boolean isLocoControlAddress(int a) {
        for (LocoControl lc: allLocoControls) {
            if (lc.address == a) return true;
        }
        return false;
    }

    private static ObservableList<SXValue> getSXDataList(int addr) {
        if (addr < 80) {

            return col1Data;
        } else if ((addr >= 80) && (addr <= 90)) {
            return col2Data;
        } else if ((addr > 90) && (addr <= SXMAX)) {
            return col3Data;
        }
        return null;
    }


    private void setColumnConstraints(GridPane gp) {
        gp.setHgap(5);
        gp.setVgap(5);
        ColumnConstraints column1 = new ColumnConstraints();
        column1.setPercentWidth(10);
        ColumnConstraints column2 = new ColumnConstraints();
        column2.setPercentWidth(10);
        ColumnConstraints column3 = new ColumnConstraints();
        column3.setPercentWidth(15);
        ColumnConstraints column4 = new ColumnConstraints();
        column4.setPercentWidth(15);
        ColumnConstraints column5 = new ColumnConstraints();
        column5.setPercentWidth(15);
        ColumnConstraints column6 = new ColumnConstraints();
        column6.setPercentWidth(25);
        ColumnConstraints column7 = new ColumnConstraints();
        column7.setPercentWidth(5);
        gp.getColumnConstraints().addAll(column1, column2, column3, column4, column5, column6, column7);

    }

}
