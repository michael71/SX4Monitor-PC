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

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.util.ArrayList;

import static de.blankedv.sx4monitor_pc.SX4Monitor.*;

/**
 * Window for accessory control (1 SX address, i.e. 8 bits)
 * <p>
 * sxData[address] is modified by sending "S <ADDR> <DATA>" to SXNET
 *
 * @author mblank
 */
public class AccessoryControl extends Control {

    private ArrayList<CheckBox> cbs = new ArrayList<>();

    AccessoryControl(int instance) {
        super(instance, "AC");

        GridPane gridPane = new GridPane();
        setColumnConstraints(gridPane);
        gridPane.setHgap(5);
        gridPane.setVgap(5);
        gridPane.setPadding(new Insets(10, 10, 10, 10));

        gridPane.add(new Label("  ADR:"), 0, 0, 3, 1);

        gridPane.add(new Label("   D:"), 3, 0, 2, 1);

        gridPane.add(lblData, 3, 1, 2, 1);

        gridPane.add(cbAddresses, 0, 1, 3, 1);

        for (int i = 1; i <= 8; i++) {
            gridPane.add(new Label(" " + i), i + 4, 0, 1, 1);
            CheckBox cb = new CheckBox();
            cbs.add(cb);
            gridPane.add(cb, i + 4, 1, 1, 1);

        }

        // set up listeners for checkbox changes
        for (int i = 0; i < 8; i++) {
            final int j = i;
            cbs.get(i).setOnAction(e -> {
                System.out.println("event action i=" + j);
                System.out.println("isSelected=" + cbs.get(j).isSelected());
                cbValueChanged(j);
            });
        }


        //StackPane sPane = new StackPane();
        // sPane.getChildren().add(gridPane);

        Scene scene = new Scene(gridPane); //, 330, 80);

        // create window (Stage)  
        Stage stage = new Stage();
        stage.setTitle("Accessory Control");
        stage.setScene(scene);
        stage.setX(300 + myInstance * 10);
        stage.setY(200 + myInstance * 10);
        stage.show();
        stage.setOnCloseRequest((WindowEvent we) -> {
            SX4Monitor.accessoryControlClosing(this);
            System.out.println("Accessory Control Stage#" + myInstance + " is closing");
        });
    }


    @Override
    void updateUI() {
        data = sxData[address];
        lblData.setText("  " + data);
        for (int i = 0; i < 8; i++) {
            if ((data & (1 << i)) != 0) {
                cbs.get(i).setSelected(true);
            } else {
                cbs.get(i).setSelected(false);
            }
        }
        // check, if there is a loco control with my address
        //    => then disable all checkboxes
        if (isLocoControlAddress(address)) {
            for (int i = 0; i < 8; i++) {
                cbs.get(i).setDisable(true);
            }
        } else {
            for (int i = 0; i < 8; i++) {
                cbs.get(i).setDisable(false);
            }
        }
    }

    private void setColumnConstraints(GridPane gp) {
        int n = 13;
        float w = 100f / n;

        for (int i = 0; i < n; i++) {
            ColumnConstraints colCB = new ColumnConstraints();
            colCB.setPercentWidth(w);
            gp.getColumnConstraints().add(colCB);
        }

    }

    private void cbValueChanged(int i) {
        if (data == INVALID_INT) data = 0;
        if (cbs.get(i).isSelected()) {
            data |= (1 << i);
        } else {
            data &= ~(1 << i);
        }
        lblData.setText("  " + data);
        if (!Utils.isValidSXAddress(address)) {
            System.out.println("ERROR: not a valid address =" + address);
            return;
        }
        // sxData[address] = data;  =is redundant
        client.send("S " + address + " " + data);
        SX4Monitor.update(address, data);  // sxData[] get updated in SX4Monitor
    }

}
