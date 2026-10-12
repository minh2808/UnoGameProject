package com.ptit.uno.client.view.fx.util;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Window;
import javafx.util.Duration;
import org.controlsfx.control.Notifications;
import org.controlsfx.control.action.Action;

import org.kordamp.ikonli.javafx.FontIcon;
import javafx.scene.paint.Color;

import java.awt.Toolkit;

/**
 * Tiện ích hiển thị thông báo đồ họa cao cấp sử dụng ControlsFX Notifications.
 * - Triệt tiêu hoàn toàn lỗi "(Not Responding)" do không dùng showAndWait() / modal dialog treo luồng.
 * - Phong cách Cyber Dark đồng bộ 100% với giao diện UNO nhóm 6.
 * - Tự động định vị góc trên bên phải (TOP_RIGHT) của cửa sổ đang hoạt động hoặc màn hình.
 */
public class UnoNotification {

    /**
     * Hiển thị thông báo Lời mời thách đấu với 2 nút hành động: "Vào phòng ngay" và "Từ chối".
     */
    public static void showInvitation(
            Window owner,
            String inviter,
            int roomId,
            String roomName,
            Runnable onAccept,
            Runnable onDecline) {

        Platform.runLater(() -> {
            try {
                Toolkit.getDefaultToolkit().beep();
            } catch (Exception ignored) {}

            StackPane iconPane = createGlowingBadge("fas-gamepad", "#38bdf8", "#1e40af");

            Action acceptAction = new Action("Vào phòng ngay", ae -> {
                dismiss(ae);
                if (onAccept != null) {
                    onAccept.run();
                }
            });
            acceptAction.setStyle(
                    "-fx-background-color: linear-gradient(to bottom, #10b981, #059669);" +
                    "-fx-text-fill: white;" +
                    "-fx-font-weight: 800;" +
                    "-fx-font-size: 13px;" +
                    "-fx-padding: 7px 16px;" +
                    "-fx-background-radius: 6px;" +
                    "-fx-border-color: #34d399;" +
                    "-fx-border-width: 1.2px;" +
                    "-fx-border-radius: 6px;" +
                    "-fx-cursor: hand;"
            );

            Action declineAction = new Action("Từ chối", ae -> {
                dismiss(ae);
                if (onDecline != null) {
                    onDecline.run();
                }
            });
            declineAction.setStyle(
                    "-fx-background-color: linear-gradient(to bottom, #475569, #334155);" +
                    "-fx-text-fill: #f1f5f9;" +
                    "-fx-font-weight: 700;" +
                    "-fx-font-size: 13px;" +
                    "-fx-padding: 7px 14px;" +
                    "-fx-background-radius: 6px;" +
                    "-fx-border-color: #64748b;" +
                    "-fx-border-width: 1px;" +
                    "-fx-border-radius: 6px;" +
                    "-fx-cursor: hand;"
            );

            String message = "Người chơi '" + inviter + "' muốn thách đấu với bạn!\n"
                    + "Phòng: " + roomName + " (#" + String.format("%02d", roomId) + ")\n"
                    + "Tự động đóng sau 20 giây";

            Notifications notify = Notifications.create()
                    .title("LỜI MỜI THÁCH ĐẤU UNO")
                    .text(message)
                    .graphic(iconPane)
                    .position(Pos.TOP_RIGHT)
                    .hideAfter(Duration.seconds(20))
                    .darkStyle()
                    .action(acceptAction, declineAction);

            if (owner != null && owner.isShowing()) {
                notify.owner(owner);
            }

            notify.show();
        });
    }

    /**
     * Hiển thị phản hồi lời mời hoặc thông báo kết quả.
     */
    public static void showFeedback(Window owner, boolean success, String title, String message) {
        Platform.runLater(() -> {
            StackPane iconPane = success
                    ? createGlowingBadge("fas-check-circle", "#34d399", "#065f46")
                    : createGlowingBadge("fas-exclamation-triangle", "#f59e0b", "#78350f");

            Notifications notify = Notifications.create()
                    .title(title)
                    .text(message)
                    .graphic(iconPane)
                    .position(Pos.TOP_RIGHT)
                    .hideAfter(Duration.seconds(6))
                    .darkStyle();

            if (owner != null && owner.isShowing()) {
                notify.owner(owner);
            }

            notify.show();
        });
    }

    public static void showInfo(Window owner, String message) {
        showInfo(owner, "THÔNG BÁO", message);
    }

    public static void showInfo(Window owner, String title, String message) {
        Platform.runLater(() -> {
            StackPane iconPane = createGlowingBadge("fas-info-circle", "#38bdf8", "#1e3a8a");
            Notifications notify = Notifications.create()
                    .title(title)
                    .text(message)
                    .graphic(iconPane)
                    .position(Pos.TOP_RIGHT)
                    .hideAfter(Duration.seconds(5))
                    .darkStyle();

            if (owner != null && owner.isShowing()) {
                notify.owner(owner);
            }
            notify.show();
        });
    }

    public static void showWarning(Window owner, String message) {
        showWarning(owner, "CẢNH BÁO", message);
    }

    public static void showWarning(Window owner, String title, String message) {
        Platform.runLater(() -> {
            StackPane iconPane = createGlowingBadge("fas-exclamation-triangle", "#fbbf24", "#78350f");
            Notifications notify = Notifications.create()
                    .title(title)
                    .text(message)
                    .graphic(iconPane)
                    .position(Pos.TOP_RIGHT)
                    .hideAfter(Duration.seconds(5))
                    .darkStyle();

            if (owner != null && owner.isShowing()) {
                notify.owner(owner);
            }
            notify.show();
        });
    }

    public static void showError(Window owner, String message) {
        showError(owner, "LỖI", message);
    }

    public static void showError(Window owner, String title, String message) {
        Platform.runLater(() -> {
            StackPane iconPane = createGlowingBadge("fas-times-circle", "#f87171", "#7f1d1d");
            Notifications notify = Notifications.create()
                    .title(title)
                    .text(message)
                    .graphic(iconPane)
                    .position(Pos.TOP_RIGHT)
                    .hideAfter(Duration.seconds(6))
                    .darkStyle();

            if (owner != null && owner.isShowing()) {
                notify.owner(owner);
            }
            notify.show();
        });
    }

    private static StackPane createGlowingBadge(String iconLiteral, String borderColor, String bgDark) {
        StackPane badge = new StackPane();
        badge.setStyle(
                "-fx-background-color: radial-gradient(center 50% 50%, radius 70%, " + borderColor + "33, " + bgDark + ");" +
                "-fx-background-radius: 50%;" +
                "-fx-border-color: " + borderColor + ";" +
                "-fx-border-width: 2px;" +
                "-fx-border-radius: 50%;" +
                "-fx-pref-width: 44px;" +
                "-fx-pref-height: 44px;" +
                "-fx-min-width: 44px;" +
                "-fx-min-height: 44px;" +
                "-fx-effect: dropshadow(three-pass-box, " + borderColor + "80, 10, 0, 0, 0);"
        );
        FontIcon icon = new FontIcon(iconLiteral);
        icon.setIconSize(18);
        icon.setIconColor(Color.WHITE);
        badge.getChildren().add(icon);
        return badge;
    }

    private static void dismiss(javafx.event.ActionEvent ae) {
        if (ae != null && ae.getSource() instanceof Node) {
            Node node = (Node) ae.getSource();
            if (node.getScene() != null && node.getScene().getWindow() != null) {
                node.getScene().getWindow().hide();
            }
        }
    }
}
