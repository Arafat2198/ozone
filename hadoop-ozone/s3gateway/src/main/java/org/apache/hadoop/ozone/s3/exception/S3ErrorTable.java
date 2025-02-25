/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hadoop.ozone.s3.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_CONFLICT;
import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_PRECON_FAILED;
import static java.net.HttpURLConnection.HTTP_NOT_IMPLEMENTED;
import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static org.apache.hadoop.ozone.s3.util.S3Consts.RANGE_NOT_SATISFIABLE;

/**
 * This class represents errors from Ozone S3 service.
 * This class needs to be updated to add new errors when required.
 */
public final class S3ErrorTable {

  private static final Logger LOG = LoggerFactory.getLogger(
      S3ErrorTable.class);

  private S3ErrorTable() {
    //No one should construct this object.
  }

  public static final OS3Exception INVALID_URI = new OS3Exception("InvalidURI",
      "Couldn't parse the specified URI.", HTTP_BAD_REQUEST);

  public static final OS3Exception NO_SUCH_VOLUME = new OS3Exception(
      "NoSuchVolume", "The specified volume does not exist", HTTP_NOT_FOUND);

  public static final OS3Exception NO_SUCH_BUCKET = new OS3Exception(
      "NoSuchBucket", "The specified bucket does not exist", HTTP_NOT_FOUND);

  public static final OS3Exception AUTH_PROTOCOL_NOT_SUPPORTED =
      new OS3Exception("AuthProtocolNotSupported", "Auth protocol used for" +
          " this request is not supported.", HTTP_BAD_REQUEST);

  public static final OS3Exception S3_AUTHINFO_CREATION_ERROR =
      new OS3Exception("InvalidRequest", "Error creating s3 auth info.",
          HTTP_BAD_REQUEST);

  public static final OS3Exception BUCKET_NOT_EMPTY = new OS3Exception(
      "BucketNotEmpty", "The bucket you tried to delete is not empty.",
      HTTP_CONFLICT);

  public static final OS3Exception MALFORMED_HEADER = new OS3Exception(
      "AuthorizationHeaderMalformed", "The authorization header you provided " +
      "is invalid.", HTTP_NOT_FOUND);

  public static final OS3Exception NO_SUCH_KEY = new OS3Exception(
      "NoSuchKey", "The specified key does not exist", HTTP_NOT_FOUND);

  public static final OS3Exception INVALID_ARGUMENT = new OS3Exception(
      "InvalidArgument", "Invalid Argument", HTTP_BAD_REQUEST);

  public static final OS3Exception INVALID_REQUEST = new OS3Exception(
      "InvalidRequest", "Invalid Request", HTTP_BAD_REQUEST);

  public static final OS3Exception INVALID_RANGE = new OS3Exception(
      "InvalidRange", "The requested range is not satisfiable",
      RANGE_NOT_SATISFIABLE);

  public static final OS3Exception NO_SUCH_UPLOAD = new OS3Exception(
      "NoSuchUpload", "The specified multipart upload does not exist. The " +
      "upload ID might be invalid, or the multipart upload might have " +
      "been aborted or completed.", HTTP_NOT_FOUND);

  public static final OS3Exception INVALID_BUCKET_NAME = new OS3Exception(
      "InvalidBucketName", "The specified bucket is not valid.",
      HTTP_BAD_REQUEST);

  public static final OS3Exception INVALID_PART = new OS3Exception(
      "InvalidPart", "One or more of the specified parts could not be found." +
      " The part might not have been uploaded, or the specified entity " +
      "tag might not have matched the part's entity tag.", HTTP_BAD_REQUEST);

  public static final OS3Exception INVALID_PART_ORDER = new OS3Exception(
      "InvalidPartOrder", "The list of parts was not in ascending order. The " +
      "parts list must be specified in order by part number.",
      HTTP_BAD_REQUEST);

  public static final OS3Exception ENTITY_TOO_SMALL = new OS3Exception(
      "EntityTooSmall", "Your proposed upload is smaller than the minimum " +
      "allowed object size. Each part must be at least 5 MB in size, except " +
      "the last part.", HTTP_BAD_REQUEST);

  public static final OS3Exception INTERNAL_ERROR = new OS3Exception(
      "InternalError", "We encountered an internal error. Please try again.",
      HTTP_INTERNAL_ERROR);

  public static final OS3Exception ACCESS_DENIED = new OS3Exception(
      "AccessDenied", "User doesn't have the right to access this " +
      "resource.", HTTP_FORBIDDEN);

  public static final OS3Exception PRECOND_FAILED = new OS3Exception(
      "PreconditionFailed", "At least one of the pre-conditions you " +
      "specified did not hold", HTTP_PRECON_FAILED);
  
  public static final OS3Exception NOT_IMPLEMENTED = new OS3Exception(
      "NotImplemented", "This part of feature is not implemented yet.",
      HTTP_NOT_IMPLEMENTED);

  /**
   * Create a new instance of Error.
   * @param e Error Template
   * @param resource Resource associated with this exception
   * @return creates a new instance of error based on the template
   */
  public static OS3Exception newError(OS3Exception e, String resource) {
    OS3Exception err =  new OS3Exception(e.getCode(), e.getErrorMessage(),
        e.getHttpCode());
    err.setResource(resource);
    if (e.getHttpCode() == HTTP_INTERNAL_ERROR) {
      LOG.error("Internal Error: {}", err.toXml(), e);
    } else if (LOG.isDebugEnabled()) {
      LOG.debug(err.toXml(), e);
    }
    return err;
  }
}
