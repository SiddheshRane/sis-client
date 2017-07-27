package sis.client.metadata;

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
import javafx.scene.layout.BorderPane;
import org.opengis.metadata.extent.GeographicBoundingBox;

/**
 *
 * @author Siddhesh Rane
 */
public class GeographicExtentBox extends BorderPane implements Initializable {

    @FXML
    private Spinner<Double> north;

    @FXML
    private Spinner<Double> south;

    @FXML
    private Spinner<Double> west;

    @FXML
    private Spinner<Double> east;

    ObjectProperty<GeographicBoundingBox> extent = new SimpleObjectProperty<>();

    public ObjectProperty<GeographicBoundingBox> valueProperty() {
        return extent;
    }

    public void setValue(GeographicBoundingBox box) {
        valueProperty().set(box);
    }

    public GeographicBoundingBox getValue() {
        return valueProperty().get();
    }

    public GeographicExtentBox(GeographicBoundingBox box) {
        FXMLLoader loader = new FXMLLoader(GeographicExtentBox.class.getResource("GeographicExtent.fxml"));
        loader.setRoot(this);
        loader.setController(this);
        try {
            loader.load();
        } catch (IOException ex) {
            Logger.getLogger(GeographicExtentBox.class.getName()).log(Level.SEVERE, null, ex);
        }
        setValue(box);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        valueProperty().addListener((observable, oldValue, newValue) -> {
            north.getValueFactory().setValue(newValue.getNorthBoundLatitude());
            south.getValueFactory().setValue(newValue.getSouthBoundLatitude());
            west.getValueFactory().setValue(newValue.getWestBoundLongitude());
            east.getValueFactory().setValue(newValue.getEastBoundLongitude());
        });
    }

}
