package sis.client;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.DragEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import sis.client.banner.BannerController;
import sis.client.dnd.DndController;
import sis.client.map.Map;

/**
 * FXML Controller class
 *
 * @author Siddhesh Rane
 */
public class AppController implements Initializable {

    @FXML
    Pane banner;
    @FXML
    BannerController bannerController;
    @FXML
    BorderPane borderPane;

    @FXML
    TabPane tabPane;
    @FXML
    Tab homeTab;
    @FXML
    Tab mapTab;
    @FXML
    Tab fileTab;

    Pane dndPane;
    DndController dndController;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        mapTab.setContent(map);
    }

    public AppController() {
        map = new Map();
    }
    private final Map map;

    @FXML
    void onDragEntered(DragEvent de) {
        if (de.getDragboard().hasFiles() && de.getSource() != tabPane) {
            tabPane.getSelectionModel().select(fileTab);
        }
    }

    @FXML
    void onDragExited(DragEvent de) {
 
    }
}
