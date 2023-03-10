package org.netbeans.modules.xml.wsdl.model.extensions.soap12.validation;

import java.util.HashSet;
import java.util.List;

import org.netbeans.modules.xml.wsdl.model.WSDLModel;
import org.netbeans.modules.xml.xam.Model;
import org.netbeans.modules.xml.xam.Model.State;
import org.netbeans.modules.xml.xam.spi.Validation;
import org.netbeans.modules.xml.xam.spi.ValidationResult;
import org.netbeans.modules.xml.xam.spi.Validator;
import org.netbeans.modules.xml.xam.spi.Validation.ValidationType;


/**
 * @author Sujit Biswas
 *
 */

@org.openide.util.lookup.ServiceProvider(service=org.netbeans.modules.xml.xam.spi.Validator.class)
public class SOAP12ComponentValidator implements Validator {
    
    /** Creates a new instance of SOAPComponentValidator */
    public SOAP12ComponentValidator() {}
    
    /**
     * Returns name of this validation service.
     */
    public String getName() {
        return getClass().getName();
    }
    
    /**
     * Validates given model.
     *
     * @param model model to validate.
     * @param validation reference to the validation context.
     * @param validationType the type of validation to perform
     * @return ValidationResult.
     */
    public ValidationResult validate(Model model, Validation validation,
            ValidationType validationType) {

        // Traverse the model
        if (model instanceof WSDLModel) {
            WSDLModel wsdlModel = (WSDLModel)model;
            
            if (model.getState() == State.NOT_WELL_FORMED) {
                return null;
            }
            SOAP12ComponentVisitor visitor = new SOAP12ComponentVisitor(this, validation);
            visitor.visit(wsdlModel);
            List<ResultItem> resultItems = visitor.getResultItems();
            HashSet<Model> models = new HashSet<Model>();
            models.add(model);
            return new ValidationResult(resultItems, models);
        }
        return null;
    }
}
