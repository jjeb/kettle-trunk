 /**********************************************************************
 **                                                                   **
 **               This code belongs to the KETTLE project.            **
 **                                                                   **
 ** Kettle, from version 2.2 on, is released into the public domain   **
 ** under the Lesser GNU Public License (LGPL).                       **
 **                                                                   **
 ** For more details, please read the document LICENSE.txt, included  **
 ** in this project                                                   **
 **                                                                   **
 ** http://www.kettle.be                                              **
 ** info@kettle.be                                                    **
 **                                                                   **
 **********************************************************************/
 
package org.pentaho.di.trans.steps.fixedinput;

import java.util.List;
import java.util.Map;

import org.eclipse.swt.widgets.Shell;
import org.pentaho.di.core.CheckResult;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.Counter;
import org.pentaho.di.core.annotations.Step;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleStepException;
import org.pentaho.di.core.exception.KettleXMLException;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMeta;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.core.variables.VariableSpace;
import org.pentaho.di.core.xml.XMLHandler;
import org.pentaho.di.repository.Repository;
import org.pentaho.di.resource.ResourceEntry;
import org.pentaho.di.resource.ResourceReference;
import org.pentaho.di.resource.ResourceEntry.ResourceType;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStepMeta;
import org.pentaho.di.trans.step.StepCategory;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepDialogInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;
import org.w3c.dom.Node;



/**
 * @since 2007-07-05
 * @author matt
 * @version 3.0
 */

@Step(name="FixedInput",image="TFI.png",tooltip="BaseStep.TypeTooltipDesc.FixedInput",description="BaseStep.TypeLongDesc.FixedInput",
		category=StepCategory.INPUT)
public class FixedInputMeta extends BaseStepMeta implements StepMetaInterface
{
	private String filename;
	
	private boolean headerPresent;

	private String lineWidth;

	private String bufferSize;
	
	private boolean lazyConversionActive;

	private boolean lineFeedPresent;

	private boolean runningInParallel;

	private FixedFileInputField fieldDefinition[];

	
	public FixedInputMeta()
	{
		super(); // allocate BaseStepMeta
		allocate(0);
	}

	public void loadXML(Node stepnode, List<DatabaseMeta> databases, Map<String, Counter> counters)
		throws KettleXMLException
	{
		readData(stepnode);
	}

	public Object clone()
	{
		Object retval = super.clone();
		return retval;
	}

	public void setDefault() {
		lineWidth = "80"  ;
		headerPresent = true;
		lazyConversionActive=true;
		bufferSize="50000";
		lineFeedPresent=true;
	}
	
	private void readData(Node stepnode) throws KettleXMLException
	{
		try
		{
			filename = XMLHandler.getTagValue(stepnode, "filename");
			lineWidth = XMLHandler.getTagValue(stepnode, "line_width");
			bufferSize  = XMLHandler.getTagValue(stepnode, "buffer_size");
			headerPresent = "Y".equalsIgnoreCase(XMLHandler.getTagValue(stepnode, "header"));
			lineFeedPresent = "Y".equalsIgnoreCase(XMLHandler.getTagValue(stepnode, "line_feed"));
			lazyConversionActive = "Y".equalsIgnoreCase(XMLHandler.getTagValue(stepnode, "lazy_conversion"));
			runningInParallel = "Y".equalsIgnoreCase(XMLHandler.getTagValue(stepnode, "parallel"));

			Node fields = XMLHandler.getSubNode(stepnode, "fields");
			int nrfields = XMLHandler.countNodes(fields, "field");
			
			allocate(nrfields);

			for (int i = 0; i < nrfields; i++)
			{
				Node fnode = XMLHandler.getSubNodeByNr(fields, "field", i);
				fieldDefinition[i] = new FixedFileInputField(fnode);
			}
		}
		catch (Exception e)
		{
			throw new KettleXMLException("Unable to load step info from XML", e);
		}
	}
	
	public void allocate(int nrFields) {
		fieldDefinition = new FixedFileInputField[nrFields];
	}

	public String getXML()
	{
		StringBuffer retval = new StringBuffer();

		retval.append("    " + XMLHandler.addTagValue("filename", filename));
		retval.append("    " + XMLHandler.addTagValue("line_width", lineWidth));
		retval.append("    " + XMLHandler.addTagValue("header", headerPresent));
		retval.append("    " + XMLHandler.addTagValue("buffer_size", bufferSize));
		retval.append("    " + XMLHandler.addTagValue("lazy_conversion", lazyConversionActive));
		retval.append("    " + XMLHandler.addTagValue("line_feed", lineFeedPresent));
		retval.append("    " + XMLHandler.addTagValue("parallel", runningInParallel));

		retval.append("    <fields>" + Const.CR);
		for (int i = 0; i < fieldDefinition.length; i++)
		{
			retval.append(fieldDefinition[i].getXML());
		}
		retval.append("      </fields>" + Const.CR);

		return retval.toString();
	}


	public void readRep(Repository rep, long id_step, List<DatabaseMeta> databases, Map<String, Counter> counters) throws KettleException
	{
		try
		{
			filename = rep.getStepAttributeString(id_step, "filename");
			lineWidth = rep.getStepAttributeString(id_step, "line_width");
			headerPresent = rep.getStepAttributeBoolean(id_step, "header");
			lineFeedPresent = rep.getStepAttributeBoolean(id_step, "line_feed");
			bufferSize = rep.getStepAttributeString(id_step, "buffer_size");
			lazyConversionActive = rep.getStepAttributeBoolean(id_step, "lazy_conversion");
			runningInParallel = rep.getStepAttributeBoolean(id_step, "parallel");
			
			int nrfields = rep.countNrStepAttributes(id_step, "field_name");

			allocate(nrfields);

			for (int i = 0; i < nrfields; i++)
			{
				FixedFileInputField field = new FixedFileInputField();
				
				field.setName( rep.getStepAttributeString(id_step, i, "field_name") );
				field.setType( ValueMeta.getType(rep.getStepAttributeString(id_step, i, "field_type")) );
				field.setFormat( rep.getStepAttributeString(id_step, i, "field_format") );
				field.setCurrency( rep.getStepAttributeString(id_step, i, "field_currency") );
				field.setDecimal( rep.getStepAttributeString(id_step, i, "field_decimal") );
				field.setGrouping( rep.getStepAttributeString(id_step, i, "field_group") );
				field.setWidth( (int) rep.getStepAttributeInteger(id_step, i, "field_width") );
				field.setLength(  (int) rep.getStepAttributeInteger(id_step, i, "field_length") );
				field.setPrecision( (int) rep.getStepAttributeInteger(id_step, i, "field_precision") );
			}
		}
		catch (Exception e)
		{
			throw new KettleException("Unexpected error reading step information from the repository", e);
		}
	}

	public void saveRep(Repository rep, long id_transformation, long id_step) throws KettleException
	{
		try
		{
			rep.saveStepAttribute(id_transformation, id_step, "filename", filename);
			rep.saveStepAttribute(id_transformation, id_step, "line_width", lineWidth);
			rep.saveStepAttribute(id_transformation, id_step, "buffer_size", bufferSize);
			rep.saveStepAttribute(id_transformation, id_step, "header", headerPresent);
			rep.saveStepAttribute(id_transformation, id_step, "lazy_conversion", lazyConversionActive);
			rep.saveStepAttribute(id_transformation, id_step, "line_feed", lineFeedPresent);
			rep.saveStepAttribute(id_transformation, id_step, "parallel", runningInParallel);

			for (int i = 0; i < fieldDefinition.length; i++)
			{
				rep.saveStepAttribute(id_transformation, id_step, i, "field_name", fieldDefinition[i].getName());
				rep.saveStepAttribute(id_transformation, id_step, i, "field_type", ValueMeta.getTypeDesc(fieldDefinition[i].getType()));
				rep.saveStepAttribute(id_transformation, id_step, i, "field_format", fieldDefinition[i].getFormat());
				rep.saveStepAttribute(id_transformation, id_step, i, "field_currency", fieldDefinition[i].getCurrency());
				rep.saveStepAttribute(id_transformation, id_step, i, "field_decimal", fieldDefinition[i].getDecimal());
				rep.saveStepAttribute(id_transformation, id_step, i, "field_group", fieldDefinition[i].getGrouping());
				rep.saveStepAttribute(id_transformation, id_step, i, "field_width", fieldDefinition[i].getWidth());
				rep.saveStepAttribute(id_transformation, id_step, i, "field_length", fieldDefinition[i].getLength());
				rep.saveStepAttribute(id_transformation, id_step, i, "field_precision", fieldDefinition[i].getPrecision());
			}
		}
		catch (Exception e)
		{
			throw new KettleException("Unable to save step information to the repository for id_step=" + id_step, e);
		}
	}
	
	public void getFields(RowMetaInterface rowMeta, String origin, RowMetaInterface[] info, StepMeta nextStep, VariableSpace space) throws KettleStepException
	{
		for (int i=0;i<fieldDefinition.length;i++) {
			FixedFileInputField field = fieldDefinition[i];
			
			ValueMetaInterface valueMeta = new ValueMeta(field.getName(), field.getType());
			valueMeta.setConversionMask(field.getFormat());
			valueMeta.setLength(field.getLength());
			valueMeta.setPrecision(field.getPrecision());
			valueMeta.setConversionMask(field.getFormat());
			valueMeta.setDecimalSymbol(field.getDecimal());
			valueMeta.setGroupingSymbol(field.getGrouping());
			valueMeta.setCurrencySymbol(field.getCurrency());
			if (lazyConversionActive) valueMeta.setStorageType(ValueMetaInterface.STORAGE_TYPE_BINARY_STRING);
			
			// In case we want to convert Strings...
			//
			ValueMetaInterface storageMetadata = (ValueMetaInterface) valueMeta.clone();
			storageMetadata.setType(ValueMetaInterface.TYPE_STRING);
			storageMetadata.setStorageType(ValueMetaInterface.STORAGE_TYPE_BINARY_STRING);
			
			valueMeta.setStorageMetadata(storageMetadata);
			
			valueMeta.setOrigin(origin);
			
			rowMeta.addValueMeta(valueMeta);
		}
	}
	
	public void check(List<CheckResult> remarks, TransMeta transMeta, StepMeta stepinfo, RowMetaInterface prev, String input[], String output[], RowMetaInterface info)
	{
		CheckResult cr;
		if (prev==null || prev.size()==0)
		{
			cr = new CheckResult(CheckResult.TYPE_RESULT_OK, Messages.getString("FixedInputMeta.CheckResult.NotReceivingFields"), stepinfo); //$NON-NLS-1$
			remarks.add(cr);
		}
		else
		{
			cr = new CheckResult(CheckResult.TYPE_RESULT_ERROR, Messages.getString("FixedInputMeta.CheckResult.StepRecevingData",prev.size()+""), stepinfo); //$NON-NLS-1$ //$NON-NLS-2$
			remarks.add(cr);
		}
		
		// See if we have input streams leading to this step!
		if (Const.isEmpty(filename))
		{
			cr = new CheckResult(CheckResult.TYPE_RESULT_ERROR, Messages.getString("FixedInputMeta.CheckResult.NoFilenameSpecified"), stepinfo); //$NON-NLS-1$
			remarks.add(cr);
		}
		else
		{
			cr = new CheckResult(CheckResult.TYPE_RESULT_OK, Messages.getString("FixedInputMeta.CheckResult.FilenameSpecified"), stepinfo); //$NON-NLS-1$
			remarks.add(cr);
		}
	}
	
	public StepDialogInterface getDialog(Shell shell, StepMetaInterface info, TransMeta transMeta, String name)
	{
		return new FixedInputDialog(shell, info, transMeta, name);
	}

	public StepInterface getStep(StepMeta stepMeta, StepDataInterface stepDataInterface, int cnr, TransMeta tr, Trans trans)
	{
		return new FixedInput(stepMeta, stepDataInterface, cnr, tr, trans);
	}
	
	public StepDataInterface getStepData()
	{
		return new FixedInputData();
	}

	/**
	 * @return the filename
	 */
	public String getFilename() {
		return filename;
	}

	/**
	 * @param filename the filename to set
	 */
	public void setFilename(String filename) {
		this.filename = filename;
	}

	/**
	 * @return the bufferSize
	 */
	public String getBufferSize() {
		return bufferSize;
	}

	/**
	 * @param bufferSize the bufferSize to set
	 */
	public void setBufferSize(String bufferSize) {
		this.bufferSize = bufferSize;
	}

	/**
	 * @return true if lazy conversion is turned on: conversions are delayed as long as possible, perhaps to never occur at all.
	 */
	public boolean isLazyConversionActive() {
		return lazyConversionActive;
	}

	/**
	 * @param lazyConversionActive true if lazy conversion is to be turned on: conversions are delayed as long as possible, perhaps to never occur at all.
	 */
	public void setLazyConversionActive(boolean lazyConversionActive) {
		this.lazyConversionActive = lazyConversionActive;
	}

	/**
	 * @return the headerPresent
	 */
	public boolean isHeaderPresent() {
		return headerPresent;
	}

	/**
	 * @param headerPresent the headerPresent to set
	 */
	public void setHeaderPresent(boolean headerPresent) {
		this.headerPresent = headerPresent;
	}

	/**
	 * @return the lineWidth
	 */
	public String getLineWidth() {
		return lineWidth;
	}

	/**
	 * @return the lineFeedPresent
	 */
	public boolean isLineFeedPresent() {
		return lineFeedPresent;
	}

	/**
	 * @param lineWidth the lineWidth to set
	 */
	public void setLineWidth(String lineWidth) {
		this.lineWidth = lineWidth;
	}

	/**
	 * @param lineFeedPresent the lineFeedPresent to set
	 */
	public void setLineFeedPresent(boolean lineFeedPresent) {
		this.lineFeedPresent = lineFeedPresent;
	}

	/**
	 * @return the runningInParallel
	 */
	public boolean isRunningInParallel() {
		return runningInParallel;
	}

	/**
	 * @param runningInParallel the runningInParallel to set
	 */
	public void setRunningInParallel(boolean runningInParallel) {
		this.runningInParallel = runningInParallel;
	}

	/**
	 * @return the fieldDefinition
	 */
	public FixedFileInputField[] getFieldDefinition() {
		return fieldDefinition;
	}

	/**
	 * @param fieldDefinition the fieldDefinition to set
	 */
	public void setFieldDefinition(FixedFileInputField[] fieldDefinition) {
		this.fieldDefinition = fieldDefinition;
	}
	
	@Override
	public List<ResourceReference> getResourceDependencies() {
		 List<ResourceReference> references = super.getResourceDependencies();
		 
		 if (!Const.isEmpty(filename)) {
			 // Add the filename to the references, including a reference to this step meta data.
			 //
			 ResourceReference reference = new ResourceReference(this);
			 reference.getEntries().add( new ResourceEntry(filename, ResourceType.FILE));
		 }
		 return references;
	}
}
