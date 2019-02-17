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

import javax.annotation.security.RolesAllowed;
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
import com.nuigfyp.jaxrs.model.CustomResponse;
//import com.nuigfyp.model.Base64Coding;
import com.nuigfyp.jaxrs.database.ConnectToDB;
import com.nuigfyp.jaxrs.model.Bug;
import com.nuigfyp.jaxrs.model.Credentials;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;

import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;

// Spend some time to familiarize yourself with JAX-RS annotations @Path, @PathParam, @POST, @GET, @Consumes and @Produces.
// String Bucket__Name = "bug-reporter-bucket-" + UUID.randomUUID();
@Consumes(MediaType.APPLICATION_JSON)
@Path("/bugs")
public class BugReporterServiceImpl implements BugReporterService {

	private final static Logger log = Logger.getLogger(BugReporterServiceImpl.class);
	public List<Bug> bugList = new ArrayList<>();
	private List<Credentials> credentials = new ArrayList<>();
	private ConnectToDB db;
	private Base64Coding base64;
	private static Map<Long, DateTime> liveSessionsMap = new HashMap<Long, DateTime>();
	private int scheduleSemaphore = 0;
	
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
		//String temp = decodedFileName;
		String[] dirs = decodedFileName.split("/+");			
		String finalFileDirectory = dirs[0] + "/" + dtf.format(LocalDateTime.now()) + "/" + dirs[1];	
		
		if (uploadedInputStream == null || fileDetail == null) {
			return Response.status(400).entity("No").build();			
		}
		
		amazonS3Api as3 = new amazonS3Api();
		as3.saveFileToS3(uploadedInputStream, finalFileDirectory);
    	
		// ======= THIS PART WILL NOT BE REQUIRED WHEN UPLOADING TO S3 BUCKET, AS S3 WILL CREATE THIS.
		// ======= THIS PART SAVES A FILE OBJECT TO MY LOCAL MACHINE C://FilesSsvedOnWebServer//
		/*try {
			createFolderIfNotExists(PRIMARY_WEBSERVICE_DIRECTORY + dirs[0] + "/" + folderNameWithDate);
		} catch (SecurityException se) {
			customResponse.setStatus(false);
			customResponse.setMessage("Can not create destination folder on server.");
			return Response.status(404).entity("No").build();
		}

		try {
			saveToFile(uploadedInputStream, finalFileDirectory);
		} catch (IOException e) {
			customResponse.setStatus(false);
			customResponse.setMessage("Can not save file.");
			return Response.status(404).entity("No").build();
		}*/

        /*CustomResponse res = new CustomResponse();
        res.setStatus(true);
        res.setMessage("File saved to " + finalFileDirectory);*/
        // return Response.status(200).entity("Success" + uploadedFileLocation).build();
		
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
		        response.header("Content-Disposition", "attachment; filename=" + fileForDownload.getName()); // response.header("Content-Disposition", "attachment; filename=file_delete.png");
		        return response.build();
				
			} catch (Exception e) {
				// ---- if returned File is null, then a null pointer exception is caught----
				System.out.println("BugReprterServiceImpl.getFileNamed() Failed to bring File from Amazon S3 Database." + e);
				log.error("General Exception at BugReporterServiceImpl.downloadFile(). " + e);
			}
		}
		
		// ========================================================================================================================
		// This Works when downloading from my local machine i.e. "C:/FilesSavedOnWebServer/NUIG/2018-08-18/07-47-05-msft_word.png" 
		// AWS S3 testing will require the file to exist in its database now and will throw an Exception
		// ========================================================================================================================
		File fileForDownload = new File(fileDir); // if file does not exist in DB then this will throw exception also
        ResponseBuilder response = Response.ok(fileDownloadedFromS3);
        response.header("Content-Disposition", "attachment; filename=" + fileForDownload.getName()); // response.header("Content-Disposition", "attachment; filename=file_delete.png");
        
        return null;  // This retuns null if this service failed to bring down the File form DB. Three lines of code above N/A.
    }
	
	
	@Override // THIS MEANS THAT THE INTERFACE MUST CONTAIN THIS METHOD
	@POST
    @Path("/addBug/{sid}")
	public Response addBugReport(@PathParam("sid") String sid, Bug bug) { 
	
		// -----------------------------------------------------------------------------------------------------------------------
		// Change from customResponse to Response like '/upload' above. customResponse is good when looking for reply from POSTMAN
		// -----------------------------------------------------------------------------------------------------------------------
		CustomResponse customResponse = new CustomResponse();
		db = new ConnectToDB();		
		boolean createdBugEntryInDatabase = false;
		
		
		if (validSessionId(sid)) {
			try {
				createdBugEntryInDatabase = db.addEntry(bug);
			} catch (SQLException e) {
				e.printStackTrace();
				log.error("General Exception at BugReporterServiceImpl.addBugReport(). " + e);
				System.out.println("Failed to Add a Bug.");
				customResponse.setStatus(true);
				customResponse.setMessage("Failed to Add a Bug with reporters name: " + bug.getReporterName());
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
		
		System.out.println("Bug for update has screnshot named " + bug.getScreenshot());
		
		if (validSessionId(sid)) {
			try {
				updateBugEntryInDatabase = db.updateDB(bug, bugId);
			} catch (SQLException e) {
				e.printStackTrace();
				log.error("General Exception at BugReporterServiceImpl.updateBug(). " + e);
				return Response.status(400).entity("Failed to Add a Bug with reporters name: " + bug.getReporterName())
						.build();
			}
		}
		
		return Response.status(200).entity(updateBugEntryInDatabase).build();
	}
	
	
	@Override
	@DELETE
    @Path("/deletebug/{bugid}/{sid}") // @Consumes(MediaType.APPLICATION_JSON) // not required as is in the header above
	@Produces(MediaType.APPLICATION_JSON)
	public Response deleteBug(@PathParam("bugid") String Id, @PathParam("sid") String sid) {
			
		db = new ConnectToDB();
		base64 = new Base64Coding();
		int bugId = Integer.parseInt(base64.decode(Id));
			
		if (validSessionId(sid)) {
			try {
				db.deleteBugAndFiles(bugId);
			} catch (SQLException e) {
				e.printStackTrace();
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
			
		System.out.println("Changed Bug Status API: ID is " + bugId);
		
		if (validSessionId(sid)) {
			try {
				db.changeBugStatus(bugId, todaysDate);
			} catch (SQLException e) {
				e.printStackTrace();
				log.error("General Exception at BugReporterServiceImpl.changeBugStatus(). " + e);
				return Response.status(400).entity("Changed Status successfull: " + bugId).build();
			}
		}
		
		return Response.status(200).entity("Changed Status successfull: " + bugId).build();
	}
	
	
	@Override
	@RolesAllowed("ADMIN")
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
	@Path("/getSessionId/{userLoginInfo}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getSessionId(@PathParam("userLoginInfo") String userLoginInfo) {
		
		// *********** ID contains user:Admin **************
		base64 = new Base64Coding();
		long leftLimit = 1L, rightLimit = 1000000000L, generatedSessionId = 0;
		String decoded = (base64.decode(userLoginInfo));		
		String[] data = decoded.split(":", 2);
		String un = data[0], pw = data[1]; 
		String returnString = "";
		DateTime currentDate = DateTime.now();
		
		String usernameInDB = "", passwordInDB = "";	
		db = new ConnectToDB();		
		credentials = db.getAllUsersCredentials();

		if (credentials.size() > 0) {
			for (Credentials result : credentials) {
				usernameInDB = result.getUsername();
				passwordInDB = result.getPassword();
				if (un.equals(usernameInDB) && pw.equals(passwordInDB)) {
					generatedSessionId = leftLimit + (long) (Math.random() * (rightLimit - leftLimit));
					liveSessionsMap.put(generatedSessionId, currentDate);
					System.out.println("User exists in the Database: " + usernameInDB + ", Password: " + passwordInDB);
					returnString = (base64.encode(generatedSessionId + ":" + currentDate));

					// RUN THE SCHEDULER
					/*if(scheduleSemaphore == 0) {
						ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
						executor.scheduleAtFixedRate(timeRunner, 59, 59, TimeUnit.SECONDS);
						scheduleSemaphore = 1;
					}*/
					
				} else {
					System.out.println("I DONT exist in the Database: ");
					return Response.status(400).entity("No").build();
				}
			}
		} 

		DateTime currentDatePlusFive = currentDate.plusMinutes(5);
		
		if (currentDate.compareTo(currentDatePlusFive) < 1) {
            System.out.println("*CurrentTimePlusFive is AFTER currentTime. So Yes, This is a Valid Session ID..");
        }
		
		return Response.ok(returnString).build();
	}
	
	public boolean validSessionId(String sid) {

		base64 = new Base64Coding();
		String decodedSid = base64.decode(sid);
		long sessionId = Long.valueOf(decodedSid);
		
		if(liveSessionsMap.containsKey(sessionId)) {
			return true;
		}
		
		return false;
		
		/* ITERAGTE THROUGH HASHMAP
		 * for (Entry<Long, DateTime> entry : liveSessionsMap.entrySet()) { Long key =
		 * entry.getKey(); Object value = entry.getValue();
		 * System.out.println("Iterating through Hashmap " + key); }
		 */
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
						
		//---------------------------------------------------------
		// imported GSON in this class and also in the POM.xml file
		// These libraries convert a Bug to Json and back again.
		//---------------------------------------------------------
		Gson gsonBuilder = new GsonBuilder().create();
		String jsonFromBugPojo = gsonBuilder.toJson(bug);
		Gson gson = new Gson();
		Bug aBug = gson.fromJson(jsonFromBugPojo, Bug.class); 

		return aBug;
	}


	/*public List<Bug> getAllBugs() {			
		db = new ConnectToDB();
		bugList = db.getAllBugs();		
		return bugList;
	}*/
    
		
	public static Runnable timeRunner = new Runnable() {
		public void run() {
			System.out.println("I run every 5 Seconds.");
		}
	};

	public void timerDelay() {
		try {
			TimeUnit.SECONDS.sleep(2);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
	}
}