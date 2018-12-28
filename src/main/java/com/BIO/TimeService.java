package com.BIO;

import java.net.ServerSocket;
import java.net.Socket;

public class TimeService {


    public static void main(String[] args) {
        int port = 8020;
        if (args != null && args.length > 0) {
            try {
                port = Integer.valueOf(args[0]);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        ServerSocket server = null;
        try {
            server = new ServerSocket(port);
            System.out.println("the time server is start in port:" + port);
            Socket socket = null;
            while (true) {
                socket = server.accept();
                new Thread(new TimeServerHandler(socket)).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (server != null) {
                System.out.println("the time server close");
                try {
                    server.close();
                    server = null;
                } catch (Exception e1) {
                    e1.printStackTrace();
                }

            }
        }
    }

}
