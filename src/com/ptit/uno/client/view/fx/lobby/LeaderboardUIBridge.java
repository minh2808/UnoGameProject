package com.ptit.uno.client.view.fx.lobby;

import com.ptit.uno.model.User;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.util.List;

public class LeaderboardUIBridge {

    @FXML private TableView<LeaderboardRow> tblLeaderboard;
    @FXML private TableColumn<LeaderboardRow, Integer> colRank;
    @FXML private TableColumn<LeaderboardRow, String> colName;
    @FXML private TableColumn<LeaderboardRow, Integer> colElo;
    @FXML private TableColumn<LeaderboardRow, Integer> colWins;
    @FXML private TableColumn<LeaderboardRow, Integer> colTotal;
    
    @FXML private Button btnClose;
    @FXML private Label lblCloseIcon;

    private ObservableList<LeaderboardRow> listData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colRank.setCellValueFactory(new PropertyValueFactory<>("rank"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colElo.setCellValueFactory(new PropertyValueFactory<>("elo"));
        colWins.setCellValueFactory(new PropertyValueFactory<>("wins"));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
        
        tblLeaderboard.setItems(listData);

        btnClose.setOnAction(e -> closeWindow());
        lblCloseIcon.setOnMouseClicked(e -> closeWindow());
    }

    private void closeWindow() {
        Stage stage = (Stage) btnClose.getScene().getWindow();
        if (stage != null) {
            stage.close();
        }
    }

    public void renderLeaderboard(List<User> list) {
        listData.clear();
        if (list != null) {
            int rank = 1;
            for (User u : list) {
                listData.add(new LeaderboardRow(
                        rank++,
                        u.getUsername(),
                        u.getScore(),
                        u.getWinMatches(),
                        u.getTotalMatches()
                ));
            }
        }
    }

    public static class LeaderboardRow {
        private final SimpleIntegerProperty rank;
        private final SimpleStringProperty name;
        private final SimpleIntegerProperty elo;
        private final SimpleIntegerProperty wins;
        private final SimpleIntegerProperty total;

        public LeaderboardRow(int rank, String name, int elo, int wins, int total) {
            this.rank = new SimpleIntegerProperty(rank);
            this.name = new SimpleStringProperty(name);
            this.elo = new SimpleIntegerProperty(elo);
            this.wins = new SimpleIntegerProperty(wins);
            this.total = new SimpleIntegerProperty(total);
        }

        public int getRank() { return rank.get(); }
        public String getName() { return name.get(); }
        public int getElo() { return elo.get(); }
        public int getWins() { return wins.get(); }
        public int getTotal() { return total.get(); }
    }
}
