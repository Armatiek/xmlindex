package nl.armatiek.xmlindex.milton.resource;

import java.util.Date;

import io.milton.http.Auth;
import io.milton.http.LockInfo;
import io.milton.http.LockResult;
import io.milton.http.LockTimeout;
import io.milton.http.LockToken;
import io.milton.http.Request;
import io.milton.http.Request.Method;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.LockedException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.PreConditionFailedException;
import io.milton.http.http11.auth.DigestResponse;
import io.milton.resource.CollectionResource;

public class DirectoryResource extends AbstractResource implements MakeCollectionableResource, 
  PutableResource, CopyableResource, DeletableResource, MoveableResource, PropFindableResource, 
  LockingCollectionResource, GetableResource {

  

}
