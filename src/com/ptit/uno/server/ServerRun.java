package com.ptit.uno.server;

import com.ptit.uno.server.control.ServerControl;
import com.ptit.uno.server.view.ServerDashboardFrm;

import javax.swing.SwingUtilities;

/**
 * Lớp khởi động phía Server (ServerRun).
 */
public class ServerRun {
    public static final int DEFAULT_PORT = 8888;

    public static void main(String[] args) {
        // Khởi động giao diện quản lý Server
        SwingUtilities.invokeLater(() -> {
            ServerDashboardFrm view = new ServerDashboardFrm(DEFAULT_PORT);
            new ServerControl(view, DEFAULT_PORT);
            view.setVisible(true);
        });
    }
}
