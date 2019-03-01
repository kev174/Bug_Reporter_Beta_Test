package com.nuigfyp.jaxrs.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpStatus;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.PutObjectRequest;


public class amazonS3Api {

	private static String appId = "AKIAJJS42KSCITPTYU4A"; // Disabled AKIAIOLZTAWRAYO46NHA
	private static String appSecret = "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"; // Disabled "OP/WuxP9zc27hnHLygY/4AZMODCeOTRNDsNaBEna"
	private final static String BUCKETNAME = "bug-reporter-bucket";


	public static boolean deleteFileFromS3(String key) {

		AmazonS3 s3 = setCredentials();		

		try {	
			//if(fileExist) {
				s3.deleteObject(BUCKETNAME, key);
				return true;
			//}	
			
		} catch (AmazonServiceException ase) {
			System.out.println("Caught an AmazonServiceException, which means your request made it "
					+ "to Amazon S3, but was rejected with an error response for some reason.");
			System.out.println("Error Message:    " + ase.getMessage());
			System.out.println("HTTP Status Code: " + ase.getStatusCode());
			System.out.println("AWS Error Code:   " + ase.getErrorCode());
			System.out.println("Error Type:       " + ase.getErrorType());
			System.out.println("Request ID:       " + ase.getRequestId());
		} catch (AmazonClientException ace) {
			System.out.println("Caught an AmazonClientException, which means the client encountered "
					+ "a serious internal problem while trying to communicate with S3, "
					+ "such as not being able to access the network.");
			System.out.println("Error Message: " + ace.getMessage());
		}
		
		return false; 
	}
	
	
	
	public static File getFileFromS3(String key) {

		AmazonS3 s3 = setCredentials();
		InputStream inputStream = null;
		File targetFile = null;

		try {
			
			inputStream = s3.getObject(BUCKETNAME, key).getObjectContent();
			targetFile = File.createTempFile("s3test", "");
			targetFile.deleteOnExit();	
			
			// Without specifying the option to replace existing file, you'll get a FileAlreadyExistsException
			Files.copy(inputStream, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);			
			return targetFile;

		} catch (AmazonServiceException ase) {
			
			if (ase.getStatusCode() == HttpStatus.SC_NOT_FOUND || ase.getStatusCode() == 404) {
				System.out.println("The file was not found, and is a retunr code of 404. " + ase.getStatusCode());
		    }
			
			System.out.println("Caught an AmazonServiceException, which means your request made it "
					+ "to Amazon S3, but was rejected with an error response for some reason.");
			System.out.println("Error Message:    " + ase.getMessage());
			System.out.println("HTTP Status Code: " + ase.getStatusCode());
			System.out.println("AWS Error Code:   " + ase.getErrorCode());
			System.out.println("Error Type:       " + ase.getErrorType());
			System.out.println("Request ID:       " + ase.getRequestId());
			
		} catch (AmazonClientException ace) {
			System.out.println("Caught an AmazonClientException, which means the client encountered "
					+ "a serious internal problem while trying to communicate with S3, "
					+ "such as not being able to access the network.");
			System.out.println("Error Message: " + ace.getMessage());
		} catch (IOException e) {
		} finally {
			try {
				inputStream.close();
			} catch (IOException e) {
			}
		}
	
		return targetFile;
	}

	public void saveFileToS3(File file, String finalFileDirectory) {

		AmazonS3 s3 = setCredentials();

		try {

			s3.putObject(new PutObjectRequest(BUCKETNAME, finalFileDirectory, file));

		} catch (AmazonServiceException ase) {
			System.out.println("Caught an AmazonServiceException, which means your request made it "
					+ "to Amazon S3, but was rejected with an error response for some reason.");
			System.out.println("Error Message:    " + ase.getMessage());
			System.out.println("HTTP Status Code: " + ase.getStatusCode());
			System.out.println("AWS Error Code:   " + ase.getErrorCode());
			System.out.println("Error Type:       " + ase.getErrorType());
			System.out.println("Request ID:       " + ase.getRequestId());
		} catch (AmazonClientException ace) {
			System.out.println("Caught an AmazonClientException, which means the client encountered "
					+ "a serious internal problem while trying to communicate with S3, "
					+ "such as not being able to access the network.");
			System.out.println("Error Message: " + ace.getMessage());
		}
	}

	private static AmazonS3 setCredentials() {

		AWSCredentials credentials = new BasicAWSCredentials(appId, appSecret);
		@SuppressWarnings("deprecation")
		AmazonS3 s3 = new AmazonS3Client(credentials);
		return s3;
		
	}

}
