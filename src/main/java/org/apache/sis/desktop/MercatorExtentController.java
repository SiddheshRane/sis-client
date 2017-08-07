package org.apache.sis.desktop;

import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Bounds;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.ImageView;
import javafx.scene.input.ScrollEvent;
import org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox;
import org.controlsfx.control.RangeSlider;
import org.controlsfx.control.SnapshotView;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.reactfx.EventStream;
import org.reactfx.EventStreams;
import org.reactfx.util.Tuple4;
import org.reactfx.value.Val;
import org.reactfx.value.Var;

/**
 * FXML Controller class
 *
 * @author Siddhesh Rane
 */
public class MercatorExtentController implements Initializable {

    @FXML
    private ImageView image;
    @FXML
    private SnapshotView snapshot;
    @FXML
    private ScrollPane scrollPane;
    @FXML
    private RangeSlider longitudeRange;
    @FXML
    private RangeSlider latitudeRange;

    private final ObjectProperty<GeographicBoundingBox> extent;

    public void setExtent(GeographicBoundingBox box) {
        extent.set(box);
    }

    public GeographicBoundingBox getExtent() {
        return extent.get();
    }

    public ObjectProperty<GeographicBoundingBox> extentProperty() {
        return extent;
    }

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        snapshot.setSelectionAreaBoundary(SnapshotView.Boundary.NODE);
        scrollPane.addEventFilter(ScrollEvent.SCROLL, se -> {
                              if (se.isControlDown()) {
                                  double imageW = image.getImage().getWidth();
                                  double viewportW = scrollPane.getViewportBounds().getWidth();
                                  double currentW = image.getFitWidth();
                                  currentW = currentW == 0 ? imageW : currentW;
                                  System.out.println("viewportW:" + viewportW + " currentW = " + currentW + " imageW:" + imageW);
                                  double deltaY = se.getDeltaY();
                                  System.out.println("deltaY = " + deltaY);

                                  currentW += deltaY;
//                                  currentW = currentW > imageW ? imageW : currentW;
                                  currentW = currentW < viewportW ? viewportW : currentW;
                                  image.setFitWidth(currentW);

                                  se.consume();
                              }
                          });
//        snapshot.selectionProperty().addListener((observable, oldValue, newValue) -> {
//            if (snapshot.isSelectionActive() && newValue != null) {
//                Bounds imageBounds = image.getBoundsInLocal();
//                double width = imageBounds.getWidth();
//                double height = imageBounds.getHeight();
//                double west = newValue.getMinX() / width * 360 - 180;
//                double east = newValue.getMaxX() / width * 360 - 180;
//                longitudeRange.setLowValue(west);
//                longitudeRange.setHighValue(east);
//
//                double north = newValue.getMinY() / height * -180 + 90;
//                double south = newValue.getMaxY() / height * -180 + 90;
//                latitudeRange.setLowValue(south);
//                latitudeRange.setHighValue(north);
//
//            } else {
//                longitudeRange.setLowValue(-180);
//                longitudeRange.setHighValue(180);
//                latitudeRange.setLowValue(-90);
//                latitudeRange.setHighValue(90);
//            }
//        });

        Var<Rectangle2D> rec2d = Var.fromVal(snapshot.selectionProperty(), (t) -> {
                                         snapshot.setSelection(t);
                                     });

        EventStream<Number> wests = EventStreams.valuesOf(longitudeRange.lowValueProperty());
        EventStream<Number> easts = EventStreams.valuesOf(longitudeRange.highValueProperty());
        EventStream<Number> souths = EventStreams.valuesOf(latitudeRange.lowValueProperty());
        EventStream<Number> norths = EventStreams.valuesOf(latitudeRange.highValueProperty());
        EventStream<Tuple4<Number, Number, Number, Number>> nsew = EventStreams.combine(norths, souths, easts, wests);
        EventStream<Rectangle2D> recs = nsew.map((Tuple4<Number, Number, Number, Number> t) -> {
            double width = image.getBoundsInLocal().getWidth();
            double height = image.getBoundsInLocal().getHeight();
            
            final double n = (t.get1().doubleValue() - 90) / -180 * height;
            final double s = (t.get2().doubleValue() - 90) / -180 * height;
            final double e = (t.get3().doubleValue() + 180) / 360 * width;
            final double w = (t.get4().doubleValue() + 180) / 360 * width;
            return new Rectangle2D(w, n, e - w, s - n);
        });
        EventStream<Rectangle2D> recsWhenNoUserManipulation = recs.conditionOn(snapshot.selectionChangingProperty().not());
        recsWhenNoUserManipulation.feedTo(snapshot.selectionProperty());
        Val.conditionOn(snapshot.selectionProperty(), snapshot.selectionChangingProperty()).values().subscribe((newValue) -> {
               Bounds imageBounds = image.getBoundsInLocal();
                double width = imageBounds.getWidth();
                double height = imageBounds.getHeight();
                double west = newValue.getMinX() / width * 360 - 180;
                double east = newValue.getMaxX() / width * 360 - 180;
                longitudeRange.setLowValue(west);
                longitudeRange.setHighValue(east);

                double north = newValue.getMinY() / height * -180 + 90;
                double south = newValue.getMaxY() / height * -180 + 90;
                latitudeRange.setLowValue(south);
                latitudeRange.setHighValue(north);
        });
    }

    public MercatorExtentController(GeographicBoundingBox bbox) {
        Objects.requireNonNull(bbox, "geographic bounding box must not be null");
        extent = new SimpleObjectProperty<>(bbox);
    }

    public MercatorExtentController() {
        this(new DefaultGeographicBoundingBox(-180, 180, -90, 90));
    }

}
