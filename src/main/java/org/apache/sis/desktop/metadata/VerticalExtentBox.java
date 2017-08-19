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
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.layout.HBox;
import org.apache.sis.metadata.iso.extent.DefaultVerticalExtent;
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

    final InvalidationListener invalidationListener = new InvalidationListener() {
        boolean changeIsLocal;

        @Override
        public void invalidated(Observable observable) {
            if (changeIsLocal) {
                return;
            }
            changeIsLocal = true;
            if (observable == extent) {
                VerticalExtent newValue = extent.get();
                min.getValueFactory().setValue(newValue.getMinimumValue());
                max.getValueFactory().setValue(newValue.getMaximumValue());
            } else {
                DefaultVerticalExtent newExtent = new DefaultVerticalExtent(min.getValue(), max.getValue(), extent.get().getVerticalCRS());
                extent.setValue(newExtent);
            }
            changeIsLocal = false;
        }
    };

    public VerticalExtentBox(VerticalExtent extent) {
        FXMLLoader loader = new FXMLLoader(VerticalExtentBox.class.getResource("VerticalExtent.fxml"));
        loader.setRoot(this);
        loader.setController(this);
        try {
            loader.load();
        } catch (IOException ex) {
            Logger.getLogger(VerticalExtentBox.class.getName()).log(Level.SEVERE, null, ex);
        }
        setValue(extent);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        min.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY));
        max.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY));

        valueProperty().addListener(invalidationListener);
        min.getValueFactory().valueProperty().addListener(invalidationListener);
        max.getValueFactory().valueProperty().addListener(invalidationListener);
    }

}
