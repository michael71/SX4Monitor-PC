/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.blankedv.sx4monitorfx;

import com.sun.deploy.uitoolkit.impl.fx.HostServicesFactory;
import com.sun.javafx.application.HostServicesDelegate;
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

import static de.blankedv.sx4monitorfx.SXnetClientThread.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TableRow;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import javafx.util.Callback;
import javafx.util.Duration;

/**
 *
 * @author mblank
 */
public class SX4Monitor extends Application {

    public static final int INVALID_INT = -1;
    public static final int NEW_TIME_SEC = 5;  //how many seconds a new value is considered "new"

    public static SXnetClientThread client;
    public static BooleanProperty globalPower = new SimpleBooleanProperty(false);
    public static final int[] sxData = new int[SXMAX + 1];
    public static final int LBMIN = 1200;  // minimum (pure) lanbahn address
    public static final int LBMAX = 9999;  // maximum (pure) lanbahn address
    public static final int MAX_SX_COL = 2;  // cols 0, 1, 2 are "SX" type, else lanbahn
    public static String hostAddress;

    private static final ArrayList<LocoControl> allLocoControls = new ArrayList<>();
    private static int currentLocoControlID = 0;
    private static final ArrayList<AccessoryControl> allAccessoryControls = new ArrayList<>();
    private static int currentAccessoryControlID = 0;

    private static final ObservableList<SXValue> col0Data = FXCollections.observableArrayList();
    private static final ObservableList<SXValue> col1Data = FXCollections.observableArrayList();
    private static final ObservableList<SXValue> col2Data = FXCollections.observableArrayList();
    private static final ObservableList<SXValue> col3Data = FXCollections.observableArrayList();
    private static final ObservableList<SXValue> col4Data = FXCollections.observableArrayList();
    private static final List<Integer> channelsOfInterest = Collections.synchronizedList(new ArrayList<>());

    private final TableView[] tableViewData = {new TableView(), new TableView(), new TableView(), new TableView(), new TableView()};

    private MenuItem settingsItem;

    final String version = "v0.33 - 28 Dec 2018";
    final String DEFAULT_SERVER = "192.168.178.xx";

    @Override
    public void start(Stage theStage) throws InterruptedException {

        theStage.setTitle("SX4 Monitor");

        final Parameters params = getParameters();
        final List<String> parameters = params.getRaw();

        final Preferences prefs = Preferences.userNodeForPackage(this.getClass());

        // simple displays ImageView the image as is
        parameters.forEach((s) -> {
            System.out.println("param: " + s);
        });

        theStage.setOnCloseRequest((WindowEvent e) -> {
            // needed for stopping network thread.
            Platform.exit();
            System.exit(0);
        });

        MenuBar menuBar = new MenuBar();
        createMenu(prefs, menuBar);

        BorderPane bp = new BorderPane();
        bp.setPadding(new Insets(0, 0, 0, 0));

        HBox statusbar = new HBox(8);
        Label status = new Label("ready");
        status.textProperty().bind(connString);
        HBox.setHgrow(status, Priority.ALWAYS);
        statusbar.getChildren().add(status);

        HBox buttonsPane = new HBox(6);
        VBox vboxTop = new VBox(menuBar, buttonsPane);
        createButtons(buttonsPane);
        buttonsPane.setPadding(new Insets(3, 3, 3, 3));

        createSXDataTables();
        bp.setTop(vboxTop);

        HBox hbCenter = new HBox(3);
        /*HBox.setHgrow(tableViewSXData[0], Priority.ALWAYS);
        HBox.setHgrow(tableViewSXData[1], Priority.ALWAYS);
        HBox.setHgrow(tableViewSXData[2], Priority.ALWAYS);
        HBox.setHgrow(tableViewLanbahnData[0], Priority.ALWAYS);
        HBox.setHgrow(tableViewLanbahnData[1], Priority.ALWAYS);
        tableViewSXData[0].setPrefWidth(100000 * 25);
        tableViewSXData[1].setPrefWidth(100000 * 25);
        tableViewSXData[2].setPrefWidth(100000 * 25);
        tableViewLanbahnData[0].setPrefWidth(100000 * 20);
        tableViewLanbahnData[1].setPrefWidth(100000 * 20); */
        hbCenter.getChildren().addAll(tableViewData[0], tableViewData[1], tableViewData[2]);
        Scene theScene;
        if (prefs.getBoolean("virtualData", false)) {
            hbCenter.getChildren().addAll(tableViewData[3], tableViewData[4]);
            theScene = new Scene(bp, 950, 500);
        } else {
            theScene = new Scene(bp, 600, 500);

        }
        theStage.setScene(theScene);

        bp.setCenter(hbCenter);

        bp.setBottom(statusbar);   // add the status line at the bottom

        theScene.widthProperty().addListener((ObservableValue<? extends Number> observableValue, Number oldSceneWidth, Number newSceneWidth) -> {
            //System.out.println("Width: " + newSceneWidth);
        });

        theScene.heightProperty().addListener((ObservableValue<? extends Number> observableValue, Number oldSceneHeight, Number newSceneHeight) -> {
            //System.out.println("Height: " + newSceneHeight);
        });

        theStage.show();

        boolean result = openServer(prefs);
        if (!result) {
            Dialogs.ErrorExit("Fehler: Keine Verbindung zu SX4 möglich!", "Netzwerk überprüfen!");
        }

        // check connection every few seconds
        Timeline fiveSeconds = new Timeline(new KeyFrame(Duration.seconds(5), (ActionEvent event) -> {
            // check if connection is still alive, this is run on UI Thread (in contrast to "the good old java Timer")
            if ((System.currentTimeMillis() - timeOfLastMsgReceived) >= 10 * 1000) {
                timeOfLastMsgReceived = System.currentTimeMillis() + 10 * 1000;  // shown again after 20secs
                Platform.runLater(() -> {
                    Dialogs.ErrorExit("Fehler: Verbindung verloren!", "keine Verbindung mehr zu SX4...");
                });
            }
        }));
        fiveSeconds.setCycleCount(Timeline.INDEFINITE);
        fiveSeconds.play();

        Timeline twoSeconds = new Timeline(new KeyFrame(Duration.seconds(2), (ActionEvent event) -> {
            doOldDataCheck(col0Data);
            doOldDataCheck(col1Data);
            doOldDataCheck(col2Data);
            if (prefs.getBoolean("virtualData", false)) {
                doOldDataCheck(col3Data);
                doOldDataCheck(col4Data);
            }
        }));

        twoSeconds.setCycleCount(Timeline.INDEFINITE);
        twoSeconds.play();

    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }

    public void doOldDataCheck(ObservableList<SXValue> colD) {
        ArrayList<SXValue> sxvOld = new ArrayList<>();
        for (SXValue sxv : colD) {
            if (sxv.isMarked()) {
                if ((System.currentTimeMillis() - sxv.gettStamp()) > 5000) {
                    sxvOld.add(sxv);
                }
            }
        }
        //System.out.println("sxvOld.size()=" + sxvOld.size());
        for (SXValue s : sxvOld) {
            colD.remove(s);
            s.setMarked(false);
            colD.add(new SXValue(s));
        }
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
        if (addr <= LBMIN) {
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
        }

        // update this lists for display the data
        for (SXValue sxv : getSXDataList(addr)) {
            if (sxv.channel == addr) {
                getSXDataList(addr).remove(sxv);
                break;
            }
        }
        // then create new
        // ! update of the data only does not trigger "change" of ObservableList
        if ((data != 0) || channelsOfInterest.contains(addr) || (addr >= LBMIN)) {
            getSXDataList(addr).add(new SXValue(addr, data, true));
            if (!channelsOfInterest.contains(addr)) {
                channelsOfInterest.add(addr);
            }
        }
    }

    public static boolean isLocoControlAddress(int a) {
        for (LocoControl lc : allLocoControls) {
            if (lc.address == a) {
                return true;
            }
        }
        return false;
    }

    public static void setNewIP(String name) {
        InetAddress ip;
        try {
            ip = InetAddress.getByName(name);
            System.out.println("new ip selected = " + name);
            if (client != null) {
                client.shutdown();
            }
            try {
                Thread.sleep(100);
                client = new SXnetClientThread(ip);
                client.start();
            } catch (InterruptedException ex) {
                System.out.println("kann Client nicht starten");
            }

        } catch (UnknownHostException ex) {
            System.out.println("ungültiger Host");
        }
    }

    private static ObservableList<SXValue> getSXDataList(int addr) {
        if (addr < 80) {
            return col0Data;
        } else if ((addr >= 80) && (addr <= 90)) {
            return col1Data;
        } else if ((addr > 90) && (addr <= SXMAX)) {
            return col2Data;
        } else if ((addr >= LBMIN) && (addr < 2000)) {
            return col3Data;
        } else {
            return col4Data;  // put all other addresses into last col
        }
    }

    private void createMenu(Preferences prefs, MenuBar menuBar) {
        // final ImageView ivSettings = new ImageView(new Image("/de/blankedv/sx4monitorfx/res/settings.png"));
        final ImageView ivInfo = new ImageView(new Image("/de/blankedv/sx4monitorfx/res/info.png"));
        final Menu menu1 = new Menu("File");
        final Menu menuOptions = new Menu("Options");
        final Menu menuInfo = new Menu("Help");
        final MenuItem exitItem = new MenuItem("Exit");
        menu1.getItems().add(exitItem);
        exitItem.setOnAction((event) -> {
            System.exit(0);
        });
        hostAddress = prefs.get("server", DEFAULT_SERVER);
        settingsItem = new MenuItem(hostAddress);

        //settingsItem.setGraphic(ivSettings);
        settingsItem.setOnAction((event) -> {
            selectServerIP(prefs, hostAddress);
        });
        RadioMenuItem localServer = new RadioMenuItem("Local Server");
        RadioMenuItem remoteServer = new RadioMenuItem("Remote Server");
        ToggleGroup group = new ToggleGroup();
        localServer.setToggleGroup(group);
        remoteServer.setToggleGroup(group);
        if (prefs.getBoolean("localServer", true)) {
            localServer.setSelected(true);
            remoteServer.setSelected(false);
            settingsItem.setDisable(true);
        } else {
            localServer.setSelected(false);
            remoteServer.setSelected(true);
            settingsItem.setDisable(false);
        }
        localServer.setOnAction(e
                -> {
            if (localServer.isSelected()) {
                System.out.println("ls: local server");
                prefs.putBoolean("localServer", true);
                settingsItem.setDisable(true);
                openLocalServer();
            } else {
                System.out.println("ls: remote server");
                prefs.putBoolean("localServer", false);
                settingsItem.setDisable(false);
            }
        }
        );
        remoteServer.setOnAction(e
                -> {
            if (remoteServer.isSelected()) {
                System.out.println("rs: remote server");
                settingsItem.setDisable(false);
                prefs.putBoolean("localServer", false);
                openRemoteServer(prefs);
            } else {
                System.out.println("rs: local server");
                prefs.putBoolean("localServer", true);
                settingsItem.setDisable(true);
            }
        });

        RadioMenuItem virtData = new RadioMenuItem("Disp virtual Data");
        virtData.setSelected(prefs.getBoolean("virtualData", false));

        virtData.setOnAction(e
                -> {
            if (virtData.isSelected()) {
                System.out.println("virtData ON");
                prefs.putBoolean("virtualData", true);
            } else {
                System.out.println("virtData OFF");
                prefs.putBoolean("virtualData", false);
            }
        });

        menuOptions.getItems().addAll(localServer, remoteServer, settingsItem, new SeparatorMenuItem(), virtData);

        final MenuItem infoItem = new MenuItem("Info");
        menuInfo.getItems().add(infoItem);
        infoItem.setGraphic(ivInfo);
        infoItem.setOnAction((event) -> {
            System.out.println("info clicked");
            Dialogs.InfoAlert("Info", "SX4Monitor\nhttps://opensx.net/sx4 ", "Programm version:" + version, this);
        });

        menuBar.getMenus().addAll(menu1, menuOptions, menuInfo);
    }

    private void createButtons(HBox buttonsPane) {

        // load the image
        Image green = new Image("/de/blankedv/sx4monitorfx/res/greendot.png");
        Image red = new Image("/de/blankedv/sx4monitorfx/res/reddot.png");

        ImageView ivPowerState = new ImageView();
        globalPower.set(false);
        if (globalPower.get()) {
            ivPowerState.setImage(green);
        } else {
            ivPowerState.setImage(red);
        }

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

        Button btnAddChan = new Button("+Monitor");
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
                    (getSXDataList(addr)).add(new SXValue(addr, 0, true));
                    client.send("R " + addr); // just to be sure ...
                }
            });
        });

        Button btnControlChan = new Button("+Accessory");
        btnControlChan.setOnAction((ActionEvent event) -> {
            currentAccessoryControlID++;
            AccessoryControl ac = new AccessoryControl(currentAccessoryControlID);
            ac.updateUI();
            allAccessoryControls.add(ac);
        });

        Button btnLoco = new Button("+Loco");
        btnLoco.setOnAction((ActionEvent event) -> {
            currentLocoControlID++;
            LocoControl lc = new LocoControl(currentLocoControlID);
            lc.updateUI();
            allLocoControls.add(lc);
        });

        buttonsPane.getChildren().addAll(new Label("Power:"), ivPowerState, btnAddChan, btnControlChan, btnLoco);

    }

    private void createSXDataTables() {
        for (int i = 0; i < 5; i++) {
            TableColumn<SXValue, String> chanCol = new TableColumn<>("ADR");
            TableColumn<SXValue, String> dataCol = new TableColumn<>("D");
            if (i <= MAX_SX_COL) {
                TableColumn<SXValue, String> bitsCol = new TableColumn<>("12345678");
                tableViewData[i].getColumns().setAll(chanCol, dataCol, bitsCol);
                bitsCol.setMaxWidth(1f * Integer.MAX_VALUE * 64); // 70% width
                bitsCol.setCellValueFactory(new PropertyValueFactory<>("bits"));
                bitsCol.setStyle("-fx-alignment: CENTER;");
            } else {
                tableViewData[i].getColumns().setAll(chanCol, dataCol);
            }

            tableViewData[i].setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
            chanCol.setMaxWidth(1f * Integer.MAX_VALUE * 18); // 30% width
            chanCol.setStyle("-fx-alignment: CENTER;");
            dataCol.setMaxWidth(1f * Integer.MAX_VALUE * 18); // 70% width
            dataCol.setStyle("-fx-alignment: CENTER;");

            tableViewData[i].setCenterShape(true);
            tableViewData[i].setRowFactory(new Callback<TableView<SXValue>, TableRow<SXValue>>() {
                @Override
                public TableRow<SXValue> call(TableView<SXValue> tableView) {
                    final TableRow<SXValue> row = new TableRow<SXValue>() {
                        @Override
                        protected void updateItem(SXValue sxv, boolean empty) {
                            super.updateItem(sxv, empty);
                            if (!empty) {
                                if (sxv.isMarked()) {
                                    setStyle("-fx-background-color: yellow;");
                                } else {
                                    setStyle("");
                                }
                            }
                        }
                    };
                    return row;
                }
            });

            chanCol.setCellValueFactory(new PropertyValueFactory<>("channel"));
            dataCol.setCellValueFactory(new PropertyValueFactory<>("data"));

        }

        tableViewData[0].setItems(new SortedList<>(col0Data.sorted()));
        tableViewData[1].setItems(new SortedList<>(col1Data.sorted()));
        tableViewData[2].setItems(new SortedList<>(col2Data.sorted()));
        tableViewData[3].setItems(new SortedList<>(col3Data.sorted()));
        tableViewData[4].setItems(new SortedList<>(col4Data.sorted()));

    }

    private void selectServerIP(Preferences prefs, String name) {

        TextInputDialog dialog = new TextInputDialog(name);

        dialog.setTitle("remote server IP");
        dialog.setHeaderText(null);

        Optional<String> result = dialog.showAndWait();

        result.ifPresent(hostname -> {
            try {
                InetAddress ip = InetAddress.getByName(hostname);
                System.out.println("new ip selected = " + hostname);
                if (client != null) {
                    client.shutdown();
                }
                try {
                    prefs.put("server", hostname);
                    Thread.sleep(100);
                    client = new SXnetClientThread(ip);
                    client.start();
                } catch (InterruptedException ex) {
                    System.out.println("kann Client nicht starten");
                }
                settingsItem = new MenuItem(hostname);

            } catch (UnknownHostException ex) {
                System.out.println("ungültiger Host");
            }

            dialog.close();
        });

    }

    private boolean openServer(Preferences prefs) {
        if (prefs.getBoolean("localServer", true)) {
            return openLocalServer();
        } else {
            return openRemoteServer(prefs);
        }
    }

    private boolean openLocalServer() {
        List<InetAddress> ips = NIC.getmyip();
        if (!ips.isEmpty()) {
            InetAddress ip = ips.get(0);
            if (client != null) {
                client.shutdown();
            }
            try {
                Thread.sleep(100);
                client = new SXnetClientThread(ip);
                client.start();
                return true;
            } catch (InterruptedException ex) {
                System.out.println("kann Client nicht starten");
            }
        } else {
            System.out.println("ERROR: kein Netzwerk, kann Client nicht starten");
        }
        return false;
    }

    private boolean openRemoteServer(Preferences prefs) {
        String serverName = prefs.get("server", DEFAULT_SERVER);
        InetAddress ip = null;
        try {
            ip = InetAddress.getByName(serverName);
        } catch (UnknownHostException ex) {
            System.out.println("ERROR: unknownHost, kann Client nicht starten");
        }
        if (client != null) {
            client.shutdown();
        }
        try {
            Thread.sleep(100);
            client = new SXnetClientThread(ip);
            client.start();
            return true;
        } catch (InterruptedException ex) {
            System.out.println("kann Client nicht starten");
        }
        return false;
    }

    private String getDefaultHostAddress(Preferences prefs) {
        List<InetAddress> ips = NIC.getmyip();
        String host = "";
        if (ips.isEmpty()) {
            host = prefs.get("server", DEFAULT_SERVER);
        } else {
            host = ips.get(0).getHostAddress();
        }
        return host;
    }
}
