package org.apache.sis.desktop.metadata;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.BorderPane;
import javafx.util.converter.FormatStringConverter;
import org.apache.sis.measure.AngleFormat;
import org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox;
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
        north.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(-90, 90));
        south.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(-90, 90));
        east.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(-180, 180));
        west.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(-180, 180));

        north.getEditor().setTextFormatter(new TextFormatter(new FormatStringConverter(AngleFormat.getInstance())));
        south.getEditor().setTextFormatter(new TextFormatter(new FormatStringConverter(AngleFormat.getInstance())));
        west.getEditor().setTextFormatter(new TextFormatter(new FormatStringConverter(AngleFormat.getInstance())));
        east.getEditor().setTextFormatter(new TextFormatter(new FormatStringConverter(AngleFormat.getInstance())));

        final InvalidationListener invalidationListener = new InvalidationListener() {
            boolean changeIsLocal;

            @Override
            public void invalidated(Observable observable) {
                if (changeIsLocal) {
                    return;
                }
                changeIsLocal = true;
                if (observable == extent) {
                    GeographicBoundingBox newValue = extent.get();
                    north.getValueFactory().setValue(newValue.getNorthBoundLatitude());
                    south.getValueFactory().setValue(newValue.getSouthBoundLatitude());
                    west.getValueFactory().setValue(newValue.getWestBoundLongitude());
                    east.getValueFactory().setValue(newValue.getEastBoundLongitude());
                } else {
                    DefaultGeographicBoundingBox newExtent = new DefaultGeographicBoundingBox(west.getValue(), east.getValue(), south.getValue(), north.getValue());
                    extent.setValue(newExtent);
                }
                changeIsLocal = false;
            }
        };

        valueProperty().addListener(invalidationListener);
        north.getValueFactory().valueProperty().addListener(invalidationListener);
        south.getValueFactory().valueProperty().addListener(invalidationListener);
        east.getValueFactory().valueProperty().addListener(invalidationListener);
        west.getValueFactory().valueProperty().addListener(invalidationListener);
    }

}
