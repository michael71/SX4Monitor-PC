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


import javafx.scene.control.Alert;

import static de.blankedv.sx4monitor_pc.SXnetClientThread.SXMAX;

/**
 * @author mblank
 */
public class Utils {
    // return the right List for the lanbahn address


    public static void showInfoAlert(String title, String header, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText(msg);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.showingProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                return;
            }
        });
        alert.show();  // sofort anzeigen
    }


    public static boolean isValidSXAddress(int a) {
        return ((a >= 0) && (a <= SXMAX));
    }


}
