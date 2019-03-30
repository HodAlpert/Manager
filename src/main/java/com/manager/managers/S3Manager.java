package com.manager.managers;/*
 * Copyright 2010-2012 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

import com.manager.common.init;
import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * This sample demonstrates how to make basic requests to Amazon S3 using
 * the AWS SDK for Java.
 * <p>
 * <b>Prerequisites:</b> You must have a valid Amazon Web Services developer
 * account, and be signed up to use Amazon S3. For more information on
 * Amazon S3, see http://aws.amazon.com/s3.
 * <p>
 * <b>Important:</b> Be sure to fill in your AWS access credentials in the
 *                   AwsCredentials.properties file before you try to run this
 *                   sample.
 * http://aws.amazon.com/security-credentials
 */
public class S3Manager extends BaseManager {
    private AmazonS3 s3 = AmazonS3ClientBuilder.standard()
            .withRegion("us-east-2")
            .build();
    private String bucket_name = init.bucket_name;

    public void create_bucket() {
        bucket_name = init.bucket_name;
        String key = null;

        try {
            /*
             * Create a new S3 bucket - Amazon S3 bucket names are globally unique,
             * so once a bucket name has been taken by any user, you can't create
             * another bucket with that same name.
             *
             * You can optionally specify a location for your bucket if you want to
             * keep your data closer to your applications or users.
             */
            logger.fine("Creating bucket " + bucket_name);
            Bucket answer = s3.createBucket(bucket_name);
            logger.fine("returned " + answer);
            /*
             * List the buckets in your account
             */
        }
        catch (AmazonServiceException ase) {
            if (ase.getStatusCode() == 409 && ase.getErrorCode().equals("BucketAlreadyOwnedByYou")) {
                System.out.println("bucket " + bucket_name + "already exist");
                return;
            }
            handle_amazon_service_exception(ase);
        }
            catch (AmazonClientException ace){
                handle_client_exception(ace);
            }
    }
    public List<Bucket> list_buckets(){
        try {
            List<Bucket> answer = s3.listBuckets();
            logger.fine("returned " + answer);
            return answer;
        }
        catch (Exception exc) {
            handle_exception(exc);
            return null;
        }

    }



    public String upload_object(String file_name) throws AmazonClientException {
        /*
         * Upload an object to your bucket - You can easily upload a file to
         * S3, or upload directly an InputStream if you know the length of
         * the data in the stream. You can also specify your own metadata
         * when uploading to S3, which allows you set a variety of options
         * like content-type and content-encoding, plus additional metadata
         * specific to your applications.
         */
        try {
            logger.fine(String.format("Uploading file %s to S3 from directory", file_name));
            File file = new File(file_name);
            String answer = put_file(file);
            logger.fine("returned " + answer);
            return answer;
        }
        catch (Exception exc) {
            handle_exception(exc);
            return null;
        }
    }

    private String put_file(File file) {
        try {
            logger.fine("putting file "+ file.toString() + "in bucket");
            String key = file.getName().replace('\\', '_').replace('/', '_').replace(':', '_');
            PutObjectRequest req = new PutObjectRequest(bucket_name, key, file)
                    .withCannedAcl(CannedAccessControlList.PublicRead);
            PutObjectResult answer = s3.putObject(req);
            logger.fine("returned " + answer);
            return key;
        }
        catch (Exception exc) {
            handle_exception(exc);
            return null;
        }


    }
    public String get_object_url(String key){
        return String.format("https://s3.us-east-2.amazonaws.com/%s/%s", bucket_name, key);
    }

    public void download_file_as_text(String key, String path) {
        /*
         * Download an object - When you download an object, you get all of
         * the object's metadata and a stream from which to read the contents.
         * It's important to read the contents of the stream as quickly as
         * possibly since the data is streamed directly from Amazon S3 and your
         * network connection will remain open until you read all the data or
         * close the input stream.
         *
         * GetObjectRequest also supports several other options, including
         * conditional downloading of objects based on modification times,
         * ETags, and selectively downloading a range of an object.
         */
        try {
            logger.fine("downloading file " + key + "to " + path);
            S3Object object = s3.getObject(new GetObjectRequest(bucket_name, key));
            logger.fine("Content-Type: " + object.getObjectMetadata().getContentType());
            try {
                displayTextInputStream(object.getObjectContent(), path);
                logger.fine("downloaded");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        catch (Exception exc) {
            handle_exception(exc);
        }
    }

    public List<S3ObjectSummary> list_objects(String prefix) {
        /*
         * List objects in your bucket by prefix - There are many options for
         * listing the objects in your bucket.  Keep in mind that buckets with
         * many objects might truncate their results when listing their objects,
         * so be sure to check if the returned object listing is truncated, and
         * use the AmazonS3.listNextBatchOfObjects(...) operation to retrieve
         * additional results.
         */
        try {
            ObjectListing objectListing = s3.listObjects(new ListObjectsRequest()
                    .withBucketName(bucket_name)
                    .withPrefix(prefix));
            List<S3ObjectSummary> answer = new ArrayList<S3ObjectSummary>(objectListing.getObjectSummaries());
            logger.fine("returned; " + answer);
            return answer;
        }
        catch (Exception exc) {
            handle_exception(exc);
            return null;
        }
    }

    public void delete_object(String key) {
        /*
         * Delete an object - Unless versioning has been turned on for your bucket,
         * there is no way to undelete an object, so use caution when deleting objects.
         */
        try {
            logger.fine("deleting file" + key + "from bucket");
            s3.deleteObject(bucket_name, key);
            logger.fine("deleted successfully");
        }
        catch (Exception exc) {
            handle_exception(exc);
        }
    }

    public void empty_bucket() {
        logger.fine("emptying bucket");
        // Delete all objects from the bucket. This is sufficient
        // for unversioned buckets. For versioned buckets, when you attempt to delete objects, Amazon S3 inserts
        // delete markers for all objects, but doesn't delete the object versions.
        // To delete objects from versioned buckets, delete all of the object versions before deleting
        // the bucket (see below for an example).
        try {
            logger.fine("deleting bucket");
            ObjectListing objectListing = s3.listObjects(bucket_name);
            while (true) {
                for (S3ObjectSummary s3ObjectSummary : objectListing.getObjectSummaries()) {
                    s3.deleteObject(bucket_name, s3ObjectSummary.getKey());
                }

                // If the bucket contains many objects, the listObjects() call
                // might not return all of the objects in the first listing. Check to
                // see whether the listing was truncated. If so, retrieve the next page of objects
                // and delete them.
                if (objectListing.isTruncated()) {
                    objectListing = s3.listNextBatchOfObjects(objectListing);
                } else {
                    break;
                }
            }

            // Delete all object versions (required for versioned buckets).
            VersionListing versionList = s3.listVersions(new ListVersionsRequest().withBucketName(bucket_name));
            while (true) {
                for (S3VersionSummary vs : versionList.getVersionSummaries()) {
                    s3.deleteVersion(bucket_name, vs.getKey(), vs.getVersionId());
                }

                if (versionList.isTruncated()) {
                    versionList = s3.listNextBatchOfVersions(versionList);
                } else {
                    break;
                }
            }
        } catch (Exception exc) {
            handle_exception(exc);
        }
    }

    public void delete_bucket() {
        empty_bucket();
        try {
            logger.fine("deleting bucket");
            // After all objects and object versions are deleted, delete the bucket.
            s3.deleteBucket(bucket_name);
        }
        catch (Exception exc) {
            handle_exception(exc);
        }
}

    
    public String get_key(String format){
        return format.replace('\\', '_').replace('/', '_').replace(':', '_');
    }

    /**
     * Displays the contents of the specified input stream as text.
     *
     * @param input The input stream to display as text.
     * @throws IOException
     */
    private static void displayTextInputStream(InputStream input, String path) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(input));
        BufferedWriter out = null;
        try {
            String line = reader.readLine();
            FileWriter fstream = new FileWriter(path, true); //true tells to append data.
            out = new BufferedWriter(fstream);
            while (line != null) {
                out.write(line+ '\n');
                line = reader.readLine();
            }
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

}
