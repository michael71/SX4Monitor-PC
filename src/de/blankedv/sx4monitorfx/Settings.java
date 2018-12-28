/*
 * Copyright (C) 2018 Michael Blank <mblank@bawue.de>
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
package de.blankedv.sx4monitorfx;

import static de.blankedv.sx4monitorfx.SX4Monitor.client;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.prefs.Preferences;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

/**
 *
 * @author Michael Blank <mblank@bawue.de>
 */
public class Settings {

    public static void startSett(Preferences prefs) {

        final Stage mySettingsDialog = new Stage();
        mySettingsDialog.initModality(Modality.WINDOW_MODAL);
        mySettingsDialog.setTitle("Settings");

        Label lblIP = new Label("new Server IP address?");

        TextField text = new TextField("192.168.178.xx");

        Button btnSave = new Button("Save");
        btnSave.setOnAction((ActionEvent arg0) -> {
            String sName = text.getText();
            try {
                InetAddress ip = InetAddress.getByName(sName);
                System.out.println("new ip selected = " + sName);
                if (client != null) {
                    client.shutdown();
                }
                try {
                    prefs.put("server", sName);
                    Thread.sleep(100);
                    client = new SXnetClientThread(ip);
                    client.start();
                } catch (InterruptedException ex) {
                    System.out.println("kann Client nicht starten");
                }

            } catch (UnknownHostException ex) {
                System.out.println("ungültiger Host");
            }

            mySettingsDialog.close();
        });

        Button btnCancel = new Button("Cancel");
        btnSave.setOnAction((ActionEvent arg0) -> {
            System.out.println("cancel");
            mySettingsDialog.close();
        });

        VBox vbox = new VBox();

        vbox.getChildren().addAll(lblIP, text, btnSave);
        vbox.setSpacing(20.0);
        vbox.setAlignment(Pos.CENTER);

        Scene scene = new Scene(vbox, 300, 200);

        mySettingsDialog.setScene(scene);
        mySettingsDialog.show();

    }

}
