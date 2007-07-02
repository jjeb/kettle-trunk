package org.pentaho.di.www;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.pentaho.di.core.Const;
import org.pentaho.di.core.logging.Log4jStringAppender;
import org.pentaho.di.core.logging.LogWriter;
import org.pentaho.di.core.xml.XMLHandler;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransConfiguration;
import org.pentaho.di.trans.TransExecutionConfiguration;


public class PrepareExecutionTransServlet extends HttpServlet
{
    private static final long serialVersionUID = 3634806745372015720L;
    public static final String CONTEXT_PATH = "/kettle/prepareExec";
    private static LogWriter log = LogWriter.getInstance();
    private TransformationMap transformationMap;
    
    public PrepareExecutionTransServlet(TransformationMap transformationMap)
    {
        this.transformationMap = transformationMap;
    }
    
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        doGet(request, response);
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        if (!request.getContextPath().equals(CONTEXT_PATH)) return;
        
        if (log.isDebug()) log.logDebug(toString(), "Prepare execution of transformation requested");

        String transName = request.getParameter("name");
        boolean useXML = "Y".equalsIgnoreCase( request.getParameter("xml") );

        response.setStatus(HttpServletResponse.SC_OK);
        
        PrintWriter out = response.getWriter();
        if (useXML)
        {
            response.setContentType("text/xml");
            out.print(XMLHandler.getXMLHeader(Const.XML_ENCODING));
        }
        else
        {
            response.setContentType("text/html");
            out.println("<HTML>");
            out.println("<HEAD>");
            out.println("<TITLE>Prepare execution of transformation</TITLE>");
            out.println("<META http-equiv=\"Refresh\" content=\"2;url=/kettle/transStatus?name="+transName+"\">");
            out.println("</HEAD>");
            out.println("<BODY>");
        }
    
        try
        {
            Trans trans = transformationMap.getTransformation(transName);
            TransConfiguration transConfiguration = transformationMap.getConfiguration(transName);
            if (trans!=null && transConfiguration!=null)
            {
                TransExecutionConfiguration executionConfiguration = transConfiguration.getTransExecutionConfiguration();
                // Set the appropriate logging, variables, arguments, replaydate, ...
                // etc.
                log.setLogLevel(executionConfiguration.getLogLevel());
                trans.getTransMeta().setArguments(executionConfiguration.getArgumentStrings());
                trans.setReplayDate(executionConfiguration.getReplayDate());
                trans.setSafeModeEnabled(executionConfiguration.isSafeModeEnabled());
                
                // Log to a String
                Log4jStringAppender appender = LogWriter.createStringAppender();
                log.addAppender(appender);
                transformationMap.addAppender(transName, appender);
                
                if (trans.prepareExecution(null))
                {
                    if (useXML)
                    {
                        out.println(WebResult.OK.getXML());
                    }
                    else
                    {
                        out.println("<H1>Transformation '"+transName+"' was started.</H1>");
                        out.println("<a href=\"/kettle/transStatus?name="+transName+"\">Back to the transformation status page</a><p>");
                    }
                }
                else
                {
                    if (useXML)
                    {
                        out.println(new WebResult(WebResult.STRING_ERROR, "Initialisation of transformation failed: "+Const.CR+appender.getBuffer().toString()));
                    }
                    else
                    {
                        out.println("<H1>Transformation '"+transName+"' was not initialised correctly.</H1>");
                        out.println("<pre>");
                        out.println(appender.getBuffer().toString());
                        out.println("</pre>");
                        out.println("<a href=\"/kettle/transStatus?name="+transName+"\">Back to the transformation status page</a><p>");
                    }
                }
            }
            else
            {
                if (useXML)
                {
                    out.println(new WebResult(WebResult.STRING_ERROR, "The specified transformation ["+transName+"] could not be found"));
                }
                else
                {
                    out.println("<H1>Transformation '"+transName+"' could not be found.</H1>");
                    out.println("<a href=\"/kettle/status\">Back to the status page</a><p>");
                }
            }
        }
        catch (Exception ex)
        {
            if (useXML)
            {
                out.println(new WebResult(WebResult.STRING_ERROR, "Unexpected error during transformation execution preparation:"+Const.CR+Const.getStackTracker(ex)));
            }
            else
            {
                out.println("<p>");
                out.println("<pre>");
                ex.printStackTrace(out);
                out.println("</pre>");
            }
        }

        if (!useXML)
        {
            out.println("<p>");
            out.println("</BODY>");
            out.println("</HTML>");
        }
    }

    public String toString()
    {
        return "Start transformation";
    }
}
