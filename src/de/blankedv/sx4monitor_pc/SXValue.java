/*
 * Copyright (C) 2016 mblank
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

import java.util.Comparator;


/**
 * @author mblank
 */
public class SXValue implements Comparator<SXValue>, Comparable<SXValue> {
    int channel;
    int data;
    String bits;
    long tStamp;
    boolean marked;

    SXValue(int c, int d, boolean markNew) {
        channel = c;
        data = d;
        dataToBits();
        tStamp = System.currentTimeMillis();
        marked = markNew;
    }

    SXValue(SXValue sxv) {
        channel = sxv.getChannel();
        data = sxv.getData();
        dataToBits();
        tStamp = 0;
        marked = false;
    }

    public boolean isMarked() {
        return marked;
    }

    public void setMarked(boolean marked) {
        this.marked = marked;
    }

    public int getChannel() {
        return channel;
    }

    public void setChannel(int channel) {
        this.channel = channel;
    }

    public int getData() {
        return data;
    }

    public void setData(int data) {
        this.data = data;
        dataToBits();
    }

    public String getBits() {
        return bits;
    }

    public void setBits(String bits) {
        this.bits = bits;
    }

    private void dataToBits() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            if ((data & (1 << i)) != 0) {
                sb.append('1');
            } else {
                sb.append('0');
            }
        }
        bits = sb.toString();
    }

    public long gettStamp() {
        return tStamp;
    }

    public void settStamp(long tStamp) {
        this.tStamp = tStamp;
    }

    public boolean isOld() {
        return ((System.currentTimeMillis() - tStamp) >= 4000);
    }

    @Override
    public int compare(SXValue o1, SXValue o2) {
        if (o1.getChannel() < o2.getChannel()) {
            return -1;
        } else if (o1.getChannel() > o2.getChannel()) {
            return 1;
        } else {
            return 0;
        }
    }

    ;

    @Override
    public int compareTo(SXValue o) {
        if (channel < o.getChannel()) {
            return -1;
        } else if (channel > o.getChannel()) {
            return 1;
        } else {
            return 0;
        }
    }
}
