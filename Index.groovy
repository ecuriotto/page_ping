import java.lang.management.RuntimeMXBean;
import java.lang.management.ManagementFactory;

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import java.text.SimpleDateFormat;
import java.util.logging.Logger;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.lang.Runtime;

import org.json.simple.JSONObject;
import org.codehaus.groovy.tools.shell.CommandAlias;
import org.json.simple.JSONArray;
import org.json.simple.JSONValue;


import javax.naming.Context;
import javax.naming.InitialContext;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import javax.sql.DataSource;
import java.sql.DatabaseMetaData;

import org.apache.commons.lang3.StringEscapeUtils
 
import org.bonitasoft.engine.identity.User;
import org.bonitasoft.console.common.server.page.PageContext
import org.bonitasoft.console.common.server.page.PageController
import org.bonitasoft.console.common.server.page.PageResourceProvider
import org.bonitasoft.engine.exception.AlreadyExistsException;
import org.bonitasoft.engine.exception.BonitaHomeNotSetException;
import org.bonitasoft.engine.exception.CreationException;
import org.bonitasoft.engine.exception.DeletionException;
import org.bonitasoft.engine.exception.ServerAPIException;
import org.bonitasoft.engine.exception.UnknownAPITypeException;

import org.bonitasoft.engine.session.APISession;



public class Index implements PageController {

	private static String pageName="ping";
	private static Logger loggerCustomPage= Logger.getLogger("org.bonitasoft.custompage."+pageName+".groovy");
	
	
	public static class ActionAnswer
	{
		/*
		 * if true, the answer is managed by the action (else, it should be an HTML call)
		 */
		public boolean isManaged=false;
		/*
		 * if true, the response is in responseMap, and a JSON is necessary
		 */
		public boolean isResponseMap=true;
		/*
		 * the response under a Map 
		 */
		public Map<String,Object> responseMap =new HashMap<String,Object>();
		public void setResponse(Map<String,Object> response )
		{
			responseMap = response;
			isResponseMap=true;
		}
		
	}
	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response, PageResourceProvider pageResourceProvider, PageContext pageContext) {
		
		try {
			String requestParamJson= request.getParameter("paramjson");
			String requestParamJsonSt ="";
			try
			{
			    requestParamJsonSt = requestParamJson; // in fact, the request is decoded by the Tomcat Server
			    // requestParamJsonSt = (requestParamJson==null ? null : java.net.URLDecoder.decode(requestParamJson, "UTF-8"));
			}
			catch(Exception e)
			{
			      StringWriter sw = new StringWriter();
		          e.printStackTrace(new PrintWriter(sw));
		          String exceptionDetails = sw.toString();
		          loggerCustomPage.severe("#### "+pageName+":Groovy Exception ["+e.toString()+"] at "+exceptionDetails+" Decode["+requestParamJson+"]");
		          
		          PrintWriter out = response.getWriter()
		          response.setCharacterEncoding("UTF-8");
	              response.addHeader("content-type", "application/json");
	              out.write( "{\"status\":\"BadjsonParam\"} " );
	              out.flush();
	              out.close();
	              return;
			}
			loggerCustomPage.info("#### "+pageName+":Groovy , requestParamJsonSt=["+requestParamJsonSt+"] (source is["+requestParamJson+"])" );
			
			
			Index.ActionAnswer actionAnswer = Actions.doAction( request, requestParamJsonSt,  response, pageResourceProvider, pageContext );
			if (! actionAnswer.isManaged)
			{
				loggerCustomPage.info("#### "+pageName+"Groovy NoAction, return index.html" );
				runTheBonitaIndexDoGet( request, response,pageResourceProvider,pageContext);
				return;
			}
			loggerCustomPage.info("#### "+pageName+":Groovy , ResponseMap="+actionAnswer.responseMap.size() );
			
			if (actionAnswer.responseMap.size()>0)
			{
				response.setCharacterEncoding("UTF-8");
				response.addHeader("content-type", "application/json");
				
				PrintWriter out = response.getWriter()
				String jsonSt = JSONValue.toJSONString( actionAnswer.responseMap );
				out.write( jsonSt );
				loggerCustomPage.info("#### ##############################CustomPage: return json["+jsonSt+"]" );
				out.flush();
				out.close();
				return;
			}
			// assuming the DoAction did the job (export a ZIP file for example)
			loggerCustomPage.info("#### "+pageName+" ##############################CustomPage: AssumingDoAction did the job (export a file)" );
	            
			return;
		} catch (Exception e) {
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			String exceptionDetails = sw.toString();
			loggerCustomPage.severe("#### "+pageName+":Groovy Exception ["+e.toString()+"] at "+exceptionDetails);
		}
	}
	
	/** -------------------------------------------------------------------------
	 *
	 *getIntegerParameter
	 * 
	 */
	 public static getIntegerParameter(HttpServletRequest request, String paramName, Integer defaultValue)
	{
		String valueParamSt = request.getParameter(paramName);
		if (valueParamSt==null  || valueParamSt.length()==0)
		{
			return defaultValue;
		}
		try
		{
			return Integer.valueOf( valueParamSt );
		}
		catch( Exception e)
		{
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			String exceptionDetails = sw.toString();
			
			loggerCustomPage.severe("#### "+pageName+":Groovy : getinteger : Exception "+e.toString()+" on  ["+valueParamSt+"] at "+exceptionDetails );
			return defaultValue;
		}
	}
	/** -------------------------------------------------------------------------
	 *
	 *getBooleanParameter
	 * 
	 */
	public static Boolean getBooleanParameter(HttpServletRequest request, String paramName, Boolean defaultValue)
	{
		String valueParamSt = request.getParameter(paramName);
		if (valueParamSt==null  || valueParamSt.length()==0)
		{
			return defaultValue;
		}
		try
		{
			return  Boolean.valueOf( valueParamSt );
		}
		catch( Exception e)
		{
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			String exceptionDetails = sw.toString();
			
			loggerCustomPage.severe("#### "+pageName+":Groovy : getBoolean : Exception "+e.toString()+" on  ["+valueParamSt+"] at "+exceptionDetails );
			return defaultValue;
		}
	}
	
	/** -------------------------------------------------------------------------
	 *
	 *runTheBonitaIndexDoGet
	 * 
	 */
	private void runTheBonitaIndexDoGet(HttpServletRequest request, HttpServletResponse response, PageResourceProvider pageResourceProvider, PageContext pageContext) {
		try {
				def String indexContent;
				pageResourceProvider.getResourceAsStream("index.html").withStream { InputStream s->
						indexContent = s.getText()
				}
				
				File pageDirectory = pageResourceProvider.getPageDirectory();
				loggerCustomPage.info("#### "+pageName+": pageDirectory st="+pageDirectory.getAbsolutePath() );
				        
				// def String pageResource="pageResource?&page="+ request.getParameter("page")+"&location=";
				// indexContent= indexContent.replace("@_USER_LOCALE_@", request.getParameter("locale"));
				indexContent= indexContent.replace("@_CURRENTTIMEMILIS_@", String.valueOf(System.currentTimeMillis()));
                indexContent= indexContent.replace("@_PAGEDIRECTORY_@", pageDirectory.getAbsolutePath()) ;
				
				response.setCharacterEncoding("UTF-8");
				response.addHeader("content-type", "text/html");
				
				PrintWriter out = response.getWriter();
				out.print(indexContent);
				out.flush();
				out.close();
				loggerCustomPage.info("#### "+pageName+": return index.hml size("+indexContent.length+"]");
				
		} catch (Exception e) {
			loggerCustomPage.severe("#### "+pageName+":Error "+e.toString());
		}
		}

}
