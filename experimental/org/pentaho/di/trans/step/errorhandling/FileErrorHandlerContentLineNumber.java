package org.pentaho.di.trans.step.errorhandling;

import java.util.Date;

import org.apache.commons.vfs.FileObject;
import org.pentaho.di.trans.step.BaseStep;

import be.ibridge.kettle.core.Const;
import be.ibridge.kettle.core.exception.KettleException;


public class FileErrorHandlerContentLineNumber extends AbstractFileErrorHandler {

	public FileErrorHandlerContentLineNumber(Date date,
			String destinationDirectory, String fileExtension, String encoding, BaseStep baseStep) {
		super(date, destinationDirectory, fileExtension, encoding, baseStep);
	}

	public void handleLineError(long lineNr, String filePart)
			throws KettleException {
		try {
			getWriter(filePart).write(String.valueOf(lineNr));
			getWriter(filePart).write(Const.CR);
		} catch (Exception e) {
			throw new KettleException(Messages.getString("FileErrorHandlerContentLineNumber.Exception.CouldNotCreateWriteLine") + lineNr, //$NON-NLS-1$
					e);

		}
	}

	public void handleNonExistantFile(FileObject file ) {
	}

	public void handleNonAccessibleFile(FileObject file ) {
	}

}
