 /**********************************************************************
 **                                                                   **
 ** This Script has been developed for more StyledText Enrichment     **
 ** December-2006 by proconis GmbH / Germany                          **
 **                                                                   ** 
 ** http://www.proconis.de                                            **
 ** info@proconis.de                                                  **
 **                                                                   **
 **********************************************************************/

package org.pentaho.di.core.widget;

public class UndoRedoStack {

	
	public final static int DELETE =0;
	public final static int INSERT =1;
	
	private String strNewText;
	private String strReplacedText;
	private int iCursorPosition;
	private int iEventLength;
	private int iType;
	
	public UndoRedoStack(int iCursorPosition, String strNewText, String strReplacedText, int iEventLength, int iType){
		this.iCursorPosition = iCursorPosition;
		this.strNewText = strNewText;
		this.strReplacedText = strReplacedText;
		this.iEventLength = iEventLength;
		this.iType = iType;
	}

	public String getReplacedText(){
		return this.strReplacedText;
	}

	public String getNewText(){
		return this.strNewText;
	}
	
	public int getCursorPosition(){
		return this.iCursorPosition;
	}
	
	public int getEventLength(){
		return iEventLength;
	}
	
	public int getType(){
		return iType;
	}
	
}
