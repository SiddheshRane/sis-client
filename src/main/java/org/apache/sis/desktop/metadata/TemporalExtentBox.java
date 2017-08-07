package org.apache.sis.desktop.metadata;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.DatePicker;
import javafx.scene.layout.HBox;
import org.opengis.metadata.extent.TemporalExtent;

/**
 *
 * @author Siddhesh Rane
 */
public class TemporalExtentBox extends HBox implements Initializable {

    @FXML
    private DatePicker start;

    @FXML
    private DatePicker end;

    ObjectProperty<TemporalExtent> extent = new SimpleObjectProperty<>();

    public ObjectProperty<TemporalExtent> valueProperty() {
        return extent;
    }

    public void setValue(TemporalExtent box) {
        valueProperty().set(box);
    }

    public TemporalExtent getValue() {
        return valueProperty().get();
    }

    public TemporalExtentBox(TemporalExtent extent) {
        FXMLLoader loader = new FXMLLoader(GeographicExtentBox.class.getResource("TemporalExtent.fxml"));
        loader.setRoot(this);
        loader.setController(this);
        try {
            loader.load();
        } catch (IOException ex) {
            Logger.getLogger(GeographicExtentBox.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        valueProperty().addListener((observable, oldValue, newValue) -> {
        });
    }

}
