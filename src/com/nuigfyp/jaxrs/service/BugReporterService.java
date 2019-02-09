package com.nuigfyp.jaxrs.service;

import java.io.File;
import java.util.List;
import javax.ws.rs.core.Response;
import com.nuigfyp.jaxrs.model.Bug;
import com.nuigfyp.jaxrs.model.CustomResponse;
import com.sun.jersey.core.header.FormDataContentDisposition;

public interface BugReporterService {

	public Response uploadFile(File uploadedInputStream, FormDataContentDisposition fileDetail) ;
	
	public Response downloadFile(String fn);
	
	public Response addBugReport(Bug bug);
	
	public Response updateBug(String id, Bug bug);
	
	public Response deleteBug(String id);
		
	public Response getAllBugsInDB();

	public Response changeBugStatus(String Id);	
	
	// **** public Bug GETSpecificBugObject(String primaryKey);

	//public void getPerson(int id);
	
	//public List<Bug> getAllBugs();
	
	
	
	/*public CustomResponse deleteBug(String id);
	
	public void getPerson(int id);
	
	public Response getAllBugsInDB();
	
	public List<Bug> getAllBugs();

	//public CustomResponse addBugObject(Bug bug);

	public CustomResponse addBugReport(int id, Bug bug);*/
	
}