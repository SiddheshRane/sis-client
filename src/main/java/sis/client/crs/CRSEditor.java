package sis.client.crs;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import org.opengis.metadata.Identifier;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * FXML Controller class
 *
 * @author Siddhesh Rane
 */
public class CRSEditor extends AnchorPane implements Initializable {

    @FXML
    private Label northBound;
    @FXML
    private Label westBound;
    @FXML
    private Label eastBound;
    @FXML
    private Label southBound;
    @FXML
    private TextField crsName;
    @FXML
    private TextField datumName;
    @FXML
    private ComboBox<?> primeMeridian;
    @FXML
    private Spinner<?> semiMajor;
    @FXML
    private Spinner<?> semiMinor;
    @FXML
    private ComboBox<?> coordSystemType;
    @FXML
    private TextArea areaDescription;
    @FXML
    private TableView<?> axesTable;
    @FXML
    private TableColumn<?, ?> axisNameColumn;
    @FXML
    private TableColumn<?, ?> axisUnitColumn;
    @FXML
    private TableColumn<?, ?> axisDirectionColumn;

    private CoordinateReferenceSystem crs;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // TODO
    }

    public CRSEditor(CoordinateReferenceSystem crs) {
        this.crs = crs;

    }
    
    private void setValues(){
        Identifier name = crs.getName();
        if (name != null) {
            crsName.setText(name.toString());
        }
        
        
    }

}
