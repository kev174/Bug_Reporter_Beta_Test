package com.nuigfyp.jaxrs.database;

import java.io.File;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.apache.log4j.Logger;
import com.nuigfyp.jaxrs.model.Bug;
import com.nuigfyp.jaxrs.model.Credentials;
import com.nuigfyp.jaxrs.service.amazonS3Api;


public class ConnectToDB implements ConnectToDBInter {

	private final static Logger log = Logger.getLogger(ConnectToDB.class);
	private List<Bug> bugList = new ArrayList<>();
	private List<Credentials> credentials = new ArrayList<>();
	private String bugDatabaseName = "bug_reporter";  // filesDatabaseName = "bug_files"; Both used on both DB
	private String credentialsDB = "credentials";
	// NUIG 
	//private String databaseLink = "jdbc:mysql://mysql1.it.nuigalway.ie:3306/mydb2976?autoReconnect=true&useSSL=false";
	//private String un = " ", pw = " ";  
	// GEAR.HOST
	//private String databaseLink = "jdbc:mysql://den1.mysql1:3306/bugfiles?autoReconnect=true&useSSL=false";
	//private String un = "bugfiles", pw = " ";	
	// Amazon AWS RDS
	private String databaseLink = "jdbc:mysql://csitfyp:3306/csitfyp?autoReconnect=true&useSSL=false";
	private String un = "Cusask", pw = "Cusask";
	

	public String changeBugStatus (int id, String todaysDate) throws SQLException { 

		Statement changeStatusBug = null;
		Connection myConnection = DriverManager.getConnection(databaseLink, un, pw);
		
		try {
			
			Class.forName("com.mysql.jdbc.Driver"); 
			myConnection = DriverManager.getConnection(databaseLink, un, pw);

			String updateBugSQLStatement = ("UPDATE " + bugDatabaseName + " SET endDate = '" + todaysDate + "', active = 0 WHERE id = " + id);
			
			myConnection.setAutoCommit(false); // before the prepareStatement below
			changeStatusBug = myConnection.createStatement();
			changeStatusBug.executeUpdate(updateBugSQLStatement);
			myConnection.commit();

			return "Success in Changing Status.";

		} catch (SQLException e) {
			
			log.error("SQLException at ConnectToDB.changeBugStatus(). " + e);

			if (myConnection != null) {
				try { 
					myConnection.rollback();
				} catch (SQLException excep) {
					log.error("SQLException at ConnectToDB.changeBugStatus(). " + excep);
				}
			}
			
			return "Failed in Change Status with id " + id;
			
		} catch (ClassNotFoundException e) {
			log.error("ClassNotFoundException at ConnectToDB.changeBugStatus(). " + e);
		} finally {

			if (changeStatusBug != null) {
				changeStatusBug.close();
			}

			myConnection.setAutoCommit(true);
		}

		return "From changeBugStatus: Changed Status Successfully.";
	}

	
	public String deleteBugAndFiles(int id) throws SQLException { 

		Bug bug = new Bug();
		bug = searchForBug(id);

		if(!bug.getScreenshot().equals("No")) {
			deleteFileIfExists(bug.getScreenshot());
		}
		if(!bug.getDocument().equals("No")) {
			deleteFileIfExists(bug.getDocument());
		}
		

		Statement deleteBug = null;
		Connection myConnection = null;

		try {
			
			Class.forName("com.mysql.jdbc.Driver"); 
			myConnection = DriverManager.getConnection(databaseLink, un, pw);

			String deleteBugStatement = "DELETE FROM " + bugDatabaseName + " WHERE ID = " + id;
			
			myConnection.setAutoCommit(false); // before the prepareStatement below
			deleteBug = myConnection.createStatement();
			deleteBug.executeUpdate(deleteBugStatement);
			myConnection.commit();

			return "Success in Deleting Entry.";

		} catch (SQLException e) {
			log.error("SQLException at ConnectToDB.deleteBugAndFiles(). " + e);

			if (myConnection != null) {
				try {
					myConnection.rollback();
				} catch (SQLException excep) {
					log.error("SQLException at ConnectToDB.deleteBugAndFiles(). " + excep);
				}
			}
			
			return "Failed in Deleting Entry. " + id;
			
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} finally {

			if (deleteBug != null) {
				deleteBug.close();
			}

			myConnection.setAutoCommit(true);
		}

		return "From deleteBugAndFiles: Deleted Successfully.";
	}

	
	public String updateDB(Bug bug, int id) throws SQLException {

		Bug currentBugInDatabase = new Bug();
		currentBugInDatabase = searchForBug(id);
		String currentBugInDatabaseScreenshotDirectory = currentBugInDatabase.getScreenshot();
		String currentBugInDatabaseDocumentDirectory = currentBugInDatabase.getDocument();				
	
		if((!bug.getScreenshot().equals(currentBugInDatabaseScreenshotDirectory))) {
			deleteFileIfExists(currentBugInDatabaseScreenshotDirectory);
		}
		if((!bug.getDocument().equals(currentBugInDatabaseDocumentDirectory))) {
			deleteFileIfExists(currentBugInDatabaseDocumentDirectory);
		}

		
		PreparedStatement bugPreparedStmt = null;
		Connection myConnection = null;

		try {
			Class.forName("com.mysql.jdbc.Driver"); 
			myConnection = DriverManager.getConnection(databaseLink, un, pw);

			String updateBugSQLStatement = ("UPDATE " + bugDatabaseName
					+ " SET id = ?, reporterName = ?, testerName = ?, description = ?, severity = ?, project = ?, screenshot = ?, document = ?, bugClassification = ? WHERE id = " + id);

			myConnection.setAutoCommit(false); 
			bugPreparedStmt = myConnection.prepareStatement(updateBugSQLStatement);

			bugPreparedStmt.setInt(1, bug.getId());
			bugPreparedStmt.setString(2, bug.getReporterName());
			bugPreparedStmt.setString(3, bug.getTesterName());
			bugPreparedStmt.setString(4, bug.getDescription());
			bugPreparedStmt.setInt(5, bug.getSeverity());
			bugPreparedStmt.setInt(6, bug.getProject());
			bugPreparedStmt.setString(7, bug.getScreenshot());
			bugPreparedStmt.setString(8, bug.getDocument());			
			bugPreparedStmt.setInt(9, bug.getBugClassification());
				
			bugPreparedStmt.executeUpdate();		
			
			myConnection.commit();
			
		} catch (Exception e) {
			log.error("General Exception at updateDB(). " + e);
			
			if (myConnection != null) {
				try {
					myConnection.rollback();
				} catch (SQLException ex) {
					log.error("SQLException at ConnectToDB.updateDB(). " + ex);
				}
			}

		} finally {
			if (bugPreparedStmt != null) {
				bugPreparedStmt.close();
			}

			myConnection.setAutoCommit(true);
		}

		return "Updated Entry to Database<BR>Successfully.";
	}
	
	public Bug searchForBug(int searchId) {

		try {
			List<Bug> bugList = getAllBugs();

			for (Bug bug : bugList) {
				if (bug.getId() == searchId) {
					return bug;
				}
			}

		} catch (NumberFormatException e) {
			log.error("NumberFormatException at ConnectToDB.searchForBug(). " + e);
		}

		return null;
	}

	public boolean addEntry(Bug bug) throws SQLException {
		
		@SuppressWarnings("unused")
		int PKGenerated = 0;
		PreparedStatement preparedStmt = null;
		Connection myConnection = null;
		
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm");  
			
		try {
			
			Class.forName("com.mysql.jdbc.Driver"); // this is REQUIRED for a Maven project API
			myConnection = DriverManager.getConnection(databaseLink, un, pw);
			
			String addBugStatement = "INSERT INTO " + bugDatabaseName + " (id, reporterName, testerName, description, severity, project, screenshot, document, startDate, endDate, active, bugClassification)" + " VALUES (null,?,?,?,?,?,?,?,?,?,?,?)";
		
			myConnection.setAutoCommit(false);
			preparedStmt = myConnection.prepareStatement(addBugStatement, PreparedStatement.RETURN_GENERATED_KEYS);

			preparedStmt.setString(1, bug.getReporterName());
			preparedStmt.setString(2, bug.getTesterName());
			preparedStmt.setString(3, bug.getDescription());
			preparedStmt.setInt(4, bug.getSeverity());
			preparedStmt.setInt(5, bug.getProject());
			preparedStmt.setString(6, bug.getScreenshot());
			preparedStmt.setString(7, bug.getDocument());	
			preparedStmt.setString(8, dtf.format(LocalDateTime.now()));
			preparedStmt.setString(9, "WIP.");
			preparedStmt.setInt(10, 1); 
			preparedStmt.setInt(11, bug.getBugClassification());
			preparedStmt.execute();
								
			ResultSet addEntryRS = preparedStmt.getGeneratedKeys();		
			
		    if (addEntryRS.next()) { 
		    	addEntryRS.getInt(1);  
		    }
		    
		    PKGenerated = addEntryRS.getInt(1);		  
			myConnection.commit();   
			
		} catch (SQLException e ) {
			
	        log.error("SQLException at ConnectToDB.addEntry(). " + e);
	        
	        if (myConnection != null) {
	        	
	            try {
	            	
	                myConnection.rollback();
	                
	                return false;
	            } catch(Exception ex) {
	    	        log.error("Exception at ConnectToDB.addEntry(). " + ex);
	            	ex.printStackTrace();
	            	return false;
	            }
	        }
	        
		} catch (ClassNotFoundException e) {
			return false;
		} finally {
	        if (preparedStmt != null) {
	        	preparedStmt.close();
	        }

	        myConnection.setAutoCommit(true);
	    }
		
		return true;
	}
	
	
	public List<Bug> getAllBugs() {
			
		try {

			Class.forName("com.mysql.jdbc.Driver"); 		
			Connection myConnection = DriverManager.getConnection(databaseLink, un, pw);
			Statement connectToDBStatement = myConnection.createStatement();
			ResultSet myResultSet = connectToDBStatement.executeQuery("Select * from " + bugDatabaseName);
					
			while (myResultSet.next()) {
				int a = myResultSet.getInt("id");
				String b = myResultSet.getString("reporterName");
				String c = myResultSet.getString("testerName");
				String d = myResultSet.getString("description");
				int e = myResultSet.getInt("severity");	
				int f = myResultSet.getInt("project");
				String g = myResultSet.getString("screenshot");
				String h = myResultSet.getString("document");
				String i = myResultSet.getString("startDate");
				String j = myResultSet.getString("endDate");
				int k = myResultSet.getInt("active");
				int l = myResultSet.getInt("bugClassification");
				Bug newBug = new Bug(a, b, c, d, e, f, g, h, i, j, k, l);				
				bugList.add(newBug);		 
			}
			
			connectToDBStatement.close();
			myConnection.close();
			
		} catch (Exception e) {
        	log.error("Exception at ConnectToDB.getAllBugs(). " + e);
		}

		return bugList;
	}
	
	
	public List<Credentials> getAllUsersCredentials() {
		
		try {

			Class.forName("com.mysql.jdbc.Driver"); 		
			Connection myConnection = DriverManager.getConnection(databaseLink, un, pw);
			Statement connectToDBStatement = myConnection.createStatement();
			ResultSet myResultSet = connectToDBStatement.executeQuery("Select * from " + credentialsDB);
					
			while (myResultSet.next()) {
				int a = myResultSet.getInt("id");
				String b = myResultSet.getString("fullname");
				String c = myResultSet.getString("username");
				String d = myResultSet.getString("password");
				String e = myResultSet.getString("role");	
				
				Credentials userCredential = new Credentials(a, b, c, d, e);				
				credentials.add(userCredential);			
			}
			
			connectToDBStatement.close();
			myConnection.close();
			
		} catch (Exception e) {
        	log.error("Exception at ConnectToDB.getAllUsersCredentials(). " + e);
		}

		return credentials;
	}
	

	public String deleteFileIfExists(String s3DeleteThisFile) { 

		boolean fileExist = amazonS3Api.deleteFileFromS3(s3DeleteThisFile);
		
		if(fileExist) {
			return ("Successfull in deleteing file " + s3DeleteThisFile);
		} else {
			return ("Failed to delete the file " + s3DeleteThisFile);
		}
	}
	
	
	public void deleteDirectroyIfEmpty(String fileDirectory) { 

		File file = new File(fileDirectory);
		
		if(file.isDirectory()){		
			if(file.list().length < 1){				
				file.delete();
			} else {					
			}				
		} 
	}
	
}
