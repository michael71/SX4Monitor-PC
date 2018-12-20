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


import static de.blankedv.sx4monitorfx.SXnetClientThread.SXMAX;

/**
 *
 * @author mblank
 */
public class Utils {
    // return the right List for the lanbahn address



    public static boolean isValidSXAddress(int a) {
        return ((a >= 0) && (a <= SXMAX));
    }

   
}
