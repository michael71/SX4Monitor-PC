package de.blankedv.sx4monitorfx;

import java.net.*;
import java.util.*;

class NIC {
    
    public static InetAddress getFirstIp() {
        List<InetAddress> addrList = getmyip();
        if (addrList != null) {
            return addrList.get(0);
        } else {
            return null;
        }
    }

    /** 
     * get the ipv4(s) of this machine
     * ignore 127.0.0.1 and virtual interfaces.
     * 
     * @return 
     */
    public static List<InetAddress> getmyip() {

        List<InetAddress> addrList = new ArrayList<>();
        Enumeration<NetworkInterface> interfaces;
        try {
            interfaces = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            System.out.println("ERROR in NIC: " + e.getMessage());
            return null;
        }

        InetAddress localhost;

        try {
            localhost = InetAddress.getByName("127.0.0.1");
        } catch (UnknownHostException e) {
            System.out.println("ERROR in NIC: " + e.getMessage());
            return null;
        }

        while (interfaces.hasMoreElements()) {
            NetworkInterface ifc = interfaces.nextElement();
            //if (DEBUG) System.out.println("Network Interface="+ifc.getName());
            Enumeration<InetAddress> addressesOfAnInterface = ifc.getInetAddresses();

            while (addressesOfAnInterface.hasMoreElements()) {
                InetAddress address = addressesOfAnInterface.nextElement();
                //if (DEBUG) System.out.println("has address=" + address.getHostAddress());
                // look for IPv4 addresses which are not==127.0.0.1
                if (!address.equals(localhost) && !address.toString().contains(":")
                        && (!ifc.getName().contains("vir"))
                        && (!ifc.getName().contains("lxc"))) {
                    addrList.add(address);
                    //if (DEBUG) 
                    System.out.println("not local, not ipv6, not virtual =" + address.getHostAddress());
                    //	System.out.println("FOUND ADDRESS ON NIC: " + address.getHostAddress());

                }
            }
        }
        return addrList;
    }
}
