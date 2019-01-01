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

import static de.blankedv.sx4monitorfx.SXnetClientThread.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;
import java.util.prefs.Preferences;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TableRow;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
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

    private static final ObservableList<SXValue> col0Data = FXCollections.observableArrayList();    // SX Data
    private static final ObservableList<SXValue> col1Data = FXCollections.observableArrayList();    // SX Data
    private static final ObservableList<SXValue> col2Data = FXCollections.observableArrayList();    // SX Data
    private static final ObservableList<SXValue> col3Data = FXCollections.observableArrayList();    // virtual Data
    private static final ObservableList<SXValue> col4Data = FXCollections.observableArrayList();    // virtual Data

    private static final List<Integer> channelsOfInterest = Collections.synchronizedList(new ArrayList<>());

    private final TableView[] tableViewData = {new TableView(), new TableView(), new TableView(), new TableView(), new TableView()};
    
    private final static int MAX_ROWS_PER_COLUMN = 12;  // reshuffle data between columns if more than this number of rows in one column
    private static int limit0 = 80, limit1 = 90, limit3 = 2000;   // initial limits for data-TO-column# mapping
    private static long lastRecalcColumns = System.currentTimeMillis();

    private MenuItem settingsItem;

    final String version = "v0.34 - 01 Jan 2019";
    private final String localServer = getLocalServer();

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

        createDataTables();
        bp.setTop(vboxTop);

        HBox hbCenter = new HBox(3);
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
            doOldDataCheck(col3Data);
            doOldDataCheck(col4Data);

            if (prefs.getBoolean("autoColumn", false)) {
                recalcColumns();
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

    /** unmark "old" values
     * 
     * @param colD 
     */
    public void doOldDataCheck(ObservableList<SXValue> colD) {
        ArrayList<SXValue> sxvOld = new ArrayList<>();
        for (SXValue sxv : colD) {
            if (sxv.isMarked() && sxv.isOld()) {
                sxvOld.add(sxv);
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
        if (addr <= SXMAX) {
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

        if (addr < limit0) {
            return col0Data;
        } else if ((addr >= limit0) && (addr <= limit1)) {
            return col1Data;
        } else if ((addr > limit1) && (addr <= SXMAX)) {
            return col2Data;
        } else if ((addr >= LBMIN) && (addr < limit3)) {
            return col3Data;
        } else {
            return col4Data;  // put all other addresses into last col
        }
    }

    /** move data from one column to the next or back, if size if higher than MAX_PER_COLUMN and different between them
     * 
     */
    private static void recalcColumns() {
        // SX columns
        if ((col0Data.size() > MAX_ROWS_PER_COLUMN) || (col1Data.size() > MAX_ROWS_PER_COLUMN) || (col2Data.size() > MAX_ROWS_PER_COLUMN)) {
            // SX-Data columns must be reshuffled
            int size = col0Data.size() + col1Data.size() + col2Data.size();
            int nPerCol = (size / 3) + 1;
            if ((col0Data.size() > nPerCol) || (col1Data.size() > nPerCol) || (col2Data.size() > nPerCol)) {
                int m = col0Data.size();
                int i = 0;
                if (col1Data.size() > m) {
                    m = col1Data.size();
                    i = 1;
                }
                if (col2Data.size() > m) {
                    m = col2Data.size();
                    i = 2;
                }
                SXValue mx;
                switch (i) {
                    case 0:  // move one value to the right
                        mx = getMax(col0Data);
                        if (mx != null) {
                            col0Data.remove(mx);
                            col1Data.add(mx);
                            limit0 = mx.getChannel();
                            System.out.println("0 -> 1");                           
                        }
                        break;
                    case 2:  // move one value to the left
                        mx = getMax(col2Data);
                        if (mx != null) {
                            col2Data.remove(mx);
                            col1Data.add(mx);
                            limit1 = mx.getChannel() + 1;
                            System.out.println("2 -> 1");
                        }
                        break;
                    case 1:
                        // move to right or left ??
                        if (col0Data.size() > col2Data.size()) {
                            // move to right
                            mx = getMax(col1Data);
                            if (mx != null) {
                                col1Data.remove(mx);
                                col2Data.add(mx);
                                limit1 = mx.getChannel();
                                System.out.println("1 -> 2");
                            }
                        } else {
                            // move to left
                            mx = getMin(col1Data);
                            if (mx != null) {
                                col1Data.remove(mx);
                                col0Data.add(mx);
                                limit0 = mx.getChannel()+1;
                                System.out.println("1 -> 0");
                            }
                        }
                        break;
                }
            }
           
            //System.out.println("limit0=" + limit0 + " limit1=" + limit1);
        }
        // lanbahn (=virtual) columns
        if ((col3Data.size() > MAX_ROWS_PER_COLUMN) || (col4Data.size() > MAX_ROWS_PER_COLUMN)) {
            // SX-Data columns must be reshuffled
            int size = col3Data.size() + col4Data.size();
            int nPerCol = (size / 2) + 1;
            if ((col3Data.size() > nPerCol) || (col4Data.size() > nPerCol) ) {
                int m = col3Data.size();
                int i = 3;
                if (col4Data.size() > m) {
                    m = col4Data.size();
                    i = 4;
                }
 
                SXValue mx;
                switch (i) {
                    case 3:  // move one value to the right
                        mx = getMax(col3Data);
                        if (mx != null) {
                            col3Data.remove(mx);
                            col4Data.add(mx);
                            limit3 = mx.getChannel();
                            System.out.println("3 -> 4");                           
                        }
                        break;
                    case 4:  // move one value to the left
                        mx = getMax(col4Data);
                        if (mx != null) {
                            col4Data.remove(mx);
                            col3Data.add(mx);
                            limit3 = mx.getChannel() + 1;
                            System.out.println("4 -> 3");
                        }
                        break;
                 }
            }       
            //System.out.println("limit3=" + limit3);
        }

    }

    private static SXValue getMax(ObservableList<SXValue> cd) {
        int aMax = 0;
        SXValue maxVal = null;

        for (SXValue s : cd) {
            if (s.getChannel() > aMax) {
                aMax = s.getChannel();
                maxVal = s;
            }
        }
        return maxVal;
    }
    
    private static SXValue getMin(ObservableList<SXValue> cd) {
        int aMin = 100000;
        SXValue minVal = null;

        for (SXValue s : cd) {
            if (s.getChannel() < aMin) {
                aMin = s.getChannel();
                minVal = s;
            }
        }
        return minVal;
    }

    private void createMenu(Preferences prefs, MenuBar menuBar) {
        // final ImageView ivSettings = new ImageView(new Image("/de/blankedv/sx4monitorfx/res/settings.png"));
        final ImageView ivInfo = new ImageView(new Image("/de/blankedv/sx4monitorfx/res/info.png"));
        final Menu menu1 = new Menu("File");
        final Menu menuOptions = new Menu("Optionen");
        final Menu menuInfo = new Menu("Hilfe");
        final MenuItem exitItem = new MenuItem("Prog.Ende/Exit");
        menu1.getItems().add(exitItem);
        exitItem.setOnAction((event) -> {
            System.exit(0);
        });
        hostAddress = prefs.get("server", localServer);
        settingsItem = new MenuItem("Server: " + hostAddress);

        //settingsItem.setGraphic(ivSettings);
        settingsItem.setOnAction((event) -> {
            selectServerIP(prefs);
        });

        RadioMenuItem virtData = new RadioMenuItem("Virt. Daten anzeigen");
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
        
        RadioMenuItem autoColumn = new RadioMenuItem("Spaltenbereiche automatisch");
        autoColumn.setSelected(prefs.getBoolean("autoColumn", false));
        
        autoColumn.setOnAction(e
                -> {
            if (autoColumn.isSelected()) {
                System.out.println("autoColumn ON");
                prefs.putBoolean("autoColumn", true);
            } else {
                System.out.println("autoColumn OFF");
                prefs.putBoolean("autoColumn", false);
            }
        });

        menuOptions.getItems().addAll(settingsItem, new SeparatorMenuItem(), virtData, new SeparatorMenuItem(), autoColumn);

        final MenuItem infoItem = new MenuItem("Info");
        menuInfo.getItems().add(infoItem);
        infoItem.setGraphic(ivInfo);
        infoItem.setOnAction((event) -> {
            System.out.println("info clicked");
            Dialogs.InfoAlert("Info", "SX4Monitor\nhttps://opensx.net/sx4 ", "Programm Version:" + version, this);
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

        Button btnLoco = new Button("+Lok");
        btnLoco.setOnAction((ActionEvent event) -> {
            currentLocoControlID++;
            LocoControl lc = new LocoControl(currentLocoControlID);
            lc.updateUI();
            allLocoControls.add(lc);
        });

        buttonsPane.getChildren().addAll(new Label("Power:"), ivPowerState, btnAddChan, btnControlChan, btnLoco);

    }

    private void createDataTables() {
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
                            } else {
                                setStyle("");
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

    private void selectServerIP(Preferences prefs) {

        String name = prefs.get("server", localServer);
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
                settingsItem.setText("Server: " + hostname);

            } catch (UnknownHostException ex) {
                System.out.println("ungültiger Host");
            }

            dialog.close();
        });

    }

    private boolean openServer(Preferences prefs) {

        String serverName = prefs.get("server", localServer);

        try {
            InetAddress ip = InetAddress.getByName(serverName);
            if (client != null) {
                client.shutdown();
            }
            try {
                Thread.sleep(100);
                client = new SXnetClientThread(ip);
                client.start();
                prefs.put("server", serverName);
                return true;
            } catch (InterruptedException ex) {
                System.out.println("kann Client nicht starten");
            }
        } catch (UnknownHostException ex) {
            System.out.println("ERROR: unknownHost, kann Client nicht starten");
        }

        return false;
    }

    private String getLocalServer() {
        InetAddress hostAddr = NIC.getFirstIp();

        if (hostAddr == null) {
            System.out.println("kein Netzwerk");
            return "?";
        }  // no network

        return hostAddr.getHostAddress();
    }

}
