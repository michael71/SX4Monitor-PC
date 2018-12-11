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

import static de.blankedv.sx4monitorfx.SX4Monitor.INVALID_INT;
import static de.blankedv.sx4monitorfx.SX4Monitor.sxData;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;


/**
 * Control is a generic SX-Data control and has an address selection combobox
 * 
 * @author mblank
 */
public abstract class Control {
    
    public static final int SXMAX_USED = 106;   // maximum SX-channel controlled 

    protected int address;
    protected int data;

    protected int myInstance;
    protected Preferences prefs = Preferences.userNodeForPackage(this.getClass());

    protected final ComboBox cbAddresses = new ComboBox();
    protected final Label lblData = new Label();
    
    protected long lastLocoDataSent = 0L;
    protected int lastData;

    Control(int instance, final String type) {
        myInstance = instance;
  
        final String KEY_ADDR = type + myInstance + "address";
        
        address = prefs.getInt(KEY_ADDR, 1);
        
        // initialize data (after address is read from prefs)
        data = sxData[address];       
        lastData = INVALID_INT;
        lblData.setText("  "+sxData[address]);    
        // init address selection combo box
        List<Integer> addresses = new ArrayList<>();
        for (int i = 0; i <= SXMAX_USED; i++) {
            addresses.add(i);
        }

        cbAddresses.getItems().addAll(addresses);
        cbAddresses.getSelectionModel().select(address);
      

        // set up listener for address changes
        cbAddresses.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newAddress) -> {
                    System.out.println("new Address selected: " + newAddress);
                    String as = "" + newAddress;
                    address = Integer.parseInt(as);
                    data = sxData[address];  // read data for the new address                    
                    lastData = INVALID_INT;  // reset last send data
                    prefs.putInt(KEY_ADDR, address);
                    updateUI();
                });

    }
    
    
    public int getAddress() {
        return address;
    }

    abstract void updateUI();

}
