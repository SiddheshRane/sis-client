package org.apache.sis.desktop;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import org.opengis.feature.Feature;
import com.esri.core.geometry.Point;
import java.text.DecimalFormat;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.util.converter.FormatStringConverter;
import org.apache.sis.measure.AngleFormat;
import org.apache.sis.measure.Latitude;
import org.apache.sis.measure.Longitude;

/**
 * FXML Controller class
 *
 * @author Siddhesh Rane
 */
public class WaypointTableController implements Initializable {

    @FXML
    private TableView<Feature> table;
    @FXML
    private TableColumn<Feature, String> name;
    @FXML
    private TableColumn<Feature, Latitude> latitude;
    @FXML
    private TableColumn<Feature, Longitude> longitude;
    @FXML
    private TableColumn<Feature, Double> elevation;
    @FXML
    private TableColumn<Feature, ?> time;
    @FXML
    private TableColumn<Feature, String> description;
    private final ObservableList<Feature> waypoints = FXCollections.observableArrayList();

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        table.setItems(waypoints);
        name.setCellValueFactory((param) -> {
            return new SimpleStringProperty(param.getValue().getPropertyValue("name").toString());
        });
        name.setCellFactory(a -> new TextFieldTableCell<>());
        latitude.setCellFactory(a -> new TextFieldTableCell<>(new FormatStringConverter(AngleFormat.getInstance())));
        latitude.setCellValueFactory((param) -> {
            Feature f = param.getValue();
            Object val = f.getPropertyValue("sis:geometry");
            if (val instanceof Point) {
                Point p = (Point) val;
                return new SimpleObjectProperty<>(new Latitude(p.getY()));
            }
            return new SimpleObjectProperty<>(null);
        });
        longitude.setCellFactory(a -> new TextFieldTableCell<>(new FormatStringConverter(AngleFormat.getInstance())));
        longitude.setCellValueFactory((param) -> {
            Feature f = param.getValue();
            Object val = f.getPropertyValue("sis:geometry");
            if (val instanceof Point) {
                Point p = (Point) val;
                return new SimpleObjectProperty<>(new Longitude(p.getX()));
            }
            return new SimpleObjectProperty<>(null);
        });
        elevation.setCellValueFactory((param) -> {
            Double val = (Double) param.getValue().getPropertyValue("ele");
            return new SimpleObjectProperty<>(val);
        });
    }

    public ObservableList<Feature> getWaypoints() {
        return waypoints;
    }

}
