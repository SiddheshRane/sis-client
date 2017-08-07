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
import javafx.scene.control.Spinner;
import javafx.scene.layout.HBox;
import org.opengis.metadata.extent.VerticalExtent;

/**
 *
 * @author Siddhesh Rane
 */
public class VerticalExtentBox extends HBox implements Initializable {

    @FXML
    private Spinner<Double> min;

    @FXML
    private Spinner<Double> max;

    ObjectProperty<VerticalExtent> extent = new SimpleObjectProperty<>();

    public ObjectProperty<VerticalExtent> valueProperty() {
        return extent;
    }

    public void setValue(VerticalExtent box) {
        valueProperty().set(box);
    }

    public VerticalExtent getValue() {
        return valueProperty().get();
    }

    public VerticalExtentBox(VerticalExtent extent) {
        FXMLLoader loader = new FXMLLoader(GeographicExtentBox.class.getResource("VerticalExtent.fxml"));
        loader.setRoot(this);
        loader.setController(this);
        try {
            loader.load();
        } catch (IOException ex) {
            Logger.getLogger(GeographicExtentBox.class.getName()).log(Level.SEVERE, null, ex);
        }
        setValue(extent);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        valueProperty().addListener((observable, oldValue, newValue) -> {
            min.getValueFactory().setValue(newValue.getMinimumValue());
            max.getValueFactory().setValue(newValue.getMaximumValue());
        });
    }

}
