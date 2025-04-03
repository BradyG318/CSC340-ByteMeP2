//package ClientServer;

import java.io.Serializable;
import java.net.DatagramPacket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

/*
 * UDP Protocol
 */
public class Protocol implements Serializable {
    private String protocolType;

    private InetAddress destinationIP;
    private InetAddress senderIP;

    private Integer destinationPort;
    private Integer senderPort;

    private Integer packetNumber;
    private Double version;     

    private String[] files;
    private String data;

    private DatagramPacket packet;

    /**
     * Creates a packet with the implemented protocol that stores this data into the
     * packet.
     * 
     * @param destinationIP   Packet's destination IP address
     * @param senderIP        Packet's sender IP address
     * @param destinationPort Destination port address
     * @param senderPort      Sender port address
     * @param packetNum       Which number packet is being sent
     * @param files           The data being transfered (files for our purposes)
     */
    public Protocol(InetAddress destinationIP, InetAddress senderIP, Integer destinationPort, Integer senderPort,
            Integer packetNum, String[] files) {
        this.protocolType = "Byte-Me";
        this.version = 1.1;
        this.destinationIP = destinationIP;
        this.senderIP = senderIP;
        this.destinationPort = destinationPort;
        this.senderPort = senderPort;
        this.packetNumber = packetNum;
        this.files = files;

        // packing the data into a compressable string -- protocol type, version, destination ip,
        // sender ip, destination port, sender port, packet #, files
        this.data = this.protocolType + "," + 1.1 + "," + destinationIP.getHostAddress() + "," + senderIP.getHostAddress() + ","
                + destinationPort + "," + senderPort + "," + packetNum;
        for (String file : files) {
            this.data += "," + file;
            // System.out.println(file);
        }

        packet = new DatagramPacket(data.getBytes(), data.getBytes().length, destinationIP, destinationPort);
    }

    /**
     * Creates a packet with the implemented protocol that stores this data into the
     * packet.
     * 
     * @param destinationIP   Packet's destination IP address
     * @param senderIP        Packet's sender IP address
     * @param destinationPort Destination port address
     * @param senderPort      Sender port address
     * @param packetNum       Which number packet is being sent
     * @param files           The data being transfered (files for our purposes)
     */
    public Protocol(InetAddress destinationIP, InetAddress senderIP, Integer destinationPort, Integer senderPort,
            Integer packetNum, String data) {
        this.protocolType = "Byte-Me";
        this.version = 1.1;
        this.destinationIP = destinationIP;
        this.senderIP = senderIP;
        this.destinationPort = destinationPort;
        this.senderPort = senderPort;
        this.packetNumber = packetNum;

        this.files = new String[1];
        this.files[0] = data;

        // packing the data into a compressable string -- protocol type, destination ip,
        // sender ip, destination port, sender port, packet #, files
        this.data = this.protocolType + "," + 1.1 + "," + destinationIP.getHostAddress() + "," + senderIP.getHostAddress() + ","
                + destinationPort + "," + senderPort + "," + packetNum + "," + data;

        // packet form
        packet = new DatagramPacket(data.getBytes(), data.getBytes().length, destinationIP, destinationPort);

    }

    /**
     * Takes in a packet and formats it into the protocol structure if it is this
     * protocol type (OurProtocol/EncryptidsUDP)
     * 
     * @param packet that is an En-cryptid UDP / OurProtocol packet
     */
    public Protocol(DatagramPacket packet) {
        String unloadData = new String(packet.getData(), 0, packet.getLength());
        String[] dataParts = unloadData.split(",");

        for (String i : dataParts) {
            System.out.println(i);
        }

        System.out.println(dataParts[0] + " " + dataParts[1]);

        if (dataParts[0].startsWith("Byte-Me") && dataParts[1].equals("1.1")) {
            this.protocolType = "Byte-Me";


            try {
                this.destinationIP = InetAddress.getByName(dataParts[2]);
                this.senderIP = InetAddress.getByName(dataParts[3]);
            } catch (UnknownHostException e) {
                System.out.println("Error in IP Loading");
                e.printStackTrace();
            }

            this.destinationPort = Integer.parseInt(dataParts[4]);
            this.senderPort = Integer.parseInt(dataParts[5]);

            if (dataParts[6].equals("null") || dataParts[7].isEmpty()) {
                this.packetNumber = -1; // Default value or handle appropriately
            } else {
                this.packetNumber = Integer.parseInt(dataParts[6]);
            }

            List<String> filteredFiles = new ArrayList<>();
            for (int fileIndex = 8; fileIndex < dataParts.length; fileIndex++) {
                filteredFiles.add(dataParts[fileIndex]);
            }
            this.files = filteredFiles.toArray(new String[0]);

            this.packet = packet;
        }
    }

    /**
     * Gets the packet's number
     * 
     * @return Integer of packet number
     */
    public Integer packetNum() {
        return this.packetNumber;
    }

    /**
     * Gets the files/data
     * 
     * @return String of files
     */
    public String files() {
        return (this.files != null) ? String.join(", ", this.files) : "No files available";
    }

    /**
     * Gets the packet in this protocol's format
     * 
     * @return
     */
    public DatagramPacket getPacket() {
        return this.packet;
    }

    /**
     * Prints out the details of this packet's protocol
     */
    public void protocolDetails() {
        // Protocol control
        System.out.println(" | " + "Type:" + "Byte-Me" +  " | " + "Version : 1.1 " + " | " + "Packet #: " + packetNumber + " | ");
        System.out.println(
                "----------------------------------------------------------------------------------------------");
        System.out.println(
                " | " + "Destination IP:" + this.destinationIP + " | " + "Destination Port:" + this.destinationPort
                        + " | " + "Sender IP:" + this.senderIP + " | " + "Sender Port:" + this.senderPort + " | ");
        System.out.println(
                "----------------------------------------------------------------------------------------------");

        // Protocol data
        System.out.println(files());
    }

}
