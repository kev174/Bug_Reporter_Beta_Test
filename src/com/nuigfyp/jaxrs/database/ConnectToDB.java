package com.nuigfyp.jaxrs.database;

import java.io.File;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import javax.swing.JOptionPane;
import org.apache.log4j.Logger;
import com.nuigfyp.jaxrs.model.Bug;
import com.nuigfyp.jaxrs.model.Credentials;
//import com.nuigfyp.jaxrs.model.Credentials;
//import com.nuigfyp.jaxrs.service.BugReporterService;
import com.nuigfyp.jaxrs.service.amazonS3Api;


public class ConnectToDB implements ConnectToDBInter {

	private final static Logger log = Logger.getLogger(ConnectToDB.class);
	private List<Bug> bugList = new ArrayList<>();
	private List<Credentials> credentials = new ArrayList<>();
	private String bugDatabaseName = "bug_reporter";  // filesDatabaseName = "bug_files"; Both used on both DB
	private String credentialsDB = "credentials";
	// NUIG 
	//private String databaseLink = "jdbc:mysql://mysql1.it.nuigalway.ie:3306/mydb2976?autoReconnect=true&useSSL=false";
	//private String un = "mydb2976ck", pw = "fa4nel";  
	// GEAR.HOST
	//private String databaseLink = "jdbc:mysql://den1.mysql1.gear.host:3306/bugfiles?autoReconnect=true&useSSL=false";
	//private String un = "bugfiles", pw = "Cusask!";	
	// Amazon AWS RDS
	private String databaseLink = "jdbc:mysql://csitfyp.czjf7gxlchkh.eu-west-1.rds.amazonaws.com:3306/csitfyp?autoReconnect=true&useSSL=false";
	private String un = "Cusask174", pw = "Cusask174";
	

	public String changeBugStatus (int id, String todaysDate) throws SQLException { 

		/*System.out.println("--Todays date is " + todaysDate);
		timerDelay();*/
		
		Statement changeStatusBug = null;
		Connection myConnection = myConnection = DriverManager.getConnection(databaseLink, un, pw);

		// UPDATE bug_reporter set active = 0 WHERE id = 3		UPDATE bug_reporter set endDate = '2018-12-23', active = 0 WHERE id = 2
		
		try {
			
			Class.forName("com.mysql.jdbc.Driver"); 
			myConnection = DriverManager.getConnection(databaseLink, un, pw);

			//String deleteBugStatement = "DELETE FROM " + bugDatabaseName + " WHERE ID = " + id;
			String updateBugSQLStatement = ("UPDATE " + bugDatabaseName + " SET endDate = '" + todaysDate + "', active = 0 WHERE id = " + id);
			
			myConnection.setAutoCommit(false); // before the prepareStatement below
			changeStatusBug = myConnection.createStatement();
			changeStatusBug.executeUpdate(updateBugSQLStatement);
			myConnection.commit();

			return "Success in Changing Status.";

		} catch (SQLException e) {
			e.printStackTrace();
			log.error("SQLException at ConnectToDB.changeBugStatus(). " + e);

			if (myConnection != null) {
				try { // REMOVE THIS JOPTION PANE AS WILL NOT WORK IN AWS - NO OPTION TO OK MESSAGE
					JOptionPane.showMessageDialog(null, "Delete Transaction is being ROLLED BACK.", "SQL Exception.",
							JOptionPane.INFORMATION_MESSAGE);
					System.err.print("Transaction is being ROLLED BACK.");
					myConnection.rollback();
				} catch (SQLException excep) {
					log.error("SQLException at ConnectToDB.changeBugStatus(). " + excep);
					excep.printStackTrace();
				}
			}
			return "Failed in Change Status with id " + id;
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
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
		
		// add a return = null if (bug == null)  {}
		//File screenshotFileDirectory = new File(bug.getScreenshot());
		File documentFileDirectory = new File(bug.getDocument());	
						
		System.out.println("deleteBugAndFiles(): Document ABSOLUTE PATH directory is " + documentFileDirectory.getAbsolutePath() + ", with bugDocument() = " + bug.getDocument());
		
		if(!bug.getScreenshot().equals("No")) {
			System.out.println(deleteFileIfExists(bug.getScreenshot())); ;
		}
		if(!bug.getDocument().equals("No")) {
			System.out.println(deleteFileIfExists(bug.getDocument()));
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
			e.printStackTrace();
			log.error("SQLException at ConnectToDB.deleteBugAndFiles(). " + e);

			if (myConnection != null) {
				try {
					JOptionPane.showMessageDialog(null, "Delete Transaction is being ROLLED BACK.", "SQL Exception.",
							JOptionPane.INFORMATION_MESSAGE);
					System.err.print("Transaction is being ROLLED BACK.");
					myConnection.rollback();
				} catch (SQLException excep) {
					log.error("SQLException at ConnectToDB.deleteBugAndFiles(). " + excep);
					excep.printStackTrace();
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
		
		System.out.println("ConnectToDB.updateDB(): The UpdatedBug has document directory of " + bug.getDocument() + ". and bug in DB has a directory " + currentBugInDatabaseDocumentDirectory);
		
		// Compare Updated Bug with the Bug in the Database. NOTE: I will have to check also that Bug in DB has not 'No' set. 
		// PROBLEM: If user de-selects a file (delete a file) and selects another file then this could be an issue.
		// For now i will leave the check for 'No' out as if user de-selects and then checks to upload another file > Fail
		// to delete previous file in the DB. 
		// TESTING: With old Bug in DB set to 'No' and you update with new file, This DOES launch @DELETE - amazonS3API.deleteFileFromS3 	
		if((!bug.getScreenshot().equals(currentBugInDatabaseScreenshotDirectory))) {
			System.out.println("The Bug to be updated has a different Screenshot Directory from the Bug currently in the database. Going to delete this file " + currentBugInDatabaseScreenshotDirectory);
			System.out.println(deleteFileIfExists(currentBugInDatabaseScreenshotDirectory));
		}
		if((!bug.getDocument().equals(currentBugInDatabaseDocumentDirectory))) {
			System.out.println("The Bug to be updated has a different Document Directory from the Bug currently in the database. Going to delete this file " + currentBugInDatabaseDocumentDirectory);
			System.out.println(deleteFileIfExists(currentBugInDatabaseDocumentDirectory));
		}

		
		PreparedStatement bugPreparedStmt = null;
		Connection myConnection = null;

		try {
			//Class.forName("com.mysql.jdbc.Driver"); // this is REQUIRED for a Maven project API. // works without it!!
			myConnection = DriverManager.getConnection(databaseLink, un, pw);

			String updateBugSQLStatement = ("UPDATE " + bugDatabaseName
					+ " SET id = ?, reporterName = ?, testerName = ?, description = ?, severity = ?, project = ?, screenshot = ?, document = ?, bugClassification = ? WHERE id = " + id);

			myConnection.setAutoCommit(false); // https://stackoverflow.com/questions/34807294/updating-multiple-tables-in-mysql-using-transactions
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
			e.printStackTrace();
			log.error("General Exception at updateDB(). " + e);
			if (myConnection != null) {
				try {
					JOptionPane.showMessageDialog(null, "ConnectToDB.updateDB(): UPDATE Transaction is being ROLLED BACK!!!",
							"SQL Exception.", JOptionPane.INFORMATION_MESSAGE);
					System.err.print("Transaction is being ROLLED BACK.");
					myConnection.rollback();
				} catch (SQLException ex) {
					ex.printStackTrace();
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
			System.out.println(e);
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
			
			// ------------------------------------------------------------------------------------------------------
			// NOTE: NULL value in SQL statement. All variables are moved down one i.e. reporterName is now index 1
			// and id is not assigned to the Database yet, but will be after preparedStmt.execute(). see next() below
			// ------------------------------------------------------------------------------------------------------
			String addBugStatement = "INSERT INTO " + bugDatabaseName + " (id, reporterName, testerName, description, severity, project, screenshot, document, startDate, endDate, active, bugClassification)" + " VALUES (null,?,?,?,?,?,?,?,?,?,?,?)";
		
			myConnection.setAutoCommit(false);
			preparedStmt = myConnection.prepareStatement(addBugStatement, PreparedStatement.RETURN_GENERATED_KEYS);

			// preparedStmt.setInt(1, bug.getId());
			preparedStmt.setString(1, bug.getReporterName());
			preparedStmt.setString(2, bug.getTesterName());
			preparedStmt.setString(3, bug.getDescription());
			preparedStmt.setInt(4, bug.getSeverity());
			preparedStmt.setInt(5, bug.getProject());
			preparedStmt.setString(6, bug.getScreenshot());
			preparedStmt.setString(7, bug.getDocument());	
			preparedStmt.setString(8, dtf.format(LocalDateTime.now()));
			preparedStmt.setString(9, "WIP.");
			preparedStmt.setInt(10, 1); // Initially creating Bug object to Active (1), 0 means inactive. **********************
			preparedStmt.setInt(11, bug.getBugClassification());
			preparedStmt.execute();
								
			ResultSet addEntryRS = preparedStmt.getGeneratedKeys();		
			
		    if (addEntryRS.next()) { 
		    	addEntryRS.getInt(1);  
		    }
		    
		    PKGenerated = addEntryRS.getInt(1);		   // PKGenerated is the POJO id   
			myConnection.commit();   
			
		} catch (SQLException e ) {
	        e.printStackTrace();
	        log.error("SQLException at ConnectToDB.addEntry(). " + e);
	        if (myConnection != null) {
	        	
	            try {
	            	JOptionPane.showMessageDialog(null, "addEntry(): Add Bug Entry Transaction is being ROLLED BACK.", "SQL Exception.", JOptionPane.INFORMATION_MESSAGE);
	            	
	            	// NOTICE rollback method below. Leave in for future reference
	                myConnection.rollback();
	                
	                return false;
	            } catch(Exception ex) {
	    	        log.error("Exception at ConnectToDB.addEntry(). " + ex);
	            	ex.printStackTrace();
	            	return false;
	            }
	        }
	        
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
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

			Class.forName("com.mysql.jdbc.Driver"); // this is REQUIRED for a Maven project API			
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
				//InputStream example = myResultSet.getBinaryStream("screenshot"); // Not Causing issue in Maven
				Bug newBug = new Bug(a, b, c, d, e, f, g, h, i, j, k, l);				
				bugList.add(newBug);		 // Checkout Arraylist in SMT Backend.dao.imp.AlertsDAOImp. pom.xml		
			}
			
			connectToDBStatement.close();
			myConnection.close();
			
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Failure to getAllBugs in ConnectToDB.getAllBugs().");
        	JOptionPane.showMessageDialog(null, "getAllBugs(): Get all bugs exception. ", "General Exception.", JOptionPane.INFORMATION_MESSAGE);
        	log.error("Exception at ConnectToDB.getAllBugs(). " + e);
        	// When this occurs the Ajax keeps rotating and users can't access the app. return a null value and from BugReporterServiceImpl 
        	// do two Responses with one ok and the other fail.
		}

		return bugList;
	}
	
	
	public List<Credentials> getAllUsersCredentials() {
		
		try {

			Class.forName("com.mysql.jdbc.Driver"); // this is REQUIRED for a Maven project API			
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
				credentials.add(userCredential);		 // Checkout Arraylist in SMT Backend.dao.imp.AlertsDAOImp. pom.xml		
			}
			
			connectToDBStatement.close();
			myConnection.close();
			
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Failure in ConnectToDB.getAllUsersCredentials().");
        	JOptionPane.showMessageDialog(null, "getAllUsersCredentials(): getAllUsersCredentials exception. ", "General Exception.", JOptionPane.INFORMATION_MESSAGE);
        	log.error("Exception at ConnectToDB.getAllUsersCredentials(). " + e);
        	// When this occurs the Ajax keeps rotating and users can't access the app. return a null value and from BugReporterServiceImpl 
        	// do two Responses with one ok and the other fail.
		}

		return credentials;
	}
	
	
	// Dont think this is used at all
	/*public Map<Integer, Bug> getBugMap() {
		
		Map<Integer, Bug> bugMap = new HashMap<Integer, Bug>();
		
		try {

			Class.forName("com.mysql.jdbc.Driver"); // this is REQUIRED for a Maven project API			
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
				//InputStream example = myResultSet.getBinaryStream("screenshot"); // Not Causing issue in Maven
				Bug newBug = new Bug(a, b, c, d, e, f, g, h, i, j, k, l);					
				bugMap.put(newBug.getId(), newBug);			
			}
			
			connectToDBStatement.close();
			myConnection.close();
			
		} catch (Exception e) {
			e.printStackTrace();
			log.error("Exception at ConnectToDB.getBugMap(). " + e);
			System.out.println("Failure to getBugMap in ConnectToDB.getAllBugs().");
        	JOptionPane.showMessageDialog(null, "getBugMap(): Get all bugs exception. ", "General Exception.", JOptionPane.INFORMATION_MESSAGE);
		}

		return bugMap;
	}*/
	

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
				System.out.println("Directory is empty!. going to delete this directory.");	
				file.delete();
			}else{					
				System.out.println("Directory is not empty!. Size is " + file.list().length + ". Will not delete this directory.");					
			}				
		} else {				
			System.out.println("This is not a directory");	// N/A???		
		}
	}
	
	
	public static void timerDelay() {
		try {
			TimeUnit.SECONDS.sleep(3);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
	}
}
