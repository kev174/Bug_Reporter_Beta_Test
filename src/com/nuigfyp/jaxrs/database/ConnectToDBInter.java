package com.nuigfyp.jaxrs.database;

import java.sql.SQLException;
import java.util.List;

import com.nuigfyp.jaxrs.model.Bug;
import com.nuigfyp.jaxrs.model.Credentials;

public interface ConnectToDBInter {
	
	public String changeBugStatus(int id, String todaysDate) throws SQLException;
	public String deleteBugAndFiles(int id) throws SQLException;
	public String updateDB(Bug bug, int id) throws SQLException;
	public Bug searchForBug(int searchId);
	public boolean addEntry(Bug bug) throws SQLException;
	public List<Bug> getAllBugs();
	public List<Credentials> getAllUsersCredentials();
	public String deleteFileIfExists(String s3DeleteThisFile);
	public void deleteDirectroyIfEmpty(String fileDirectory);
}
