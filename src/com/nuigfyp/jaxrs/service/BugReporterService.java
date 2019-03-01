package com.nuigfyp.jaxrs.service;

import java.io.File;
import javax.ws.rs.core.Response;
import com.nuigfyp.jaxrs.model.Bug;
import com.sun.jersey.core.header.FormDataContentDisposition;


public interface BugReporterService {

	public Response uploadFile(File uploadedInputStream, FormDataContentDisposition fileDetail) ;	
	public Response downloadFile(String fn);
	public Response addBugReport(String sid, Bug bug);
	public Response updateBug(String id, String sid,Bug bug);
	public Response deleteBug(String id, String sid);	
	public Response changeBugStatus(String Id, String sid);	
	public Response getAllBugsInDB(String sid);
	public Bug getSpecificBug(String id);
	public Response getSessionId(String userLoginInfo);
	public boolean validSessionId(String sid);
	
}