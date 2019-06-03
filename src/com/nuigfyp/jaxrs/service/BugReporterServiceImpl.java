package com.nuigfyp.jaxrs.service;

import java.io.File;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.nuigfyp.jaxrs.database.ConnectToDB;
import com.nuigfyp.jaxrs.model.Bug;
import com.nuigfyp.jaxrs.model.Credentials;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;


@Consumes(MediaType.APPLICATION_JSON)
@Path("/bugs")
public class BugReporterServiceImpl implements BugReporterService {

	private final static Logger log = Logger.getLogger(BugReporterServiceImpl.class);
	private final static int SESSION_DURATION = 5;
	public List<Bug> bugList = new ArrayList<>();
	private List<Credentials> credentials = new ArrayList<>();
	private ConnectToDB db;
	private Base64Coding base64;
	private static Map<Long, DateTime> liveSessionsMap = new HashMap<Long, DateTime>();
	private static ScheduledExecutorService executor;
	private static int scheduleSemaphore = 0;
	
	
	// @Override
	@GET
	@Path("/testAPI")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getAllBugsInDBB() {

		return Response.ok("Test API on server is OK.").build();
	
	}
	
	
	
	@POST
	@Path("/upload")
	@Consumes(MediaType.MULTIPART_FORM_DATA)	
	@Produces(MediaType.APPLICATION_JSON)
    public Response uploadFile(
        @FormDataParam("file") File uploadedInputStream, // This could also be an InputStream
        @FormDataParam("file") FormDataContentDisposition fileDetail) {
				
		base64 = new Base64Coding();
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");  		
		String decodedFileName = base64.decode(fileDetail.getFileName());		
		String[] dirs = decodedFileName.split("/+");			
		String finalFileDirectory = dirs[0] + "/" + dtf.format(LocalDateTime.now()) + "/" + dirs[1];	
		
		if (uploadedInputStream == null || fileDetail == null) {
			return Response.status(400).entity("No").build();			
		}
		
		amazonS3Api as3 = new amazonS3Api();
		as3.saveFileToS3(uploadedInputStream, finalFileDirectory);
		
        return Response.status(200).entity(finalFileDirectory).build();
    }
	
	
	@GET	
	@Path("/getFileNamed/{fn}")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response downloadFile(@PathParam("fn") String fn) {
    
		base64 = new Base64Coding();
		String decodedFileName = base64.decode(fn);

		File file;
		String fileDir ="";
		db = new ConnectToDB();	
		bugList = db.getAllBugs();
		
		for(Bug bug : bugList) {
			file = new File(bug.getScreenshot());
			String scr = file.getName();
			file = new File(bug.getDocument());
			String pdf = file.getName();
			
			if(scr.equals(decodedFileName)) {
				fileDir = bug.getScreenshot();
				break;
			} else if (pdf.equals(decodedFileName)) {
				fileDir = bug.getDocument();
				break;
			} else {
				fileDir = null;
			}
		}
		
		File fileDownloadedFromS3 = null;
		
		if (fileDir != null) {
			try {
		
				fileDownloadedFromS3 = amazonS3Api.getFileFromS3(fileDir);
				File fileForDownload = new File(fileDir); 
		        ResponseBuilder response = Response.ok(fileDownloadedFromS3);
		        response.header("Content-Disposition", "attachment; filename=" + fileForDownload.getName()); 
		        return response.build();
				
			} catch (Exception e) {
				log.error("General Exception at BugReporterServiceImpl.downloadFile(). " + e);
			}
		}
	
		// I dont believe this is required here
		File fileForDownload = new File(fileDir); 
        ResponseBuilder response = Response.ok(fileDownloadedFromS3);
        response.header("Content-Disposition", "attachment; filename=" + fileForDownload.getName()); 
        
        return null;  
    }
	
	
	@Override 
	@POST
    @Path("/addBug/{sid}")
	public Response addBugReport(@PathParam("sid") String sid, Bug bug) { 
	
		db = new ConnectToDB();		
		boolean createdBugEntryInDatabase = false;
			
		if (validSessionId(sid)) {
			try {
				createdBugEntryInDatabase = db.addEntry(bug);
			} catch (SQLException e) {
				log.error("General Exception at BugReporterServiceImpl.addBugReport(). " + e);
				return Response.status(400).entity("Bad Request Error.").build();
			}
		}
		
		if(createdBugEntryInDatabase) {
			return Response.status(200).entity("200").build();
		} else {
			return Response.status(400).entity("Bad Request Error.").build();
		}
	}
	
	
	@PUT
    @Path("/{id}/updateBug/{sid}")
	public Response updateBug(@PathParam("id") String id, @PathParam("sid") String sid, Bug bug) { 
	
		db = new ConnectToDB();	
		base64 = new Base64Coding();				
		int bugId = Integer.parseInt(base64.decode(id));		
		String updateBugEntryInDatabase = "";
				
		if (validSessionId(sid)) {
			try {
				updateBugEntryInDatabase = db.updateDB(bug, bugId);
			} catch (SQLException e) {
				log.error("General Exception at BugReporterServiceImpl.updateBug(). " + e);
				return Response.status(400).entity("Failed to Add a Bug with reporters name: " + bug.getReporterName())
						.build();
			}
		}
		
		return Response.status(200).entity(updateBugEntryInDatabase).build();
	}
	
	
	@Override
	@DELETE
    @Path("/deletebug/{bugid}/{sid}") 
	@Produces(MediaType.APPLICATION_JSON)
	public Response deleteBug(@PathParam("bugid") String Id, @PathParam("sid") String sid) {
			
		db = new ConnectToDB();
		base64 = new Base64Coding();
		int bugId = Integer.parseInt(base64.decode(Id));
			
		if (validSessionId(sid)) {
			try {
				db.deleteBugAndFiles(bugId);
			} catch (SQLException e) {
				log.error("General Exception at BugReporterServiceImpl.deleteBug(). " + e);

				return Response.status(400).entity("400").build();
			}
		}

		return Response.status(200).entity("200").build();
	}
	
	
	@Override
	@PUT
    @Path("/changeBugStatus/{bugid}/{sid}") 
	@Produces(MediaType.APPLICATION_JSON)
	public Response changeBugStatus(@PathParam("bugid") String Id, @PathParam("sid") String sid) {
			
		db = new ConnectToDB();
		base64 = new Base64Coding();
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
		String todaysDate = dtf.format(LocalDateTime.now());		
		int bugId = Integer.parseInt(base64.decode(Id));
					
		if (validSessionId(sid)) {
			try {
				db.changeBugStatus(bugId, todaysDate);
			} catch (SQLException e) {
				log.error("General Exception at BugReporterServiceImpl.changeBugStatus(). " + e);
				return Response.status(400).entity("Changed Status successful: " + bugId).build();
			}
		}
		
		return Response.status(200).entity("Changed Status successfull: " + bugId).build();
	}
	
	
	@Override
	@GET
	@Path("/getAll/{sid}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getAllBugsInDB(@PathParam("sid") String sid) {

		if(validSessionId(sid)) {
			db = new ConnectToDB();
			bugList = db.getAllBugs();	
			return Response.ok(bugList).build();
		} else {
			return Response.status(401).entity("Unauthorized Request").build();
		}
	}
	
	
	@GET
	@Path("/{id}/getSpecificBug")
	@Produces(MediaType.APPLICATION_JSON)
	public Bug getSpecificBug(@PathParam("id") String id) {
		
		base64 = new Base64Coding();			
		int bugId = Integer.parseInt(base64.decode(id));
		
		Bug bug = new Bug();		
		db = new ConnectToDB();	
		bug = db.searchForBug(bugId);

		/*Gson gsonBuilder = new GsonBuilder().create();
		Gson gson = new Gson();
		String bugPojoConvertedToJson = gsonBuilder.toJson(bug);
		Bug aBug = gson.fromJson(bugPojoConvertedToJson, Bug.class);*/ 

		return bug;
	}
    
	
	@GET
	@Path("/getSessionId/{userLoginInfo}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getSessionId(@PathParam("userLoginInfo") String userLoginInfo) {
		
		base64 = new Base64Coding();
		long leftLimit = 1L, rightLimit = 1000000000L, generatedSessionId = 0;
		String decoded = (base64.decode(userLoginInfo));		
		String[] data = decoded.split(":", 3);
		String un = data[0], pw = data[1], user = data[2]; 
		String returnString = "";
		DateTime currentDate = DateTime.now();
		
		String usernameInDB = "", passwordInDB = "", userInDB = "";	
		db = new ConnectToDB();		
		credentials = db.getAllUsersCredentials();
		
		if (credentials.size() > 0) {
			for (Credentials result : credentials) {
				usernameInDB = result.getUsername();
				passwordInDB = result.getPassword();
				userInDB = result.getFullname();
				
				if (un.equals(usernameInDB) && pw.equals(passwordInDB) && user.equals(userInDB)) {
					generatedSessionId = leftLimit + (long) (Math.random() * (rightLimit - leftLimit));
					liveSessionsMap.put(generatedSessionId, currentDate.plusMinutes(SESSION_DURATION - 4)); // ****** CHANGE THIS FOR FYP
					returnString = (base64.encode(generatedSessionId + ":" + currentDate.plusMinutes(SESSION_DURATION)));

					if(scheduleSemaphore == 0) {				
						scheduleSemaphore = 1;
						startSchedular();
					}
					
					return Response.ok(returnString).build();
					
				} 
			}
		} 
		
		return Response.status(400).entity("No").build();
	}
	
	public boolean validSessionId(String sid) {

		base64 = new Base64Coding();
		String decodedSid = base64.decode(sid);
		long sessionId = Long.valueOf(decodedSid);

		if(liveSessionsMap.containsKey(sessionId)) {
			return true;
		}
		
		return false;
	}

		
	public static Runnable checkSessionExpiryDates = new Runnable() {
		public void run() {
			
			try {
				
				DateTime currentDate = DateTime.now();
				
				if(liveSessionsMap.size() > 0) {
					for (Long key : liveSessionsMap.keySet()) {

						DateTime sessionExpiryDate = liveSessionsMap.get(key);

						if (sessionExpiryDate.compareTo(currentDate) < 1)  {
				            //System.out.println("API: CurrentDate is GREATER than SessionExpiryDate. THIS WILL BE REMOVED FROM THE HASHMAP.");
				            liveSessionsMap.remove(key, sessionExpiryDate);
				        } 				
					}
				}
				
				//System.out.println("Size of HashMap is " + liveSessionsMap.size());
				
			} catch (Exception e) {
			}			
		}
	};

	
	public void startSchedular() {
		
		executor = Executors.newScheduledThreadPool(1);
		executor.scheduleAtFixedRate(checkSessionExpiryDates, 15, 15, TimeUnit.SECONDS);
		
	}
	
}