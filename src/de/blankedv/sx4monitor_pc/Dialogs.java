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

import javafx.application.Application;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Window;

import java.util.Optional;

import static de.blankedv.sx4monitor_pc.SXnetClientThread.shutdownFlag;

/**
 * @author mblank
 */
public class Dialogs {

    public static void ErrorExit(String title, String header) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setContentText("Programm wird beendet");
        alert.setTitle(title);
        alert.setHeaderText(header);
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent()) {
            if (result.get() == ButtonType.OK) {
                System.exit(1);
            } else {
                shutdownFlag = false;
            }
        }
    }

    public static void InfoAlert(String title, String header, String msg, Application app) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText(msg);
        alert.setTitle(title);
        alert.setHeaderText(header);
        Window window = alert.getDialogPane().getScene().getWindow();
        window.setOnCloseRequest(event -> window.hide());

        ButtonType openOpensx = new ButtonType("-> opensx.net/sx4");
        alert.getButtonTypes().addAll(openOpensx);
        Optional<ButtonType> option = alert.showAndWait();

        if ((option.isPresent()) && (option.get() == openOpensx)) {
            try {
//                HostServicesDelegate hostServices = HostServicesFactory.getInstance(app);
                app.getHostServices().showDocument("https://opensx.net/sx4");
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }
}
