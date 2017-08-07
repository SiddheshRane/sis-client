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
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import org.opengis.metadata.Identifier;
import org.opengis.util.InternationalString;

/**
 *
 * @author Siddhesh Rane
 */
public class IdentifierBox extends GridPane implements Initializable {

    @FXML
    private TextField codespace;

    @FXML
    private TextField code;

    @FXML
    private TextField version;

    @FXML
    private TextArea description;

    ObjectProperty<Identifier> indentifier = new SimpleObjectProperty<>();

    public ObjectProperty<Identifier> valueProperty() {
        return indentifier;
    }

    public void setValue(Identifier box) {
        valueProperty().set(box);
    }

    public Identifier getValue() {
        return valueProperty().get();
    }

    public IdentifierBox(Identifier id) {
        FXMLLoader loader = new FXMLLoader(GeographicExtentBox.class.getResource("Identifier.fxml"));
        loader.setRoot(this);
        loader.setController(this);
        try {
            loader.load();
        } catch (IOException ex) {
            Logger.getLogger(GeographicExtentBox.class.getName()).log(Level.SEVERE, null, ex);
        }
        setValue(id);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        valueProperty().addListener((observable, oldValue, newValue) -> {
            String codeString = newValue.getCode();
            String codeSpaceString = newValue.getCodeSpace();
            String versionString = newValue.getVersion();
            InternationalString descriptionString = newValue.getDescription();

            if (codeString != null) {
                code.setText(codeString);
            } else {
                code.setText("");
            }

            if (codeSpaceString != null) {
                codespace.setText(codeString);
            } else {
                codespace.setText("");
            }

            if (versionString != null) {
                version.setText(versionString);
            } else {
                version.setText("");
            }
            if (descriptionString != null) {
                description.setText(descriptionString.toString());
            } else {
                description.setText("");
            }

        });

    }

}
