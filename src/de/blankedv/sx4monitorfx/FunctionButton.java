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
package de.blankedv.sx4monitorfx;

import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

/**
 *
 * @author mblank
 */
public class FunctionButton extends Button {

    private int bitmask;
    private boolean isOn;
    private ImageView ivOn, ivOff;
    private boolean useImage = false;


    FunctionButton(int mask, ImageView on, ImageView off) {
        super("", on);
        bitmask = mask;
        ivOn = on;
        ivOff = off;
        isOn = false;
        useImage = true;
   }

    FunctionButton(int mask, String name) {
        super("name");
        bitmask = mask;
        useImage = false;
        isOn = false;
        setText(name);
        setStyle("-fx-font-size: 2em; -fx-text-fill: #888800");

    }

    public void setState(boolean onState) {
        isOn = onState;
        if (useImage) {
            if (isOn) {
                setGraphic(ivOn);
            } else {
                setGraphic(ivOff);
            }
        } else {
            if (isOn) {
                 setStyle("-fx-font-size: 2em; -fx-text-fill: #888800");
            } else {
                  setStyle("-fx-font-size: 2em; -fx-text-fill: #333333");
            }
        }
    }

    public int toggle(int data) {  // sx bit
        boolean f = ((data & bitmask) != 0);
        if (f) {
            data &= ~(bitmask);
        } else {
            data |= bitmask;
        }
        setState(!f);
        return data;
    }

    public void update(int data) {
        if (useImage) {
            if (((data & bitmask) != 0)) {
                setGraphic(ivOn);
            } else {
                setGraphic(ivOff);
            }
        } else {
            if (((data & bitmask) != 0)) {
                  setStyle("-fx-font-size: 2em; -fx-text-fill: #888800");
            } else {
                  setStyle("-fx-font-size: 2em; -fx-text-fill: #333333");
            }
        }
    }

}
