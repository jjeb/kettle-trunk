package org.pentaho.di.job.entry.validator;

import java.util.List;

import org.apache.commons.validator.GenericValidator;
import org.apache.commons.validator.util.ValidatorUtils;
import org.pentaho.di.core.CheckResultInterface;
import org.pentaho.di.core.CheckResultSourceInterface;

/**
 * Fails if the field's value is either <code>null</code>, an empty string, or a string containing only whitespace.
 *
 * @author mlowery
 */
public class NotBlankValidator implements JobEntryValidator {

  public static final NotBlankValidator INSTANCE = new NotBlankValidator();

  private static final String VALIDATOR_NAME = "notBlank"; //$NON-NLS-1$

  public boolean validate(CheckResultSourceInterface source, String propertyName, List<CheckResultInterface> remarks,
      ValidatorContext context) {
    String value = ValidatorUtils.getValueAsString(source, propertyName);
    if (GenericValidator.isBlankOrNull(value)) {
      JobEntryValidatorUtils.addFailureRemark(source, propertyName, VALIDATOR_NAME, remarks, JobEntryValidatorUtils
          .getLevelOnFail(context, VALIDATOR_NAME));
      return false;
    } else {
      return true;
    }
  }

  public String getName() {
    return VALIDATOR_NAME;
  }

}
