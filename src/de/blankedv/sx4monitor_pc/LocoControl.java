/*
 * Copyright (C) 2018 mblank
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.blankedv.sx4monitor_pc;

import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.ArrayList;

import static de.blankedv.sx4monitor_pc.SX4Monitor.client;
import static de.blankedv.sx4monitor_pc.SX4Monitor.sxData;

/**
 * Window for Loco Control
 * <p>
 * locoData is modified and sent to SXNET
 * (locoData is the "MASTER" data source - only once read from sxData[address] when LocoControl is created)
 *
 * @author mblank
 */
public class LocoControl extends Control implements MouseWheelListener {

    private ArrayList<CheckBox> cbs = new ArrayList<>();

    private final Label lblSpeed = new Label();

    private final Button btnStop = new Button("  STOP  ");
    private final Slider slSpeed = new Slider(0, 31, 0);

    private final int LIGHT = 0x40;
    private final int DIR = 0x20;
    private final int FUNC = 0x80;

    private final ImageView ivLeft = new ImageView(new Image("/de/blankedv/sx4monitor_pc/res/left3.png", 20, 20, true, true));
    private final ImageView ivRight = new ImageView(new Image("/de/blankedv/sx4monitor_pc/res/right3.png", 20, 20, true, true));

    private final ImageView ivLightOn = new ImageView(new Image("/de/blankedv/sx4monitor_pc/res/lamp1.png", 20, 20, true, true));
    private final ImageView ivLightOff = new ImageView(new Image("/de/blankedv/sx4monitor_pc/res/lamp0.png", 20, 20, true, true));

    private final ImageView ivFuncOn = new ImageView(new Image("/de/blankedv/sx4monitor_pc/res/function1.png", 20, 20, true, true));
    private final ImageView ivFuncOff = new ImageView(new Image("/de/blankedv/sx4monitor_pc/res/function0.png", 20, 20, true, true));

    private final FunctionButton btnDir = new FunctionButton(DIR, ivLeft, ivRight);
    private final FunctionButton btnLight = new FunctionButton(LIGHT, ivLightOn, ivLightOff);
    private final FunctionButton btnFunc = new FunctionButton(FUNC, ivFuncOn, ivFuncOff);
    //private final FunctionButton btnFunc = new FunctionButton(FUNC,"F");

    private int locoData;

    LocoControl(int instanceNumber) {

        super(instanceNumber, "LC");  // LC -> to get the right preferences

        System.out.println("Loco Control Stage#" + myInstance + " is starting");
        locoData = data; // init from central

        GridPane gridPane = new GridPane();

        setColumnConstraints(gridPane);
        gridPane.setHgap(5);
        gridPane.setVgap(5);
        gridPane.setPadding(new Insets(10, 10, 10, 10));

        gridPane.add(new Label("  ADR:"), 0, 0, 3, 1);
        gridPane.add(lblData, 0, 4, 2, 1);
        gridPane.add(btnStop, 0, 5, 3, 1);
        gridPane.add(cbAddresses, 0, 1, 3, 1);

        // Light
        gridPane.add(btnLight, 3, 1, 1, 1);
        btnLight.update(locoData);  // init
        btnLight.setOnAction(e -> {
            locoData = btnLight.toggle(locoData);
            sendLocoData();
        });

        // Function
        gridPane.add(btnFunc, 4, 1, 1, 1);
        btnFunc.update(locoData);  // init
        btnFunc.setOnAction(e -> {
            locoData = btnFunc.toggle(locoData);
            sendLocoData();
        });

        // Direction
        gridPane.add(btnDir, 4, 5, 1, 1);
        btnDir.update(locoData);  // init
        btnDir.setOnAction(e -> {
            locoData = btnDir.toggle(locoData);
            locoData &= ~(0x1f);  // speed = 0
            sendLocoData();
        });

        // Speed
        slSpeed.setBlockIncrement(5);
        slSpeed.setOrientation(Orientation.VERTICAL);
        slSpeed.setShowTickMarks(true);
        slSpeed.setShowTickLabels(true);
        slSpeed.setMajorTickUnit(5);

        gridPane.add(slSpeed, 6, 0, 1, 6);

        gridPane.add(lblSpeed, 3, 5, 1, 1);
        int initSpeed = getSpeed();  // init
        lblSpeed.setText("" + initSpeed);  // init
        slSpeed.setValue(1.0d * initSpeed);  // init
        slSpeed.valueProperty().addListener(
                (ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> {
                    String sp = "" + newValue;
                    int speed = (int) Double.parseDouble(sp);
                    lblSpeed.setText("" + speed);
                    locoData = (locoData & 0xe0) | (speed & 0x1f);
                    sendLocoData();
                });

        btnStop.setOnAction(e -> {
            locoData &= ~(0x1f);  // speed = 0
            sendLocoData();
        });

        Scene scene = new Scene(gridPane);

        // stop loco when middle mouse button (= mouse wheel button) is clicked 
        scene.setOnMouseClicked((MouseEvent e) -> {
            MouseButton mouseButton = e.getButton();
            if (mouseButton == MouseButton.MIDDLE) {
                locoData &= ~(0x1f);  // speed = 0
                sendLocoData();
            }
        });

        // change speed with mouse wheel scroll
        scene.setOnScroll((ScrollEvent event) -> {
            int sp = getSpeed();
            if (event.getDeltaY() > 0) {
                sp += 1;
                if (sp > 31) {
                    sp = 31;
                }
            } else {
                sp -= 1;
                if (sp < 0) {
                    sp = 0;
                }
            }
            lblSpeed.setText("   " + sp);
            slSpeed.setValue(1.0d * sp);
            locoData = (locoData & 0xe0) | (sp & 0x1f);
            sendLocoData();
        });

        // create window (Stage)
        Stage stage = new Stage();
        stage.setTitle("Lok");
        stage.setScene(scene);
        stage.setX(300 + myInstance * 10);
        stage.setY(400 + myInstance * 10);
        stage.show();
        stage.setOnCloseRequest((WindowEvent we) -> {
            SX4Monitor.locoControlClosing(this);
            System.out.println("Loco Control Stage#" + myInstance + " is closing");
        });
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {

    }


    @Override
    void updateUI() {
        //data = sxData[address];   NOT USED, because loco var "locoData" is our MASTER (except for init from sxData)

        btnDir.update(locoData);
        btnLight.update(locoData);
        btnFunc.update(locoData);
        int speed = getSpeed();
        lblSpeed.setText("   " + speed);
        slSpeed.setValue(1.0d * speed);
        lblData.setText("D: " + locoData);
    }

    private void setColumnConstraints(GridPane gp) {
        int n = 7;
        float w = 100f / n;

        for (int i = 0; i < n; i++) {
            ColumnConstraints colCB = new ColumnConstraints();
            colCB.setPercentWidth(w);
            gp.getColumnConstraints().add(colCB);
        }

    }


    private int getSpeed() {
        return (locoData & 0x1f);
    }

    private void sendLocoData() {
        if ((locoData == lastData) && ((System.currentTimeMillis() - lastLocoDataSent) < 1000)) {
            // don't send identical speed twice (within a second interval)
            return;
        }

        sxData[address] = locoData;
        client.send("S " + address + " " + locoData);
        lastData = locoData;
        lastLocoDataSent = System.currentTimeMillis();
        SX4Monitor.update(address, locoData);  // sxData[] get updated in SX4Monitor
    }

    private void toggleBit(int bitmask) {  // sx bit
        boolean f = ((locoData & bitmask) != 0);
        if (f) {
            locoData &= ~(bitmask);
        } else {
            locoData |= bitmask;
        }
    }

}
