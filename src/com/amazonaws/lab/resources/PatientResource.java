package com.amazonaws.lab.resources;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.dstu3.model.Bundle.BundleLinkComponent;
import org.hl7.fhir.dstu3.model.Bundle.BundleType;
import org.hl7.fhir.dstu3.model.Enumerations.ResourceType;
import org.hl7.fhir.dstu3.model.HumanName;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Narrative;
import org.hl7.fhir.dstu3.model.Narrative.NarrativeStatus;
import org.hl7.fhir.dstu3.model.OperationOutcome;
import org.hl7.fhir.dstu3.model.OperationOutcome.IssueSeverity;
import org.hl7.fhir.dstu3.model.OperationOutcome.IssueType;
import org.hl7.fhir.dstu3.model.OperationOutcome.OperationOutcomeIssueComponent;
import org.hl7.fhir.dstu3.model.Patient;

import com.amazonaws.lab.LambdaHandler;
import com.amazonaws.serverless.proxy.internal.jaxrs.AwsProxySecurityContext.CognitoUserPoolPrincipal;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.document.AttributeUpdate;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Index;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.KeyAttribute;
import com.amazonaws.services.dynamodbv2.document.QueryOutcome;
import com.amazonaws.services.dynamodbv2.document.RangeKeyCondition;
import com.amazonaws.services.dynamodbv2.document.ScanOutcome;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import com.amazonaws.services.dynamodbv2.document.spec.ScanSpec;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.s3.AmazonS3;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.StrictErrorHandler;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.SingleValidationMessage;
import ca.uhn.fhir.validation.ValidationResult;
import io.swagger.annotations.ApiParam;

@Path("/Patient")

@io.swagger.annotations.Api(description = "the Patient API")
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaJerseyServerCodegen", date = "2018-07-17T16:45:16.134-07:00")
public class PatientResource  {
	@Context
	SecurityContext securityContext;
	
	

	static final Logger log = LogManager.getLogger(PatientResource.class);

	private static final String PATIENT_TABLE = System.getenv("FHIR_PATIENT_TABLE");
	private static final String FHIR_META_TABLE = System.getenv("FHIR_RESOURCE_META_TABLE");
	private static final String FHIR_INSTANCE_BUCKET = System.getenv("FHIR_INSTANCE_BUCKET");
	private static final String VALIDATE_FHIR_RESOURCE = System.getenv("VALIDATE_FHIR_RESOURCE");
	//private static final String PATIENT_NAME_TABLE = System.getenv("PATIENT_NAME_TABLE");
	//private static final String PATIENT_CONTACT_INFO_TABLE = System.getenv("PATIENT_CONTACT_INFO_TABLE");
	
	private static final String COGNITO_ENABLED = System.getenv("COGNITO_ENABLED");
	
	public PatientResource() {
		//super(FhirContext.forDstu3(), SERVER_DESCRIPTION, SERVER_NAME, SERVER_VERSION);
	}
	


	@DELETE
	@Path("/{id}")

	@Produces({ "application/fhir+json", "application/xml+fhir" })
	@io.swagger.annotations.ApiOperation(value = "", notes = "Delete resource ", response = Void.class, tags = {})
	@io.swagger.annotations.ApiResponses(value = {
			@io.swagger.annotations.ApiResponse(code = 204, message = "Succesfully deleted resource ", response = Void.class),

			@io.swagger.annotations.ApiResponse(code = 404, message = "Not Found - resource was not found ", response = Void.class),

			@io.swagger.annotations.ApiResponse(code = 405, message = "Method Not allowed - delete is not allowed ", response = Void.class) })
	public Response dELETEPatientid(@ApiParam(value = "", required = true) @PathParam("id") String id,
			@Context SecurityContext securityContext) {
		return Response.status(405).entity("Method Not allowed - delete is not allowed ").build();
		
	}

	@GET
	
	@Produces({ "application/fhir+json", "application/xml+fhir" })
	@io.swagger.annotations.ApiOperation(value = "", notes = "Get Patient", response = Void.class, tags = {})
	@io.swagger.annotations.ApiResponses(value = {
			@io.swagger.annotations.ApiResponse(code = 200, message = "Status 200", response = Void.class) })
	
	public Response gETPatient(@Context SecurityContext securityContext,
			@DefaultValue("ALL") @QueryParam("gender") String gender,
			@DefaultValue("30") @QueryParam("date-range-days") String dateRangeDays)  {
		
		
		Bundle bundle = new Bundle();
		
		log.debug("Entering patient search for last "+dateRangeDays+" days ");

		bundle.setType(BundleType.SEARCHSET);
		bundle.setId(UUID.randomUUID().toString());
		
		BundleLinkComponent bunLinkComp = new BundleLinkComponent();
		bunLinkComp.setRelation("self");
		//bunLinkComp.setUrl("http://hapi.fhir.org/baseDstu3/Patient?_pretty=true&address-country=US");
		
		ArrayList<BundleLinkComponent> bunLinkList = new ArrayList<>();
		bunLinkList.add(bunLinkComp);
		
		bundle.setLink(bunLinkList);
		
		ArrayList<BundleEntryComponent> entryList = new ArrayList<>();
		
		DynamoDB dynamoDB = LambdaHandler.getDynamoDB();
		Table table = dynamoDB.getTable(PATIENT_TABLE);
		
		Index index = table.getIndex("gender-createdDate-index");
		log.debug("The key attribute value "+gender);
		int resultCount = 0;
		if(gender.equals("ALL")) {
			LocalDate now = LocalDate.now();
			LocalDate pastDate = now.minusDays( Integer.parseInt(dateRangeDays) );
			RangeKeyCondition rangeKeyCond = new RangeKeyCondition("createdDate").ge(pastDate.toString());
			//search for female patients
			KeyAttribute key = new KeyAttribute("gender", "female");
			
			log.debug("The last date : "+pastDate);
			QuerySpec spec = new QuerySpec()
				.withHashKey(key)
			    //.withKeyConditionExpression("gender = :v_gender")
			    .withRangeKeyCondition(rangeKeyCond);
			
			ItemCollection<QueryOutcome> itemsFemale = index.query(spec);
			log.debug("The items received : "+itemsFemale);
			
			
			if(itemsFemale != null) {
				Iterator<Item> iter = itemsFemale.iterator(); 
				while (iter.hasNext()) {
					BundleEntryComponent comp = new BundleEntryComponent();
					Item item = iter.next();
					String patJSON = item.toJSON();
					log.debug("The patient json :"+patJSON);
					//Patient patient = LambdaHandler.getJsonParser().parseResource(Patient.class, obsJSON);
					Patient patient = LambdaHandler.getFHIRContext().newJsonParser().parseResource(Patient.class, patJSON);
					String obsId = item.getString("id");
				    log.debug("The patient id : "+item.getString("id"));
				    
					comp.setResource(patient);
					comp.setFullUrl("http://hapi.fhir.org/baseDstu3/Patient/"+obsId);
					
					entryList.add(comp);
					resultCount++;
				}
			}
			//search for male patients
			key = new KeyAttribute("gender", "male");
			spec = new QuerySpec()
				.withHashKey(key)
			    //.withKeyConditionExpression("gender = :v_gender")
			    .withRangeKeyCondition(rangeKeyCond);
			
			ItemCollection<QueryOutcome> itemsMale = index.query(spec);
			log.debug("The items received : "+itemsMale);
			if(itemsMale != null) {
				Iterator<Item> iter = itemsMale.iterator(); 
				while (iter.hasNext()) {
					BundleEntryComponent comp = new BundleEntryComponent();
					Item item = iter.next();
					String patJSON = item.toJSON();
					log.debug("The patient json :"+patJSON);
					//Patient patient = LambdaHandler.getJsonParser().parseResource(Patient.class, obsJSON);
					Patient patient = LambdaHandler.getFHIRContext().newJsonParser().parseResource(Patient.class, patJSON);
					String obsId = item.getString("id");
				    log.debug("The patient id : "+item.getString("id"));
				    
					comp.setResource(patient);
					comp.setFullUrl("http://hapi.fhir.org/baseDstu3/Patient/"+obsId);
					
					entryList.add(comp);
					resultCount++;
				}
			}
			
		}else {
			LocalDate now = LocalDate.now();
			LocalDate pastDate = now.minusDays( Integer.parseInt(dateRangeDays) );
			RangeKeyCondition rangeKeyCond = new RangeKeyCondition("createdDate").ge(pastDate.toString());
			KeyAttribute key = new KeyAttribute("gender", gender.equals("F")?"female":"male");
			log.debug("The key attribute value "+gender);
			
			
			log.debug("The last date : "+pastDate);
			QuerySpec spec = new QuerySpec()
				.withHashKey(key)
			    //.withKeyConditionExpression("gender = :v_gender")
			    .withRangeKeyCondition(rangeKeyCond);
			
			ItemCollection<QueryOutcome> items = index.query(spec);
			log.debug("The items received : "+items);
	
			if(items != null) {
				Iterator<Item> iter = items.iterator(); 
				while (iter.hasNext()) {
					BundleEntryComponent comp = new BundleEntryComponent();
					Item item = iter.next();
					String patJSON = item.toJSON();
					log.debug("The patient json :"+patJSON);
					//Patient patient = LambdaHandler.getJsonParser().parseResource(Patient.class, obsJSON);
					Patient patient = LambdaHandler.getFHIRContext().newJsonParser().parseResource(Patient.class, patJSON);
					String obsId = item.getString("id");
				    log.debug("The patient id : "+item.getString("id"));
				    
					comp.setResource(patient);
					comp.setFullUrl("http://hapi.fhir.org/baseDstu3/Patient/"+obsId);
					
					entryList.add(comp);
					resultCount++;
				}
				
			}	
		}
		bundle.setEntry(entryList);
		bundle.setTotal(resultCount);
		return Response.status(200).entity(LambdaHandler.getFHIRContext().newJsonParser().encodeResourceToString(bundle)).build();
	}



	@GET
	@Path("/_search")

	@Produces({ "application/fhir+json", "application/xml+fhir" })
	@io.swagger.annotations.ApiOperation(value = "", notes = "", response = Void.class, tags = {})
	@io.swagger.annotations.ApiResponses(value = {
			@io.swagger.annotations.ApiResponse(code = 200, message = "Status 200", response = Void.class) })
	
	public Response gETPatientSearch(
			@DefaultValue("") @QueryParam("gender") String gender,
			@DefaultValue("") @QueryParam("birthDate") String birthDate,
			@DefaultValue("") @QueryParam("address-city") String addressCity,
			@DefaultValue("") @QueryParam("firstName") String firstName,
			@DefaultValue("") @QueryParam("lastName") String lastName,
			@DefaultValue("") @QueryParam("MRN") String mrnVal,
			@Context SecurityContext securityContext) {
		
		Bundle bundle = new Bundle();
	
		log.debug("Entering patient search with query param :"+gender + "-"+ birthDate+"-"+addressCity+"-"+firstName+"-"+lastName+"-"+mrnVal);
		Table table = LambdaHandler.getDynamoDB().getTable(PATIENT_TABLE);
	
		HashMap<String, String> nameMap = new HashMap<String,String>();
		nameMap.put("#name", "name");
		nameMap.put("#family", "family");
		nameMap.put("#type", "type");
		nameMap.put("#value", "value");
		ScanSpec spec = new ScanSpec()
				.withNameMap(nameMap)
				.withFilterExpression("gender = :v_gender and birthDate = :v_birthDate "
						+ "and address[0].city = :v_addressCity "
						+ "and #name[0].#family = :v_lastName "
						+ "and #name[0].given[0]= :v_firstName "
						+ "and identifier[1].#type.coding[0].code= :v_mrnCode "
						+ "and identifier[1].#value = :v_mrnVal")
				
				.withValueMap(new ValueMap()
						.withString(":v_gender", gender)
						.withString(":v_birthDate", birthDate)
						.withString(":v_addressCity", addressCity)
						.withString(":v_lastName", lastName)
						.withString(":v_firstName", firstName)
						.withString(":v_mrnCode", "MR")
						.withString(":v_mrnVal", mrnVal));

		
		ItemCollection<ScanOutcome> items = table.scan(spec);
		Iterator<Item> iter = items.iterator();
		
		log.debug("Iter received : "+iter.hasNext());
		
		
		bundle.setType(BundleType.SEARCHSET);
		bundle.setId(UUID.randomUUID().toString());
		
		BundleLinkComponent bunLinkComp = new BundleLinkComponent();
		bunLinkComp.setRelation("self");
		bunLinkComp.setUrl("http://hapi.fhir.org/baseDstu3/Patient?_pretty=true&address-country=US");
		
		ArrayList<BundleLinkComponent> bunLinkList = new ArrayList<>();
		bunLinkList.add(bunLinkComp);
		
		bundle.setLink(bunLinkList);
		
		ArrayList<BundleEntryComponent> entryList = new ArrayList<>();
		int resultCount = 0;
		
		while(iter.hasNext()) {
			BundleEntryComponent comp = new BundleEntryComponent();
			String patJSON = iter.next().toJSON();
			log.debug("The JSON string : "+patJSON);
			Patient patient = LambdaHandler.getFHIRContext().newJsonParser().parseResource(Patient.class,patJSON);
			comp.setResource(patient);
			//comp.setFullUrl("http://hapi.fhir.org/baseDstu3/Patient/"+patient.getId());
			log.debug("The ID element set : "+patient.getIdElement().getIdPart());
			comp.setFullUrl("urn:uuid:"+patient.getIdElement().getIdPart());
			//log.debug("The item data "+ patient.getIdElement().getId());
			entryList.add(comp);
			resultCount++;
		}
		bundle.setEntry(entryList);
		bundle.setTotal(resultCount);

		return Response.status(200).entity(LambdaHandler.getFHIRContext().newJsonParser().encodeResourceToString(bundle)).build();
	}



	@GET
	@Path("/{id}")
	@Produces({ "application/fhir+json", "application/xml+fhir" })
	@io.swagger.annotations.ApiOperation(value = "", notes = "", response = Void.class, tags = {})
	@io.swagger.annotations.ApiResponses(value = {
			@io.swagger.annotations.ApiResponse(code = 200, message = "Status 200", response = Void.class),

			@io.swagger.annotations.ApiResponse(code = 404, message = "Tried to get an unknown resource ", response = Void.class) })
	
	
	public Response gETPatientid(@Context SecurityContext securityContext,
			@ApiParam(value = "", required = true) @PathParam("id") String id, @HeaderParam("Accept") String accepted) {

		// System.out.println("Method call invoked..");
		log.debug("Method call invoked..");
		
		String respMsg = "";
		int respCode=200;
		

		log.debug("The id received from API Gateway : " + id);

		AmazonDynamoDB client = LambdaHandler.getDDBClient();
		DynamoDB db = new DynamoDB(client);
		Table table = db.getTable(PATIENT_TABLE);

		Item item = table.getItem("id", id);
		
		if(item != null) {
			item.removeAttribute("createdDate");
			respMsg = item.toJSON();
			respCode = 200;
		}else {
			respCode = 404;
			respMsg = "Tried to get an unknown resource";
		}
		
		log.debug("The json string retrieved : " + respMsg);
		if(respCode != 404) {
			Patient pat = LambdaHandler.getFHIRContext().newJsonParser().parseResource(Patient.class, respMsg);
			
			respMsg = LambdaHandler.getFHIRContext().newJsonParser().encodeResourceToString(pat);
		}
		return Response.status(respCode).entity(respMsg).build();

	}


	@GET
	@Path("/$meta")

	@io.swagger.annotations.ApiOperation(value = "", notes = "", response = Void.class, tags = {})
	@io.swagger.annotations.ApiResponses(value = {
			@io.swagger.annotations.ApiResponse(code = 200, message = "Successfully retrieved resource(s) ", response = Void.class) })
	public Response gETPatientmeta(@Context SecurityContext securityContext)  {
		return Response.status(Response.Status.NOT_IMPLEMENTED).entity("Meta not supported").build();
		
	}


	@POST

	@Consumes({ "application/fhir+json", "application/xml+fhir" })
	@Produces({ "application/fhir+json", "application/xml+fhir" })
	@io.swagger.annotations.ApiOperation(value = "", notes = "Create a new type ", response = Void.class, tags = {})
	@io.swagger.annotations.ApiResponses(value = {
			@io.swagger.annotations.ApiResponse(code = 201, message = "Succesfully created a new type ", response = Void.class),

			@io.swagger.annotations.ApiResponse(code = 400, message = "Bad Request - Resource cound not be parsed or failed basic FHIR validation rules ", response = Void.class),

			@io.swagger.annotations.ApiResponse(code = 404, message = "Not Found - resource type not support, or not a FHIR validation rules ", response = Void.class) })
	
	public Response pOSTPatient(@Context SecurityContext securityContext, String patientBlob)  {
		OperationOutcome opOutCome = null;
		
			
		log.debug("Before Validation started ..");
		//ValidationResult result = FhirContext.forDstu3().newValidator().validateWithResult(patientBlob);
		if(VALIDATE_FHIR_RESOURCE.equals("true")) {
			ValidationResult result = LambdaHandler.getFHIRValidator().validateWithResult(patientBlob);

			log.debug("After Validation  ..");
			if (result.getMessages().size() > 0) {
				log.debug("Validation failed ..");
				// The result object now contains the validation results
				for (SingleValidationMessage next : result.getMessages()) {
					log.debug("Validation message : " + next.getLocationString() + " " + next.getMessage());
				}
				return Response.status(Response.Status.BAD_REQUEST).build();
			}
		}
		Patient patient = LambdaHandler.getFHIRContext().newJsonParser().parseResource(Patient.class, patientBlob);
		String userId = null;
		if(COGNITO_ENABLED.equals("true")) {
			CognitoUserPoolPrincipal cognitoPrin = 
					securityContext.getUserPrincipal()!=null?(CognitoUserPoolPrincipal)securityContext.getUserPrincipal():null;
			userId = 
					cognitoPrin!=null?cognitoPrin.getClaims().getUsername():null;
		}
		String id = this.createPatient(patient,userId!=null?userId:"Unknown");

		//load attachment to S3
		
		AmazonS3 s3Client = LambdaHandler.getS3Client();
		String s3Key = id+"_V1";
		s3Client.putObject(FHIR_INSTANCE_BUCKET,s3Key, patientBlob);
		
		//load meta info to Dyanamo DB
		
		HashMap<String, AttributeValue> attValues = new HashMap<String,AttributeValue>();
		attValues.put("ResourceType", new AttributeValue("Patient"));
		attValues.put("id",new AttributeValue(id));
		attValues.put("BucketName",new AttributeValue(FHIR_INSTANCE_BUCKET));
		attValues.put("Key",new AttributeValue(s3Key));
		
		//metaTable.putItem(FHIR_META_TABLE,)
		AmazonDynamoDB ddbClient = LambdaHandler.getDDBClient();
		ddbClient.putItem(FHIR_META_TABLE, attValues);
		
		opOutCome = new OperationOutcome();
		opOutCome.setId(new IdType("Patient", id, "1"));
		//opOutCome.fhirType();
		Narrative narrative = new Narrative();
		narrative.setStatus(NarrativeStatus.GENERATED);
		narrative.setDivAsString("<div xmlns=\\\"http://www.w3.org/1999/xhtml\\\"><h1>Operation Outcome</h1>"
				+ "<table border=\\\"0\\\"><tr><td style=\\\"font-weight: bold;\\\">INFORMATION</td><td>[]</td>"
				+ "<td><pre>Successfully created resource \\\"Patient/"+id+"/_history/1\\\" in 36ms</pre>"
				+ "</td>\\n\\t\\t\\t\\t\\t\\n\\t\\t\\t\\t\\n\\t\\t\\t</tr>\\n\\t\\t\\t<tr>\\n\\t\\t\\t\\t<td style=\\\"font-weight: bold;\\\">INFORMATION</td>"
				+ "\\n\\t\\t\\t\\t<td>[]</td>\\n\\t\\t\\t\\t\\n\\t\\t\\t\\t\\t\\n\\t\\t\\t\\t\\t\\n\\t\\t\\t\\t\\t\\t<td>"
				+ "<pre>No issues detected during validation</pre></td>\\n\\t\\t\\t\\t\\t\\n\\t\\t\\t\\t\\n\\t\\t\\t</tr>\\n\\t\\t</table>\\n\\t</div>");
		opOutCome.setText(narrative);
		ArrayList<OperationOutcomeIssueComponent> list = new ArrayList<OperationOutcomeIssueComponent>();
		
		OperationOutcomeIssueComponent issue = new OperationOutcomeIssueComponent();
		issue.setSeverity(IssueSeverity.INFORMATION);
		issue.setCode(IssueType.INFORMATIONAL);
		issue.setDiagnostics("Successfully created resource Patient/"+id+"/_history/1");
		list.add(issue);
		
		issue = new OperationOutcomeIssueComponent();
		issue.setSeverity(IssueSeverity.INFORMATION);
		issue.setCode(IssueType.INFORMATIONAL);
		issue.setDiagnostics("No issues detected during validation");
		list.add(issue);
		opOutCome.addContained(patient);
		
		opOutCome.setIssue(list);
		// return Response.status(201).entity(newOrder).build();

		log.debug("End of function...");
		System.out.println("End of function from system out....");

		return Response.status(Response.Status.CREATED).entity(LambdaHandler
				.getFHIRContext().newJsonParser()
				.encodeResourceToString(opOutCome)).build();
	}
	
	
	/**
	 * method to create Patient record using a Patient object
	 * @param pat
	 * @return
	 */
	
	public String createPatient(Patient patient, String userId) {
		log.debug("Executing patient create.. ");
		//log.debug("The security context object.." + securityContext);

		if (patient != null) {
			log.debug("The patient object received .." + patient.getName());
		} else {
			log.debug("Patient object is null..");
		}
		String id = patient.getId();
		if(id == null) {
			id =  UUID.randomUUID().toString();
			patient.setId(id);
		}else {
			log.debug("The id is : "+id.substring(id.indexOf("urn:uuid:")+9));
			patient.setId(id.substring(id.indexOf("urn:uuid:")+9)); //extracting the guid part
		}
		// log.debug("Executing dynamo db..");
		log.debug("Execute Dynamo DB with id" +id);

		DynamoDB dynamodb = new DynamoDB(LambdaHandler.getDDBClient());
		Table myTable = dynamodb.getTable(PATIENT_TABLE);

		// Make sure your object includes the hash or hash/range key
		String myJsonString = LambdaHandler.getFHIRContext().newJsonParser().encodeResourceToString(patient);
		
		log.debug("The patient id : "+patient.getId()+ " JSON String "+myJsonString);

		// Convert the JSON into an object
		Item myItem = Item.fromJSON(myJsonString);
		
		//put a created date
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		myItem.withString("createdDate", sdf.format(new Date()));
		
		//add the user id to the record
		myItem.withString("userid", userId);

		// Insert the Object
		myTable.putItem(myItem);
		return id;
	}



	@PUT
	@Path("/{id}")
	@Consumes({ "application/fhir+json", "application/xml+fhir" })
	@Produces({ "application/fhir+json", "application/xml+fhir" })
	@io.swagger.annotations.ApiOperation(value = "", notes = "Update an existing instance ", response = Void.class, tags = {})
	@io.swagger.annotations.ApiResponses(value = {
			@io.swagger.annotations.ApiResponse(code = 200, message = "Succesfully updated the instance  ", response = Void.class),

			@io.swagger.annotations.ApiResponse(code = 201, message = "Succesfully created the instance  ", response = Void.class),

			@io.swagger.annotations.ApiResponse(code = 400, message = "Bad Request - Resource cound not be parsed or failed basic FHIR validation rules ", response = Void.class),

			@io.swagger.annotations.ApiResponse(code = 404, message = "Not Found - resource type not support, or not a FHIR validation rules ", response = Void.class),

			@io.swagger.annotations.ApiResponse(code = 405, message = "Method Not allowed - the resource did not exist prior to the update, and the server does not allow client defined ids ", response = Void.class),

			@io.swagger.annotations.ApiResponse(code = 409, message = "Version conflict management ", response = Void.class),

			@io.swagger.annotations.ApiResponse(code = 412, message = "Version conflict management ", response = Void.class),

			@io.swagger.annotations.ApiResponse(code = 422, message = "Unprocessable Entity - the proposed resource violated applicable FHIR  profiles or server business rules.  This should be accompanied by an OperationOutcome resource providing additional detail. ", response = Void.class) })
	

	public Response pUTPatientid(@ApiParam(value = "", required = true) @PathParam("id") String id,
			@Context SecurityContext securityContext,
			String patientBlob) {
		try {
			log.debug("The id received is :" + id);
			log.debug("Before Validation started ..");

			Patient patient = LambdaHandler.getFHIRContext().newJsonParser().parseResource(Patient.class,patientBlob);
			DynamoDB dynamodb = new DynamoDB(LambdaHandler.getDDBClient());
			Table myTable = dynamodb.getTable(PATIENT_TABLE);
			KeyAttribute key = new KeyAttribute("id", id);
			
			//Iterable<Map.Entry<String, Object>> iterable =Item.fromJSON(patientBlob).attributes();
			ArrayList<AttributeUpdate> attList = new ArrayList<AttributeUpdate>();
			ArrayList<AttributeValue> valList = new ArrayList<AttributeValue>();
			
			for (Map.Entry<String,Object> entry : Item.fromJSON(patientBlob).attributes()) {
				//log.debug("The key is :"+entry.getKey());
				//log.debug("The value is :"+entry.getValue());
				AttributeUpdate attUpd = new AttributeUpdate(entry.getKey());
				attList.add(attUpd);
				AttributeValue attVal = new AttributeValue(entry.getValue().toString());
				valList.add(attVal);
				
			}
			patient.setId(id);
			// Make sure your object includes the hash or hash/range key
			String myJsonString = LambdaHandler.getFHIRContext().newJsonParser().encodeResourceToString(patient);

			// Convert the JSON into an object
			Item myItem = Item.fromJSON(myJsonString);

			// Insert the Object
			myTable.putItem(myItem);
			
			log.debug("Patient table updated : ");
		}catch(Exception exp) {
			exp.printStackTrace();
			return Response.status(Response.Status.BAD_REQUEST)
					.entity("Bad Request - Resource cound not be parsed or failed basic FHIR validation rules")
					.build();
		}

		return Response.status(Response.Status.ACCEPTED).entity(patientBlob).build();
		
	}
	
	public void testBundle()throws IOException {
		
		String bundleBlob = this.getFile("Doretha289_Bayer639_4480d762-f8c4-4691-bbe9-3dabe66496eb.json");
				
		Bundle bundle = LambdaHandler.getFHIRContext().newJsonParser().parseResource(Bundle.class, bundleBlob);
		
		List<BundleEntryComponent> list = bundle.getEntry();
		
		for(BundleEntryComponent entry : list) {
			String fhirType = entry.getResource().fhirType();
			//System.out.println(entry.getResource().fhirType());
			
			if(fhirType.equals(ResourceType.PATIENT.getDisplay())) {
				Patient pat = (Patient)entry.getResource();
				log.debug("The patient name "+pat.getName());
				List<HumanName> namelist = pat.getName();
				for(HumanName name : namelist) {
					log.debug("The patient name : "+ name.getGivenAsSingleString());
				}
				
			}	
		}
		
	}
	
	private String getFile(String fileName) {

		StringBuilder result = new StringBuilder("");

		//Get file from resources folder
		ClassLoader classLoader = getClass().getClassLoader();
		File file = new File(classLoader.getResource(fileName).getFile());

		try (Scanner scanner = new Scanner(file)) {

			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				result.append(line).append("\n");
			}

			scanner.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
			
		return result.toString();

	}

  
    
	public static void main(String[] args) {
		AmazonDynamoDB client = LambdaHandler.getDDBClient();
		DynamoDB db = new DynamoDB(client);
		Table table = db.getTable("FHIRPatient");

		Item item = table.getItem("id", "123456789");

		String json = item.toJSON();
		log.debug("The json " + json);

		String patientBlob = "{\n" + "  \"resourceType\": \"Patient\",\n" + "  \"id\": \"example\",\n"
				+ "  \"text\": {\n" + "    \"status\": \"generated\",\n"
				+ "    \"div\": \"<div xmlns=\\\"http://www.w3.org/1999/xhtml\\\">\\n\\t\\t\\t<table>\\n\\t\\t\\t\\t<tbody>\\n\\t\\t\\t\\t\\t<tr>\\n\\t\\t\\t\\t\\t\\t<td>Name</td>\\n\\t\\t\\t\\t\\t\\t<td>Peter James \\n              <b>Chalmers</b> (&quot;Jim&quot;)\\n            </td>\\n\\t\\t\\t\\t\\t</tr>\\n\\t\\t\\t\\t\\t<tr>\\n\\t\\t\\t\\t\\t\\t<td>Address</td>\\n\\t\\t\\t\\t\\t\\t<td>534 Erewhon, Pleasantville, Vic, 3999</td>\\n\\t\\t\\t\\t\\t</tr>\\n\\t\\t\\t\\t\\t<tr>\\n\\t\\t\\t\\t\\t\\t<td>Contacts</td>\\n\\t\\t\\t\\t\\t\\t<td>Home: unknown. Work: (03) 5555 6473</td>\\n\\t\\t\\t\\t\\t</tr>\\n\\t\\t\\t\\t\\t<tr>\\n\\t\\t\\t\\t\\t\\t<td>Id</td>\\n\\t\\t\\t\\t\\t\\t<td>MRN: 12345 (Acme Healthcare)</td>\\n\\t\\t\\t\\t\\t</tr>\\n\\t\\t\\t\\t</tbody>\\n\\t\\t\\t</table>\\n\\t\\t</div>\"\n"
				+ "  },\n" + "  \"identifier\": [\n" + "    {\n" + "      \"use\": \"usual\",\n" + "      \"type\": {\n"
				+ "        \"coding\": [\n" + "          {\n"
				+ "            \"system\": \"http://hl7.org/fhir/v2/0203\",\n" + "            \"code\": \"MR\"\n"
				+ "          }\n" + "        ]\n" + "      },\n"
				+ "      \"system\": \"urn:oid:1.2.36.146.595.217.0.1\",\n" + "      \"value\": \"12345\",\n"
				+ "      \"period\": {\n" + "        \"start\": \"2001-05-06\"\n" + "      },\n"
				+ "      \"assigner\": {\n" + "        \"display\": \"Acme Healthcare\"\n" + "      }\n" + "    }\n"
				+ "  ],\n" + "  \"active\": true,\n" + "  \"active\": false,\n" + "  \"name\": [\n" + "    {\n"
				+ "      \"use\": \"official\",\n" + "      \"family\": \"Chalmers\",\n" + "      \"given\": [\n"
				+ "        \"Peter\",\n" + "        \"James\"\n" + "      ]\n" + "    },\n" + "    {\n"
				+ "      \"use\": \"usual\",\n" + "      \"given\": [\n" + "        \"Jim\"\n" + "      ]\n"
				+ "    },\n" + "    {\n" + "      \"use\": \"maiden\",\n" + "      \"family\": \"Windsor\",\n"
				+ "      \"given\": [\n" + "        \"Peter\",\n" + "        \"James\"\n" + "      ],\n"
				+ "      \"period\": {\n" + "        \"end\": \"2002\"\n" + "      }\n" + "    }\n" + "  ],\n"
				+ "  \"telecom\": [\n" + "    {\n" + "      \"use\": \"home\"\n" + "    },\n" + "    {\n"
				+ "      \"system\": \"phone\",\n" + "      \"value\": \"(03) 5555 6473\",\n"
				+ "      \"use\": \"work\",\n" + "      \"rank\": 1\n" + "    },\n" + "    {\n"
				+ "      \"system\": \"phone\",\n" + "      \"value\": \"(03) 3410 5613\",\n"
				+ "      \"use\": \"mobile\",\n" + "      \"rank\": 2\n" + "    },\n" + "    {\n"
				+ "      \"system\": \"phone\",\n" + "      \"value\": \"(03) 5555 8834\",\n"
				+ "      \"use\": \"old\",\n" + "      \"period\": {\n" + "        \"end\": \"2014\"\n" + "      }\n"
				+ "    }\n" + "  ],\n" + "  \"gender\": \"male\",\n" + "  \"birthDate\": \"1974-12-25\",\n"
				+ "  \"_birthDate\": {\n" + "    \"extension\": [\n" + "      {\n"
				+ "        \"url\": \"http://hl7.org/fhir/StructureDefinition/patient-birthTime\",\n"
				+ "        \"valueDateTime\": \"1974-12-25T14:35:45-05:00\"\n" + "      }\n" + "    ]\n" + "  },\n"
				+ "  \"deceasedBoolean\": false,\n" + "  \"address\": [\n" + "    {\n" + "      \"use\": \"home\",\n"
				+ "      \"type\": \"both\",\n"
				+ "      \"text\": \"534 Erewhon St PeasantVille, Rainbow, Vic  3999\",\n" + "      \"line\": [\n"
				+ "        \"534 Erewhon St\"\n" + "      ],\n" + "      \"city\": \"PleasantVille\",\n"
				+ "      \"district\": \"Rainbow\",\n" + "      \"state\": \"Vic\",\n"
				+ "      \"postalCode\": \"3999\",\n" + "      \"period\": {\n" + "        \"start\": \"1974-12-25\"\n"
				+ "      }\n" + "    }\n" + "  ],\n" + "  \"contact\": [\n" + "    {\n" + "      \"relationship\": [\n"
				+ "        {\n" + "          \"coding\": [\n" + "            {\n"
				+ "              \"system\": \"http://hl7.org/fhir/v2/0131\",\n" + "              \"code\": \"N\"\n"
				+ "            }\n" + "          ]\n" + "        }\n" + "      ],\n" + "      \"name\": {\n"
				+ "        \"family\": \"du Marché\",\n" + "        \"_family\": {\n" + "          \"extension\": [\n"
				+ "            {\n"
				+ "              \"url\": \"http://hl7.org/fhir/StructureDefinition/humanname-own-prefix\",\n"
				+ "              \"valueString\": \"VV\"\n" + "            }\n" + "          ]\n" + "        },\n"
				+ "        \"given\": [\n" + "          \"Bénédicte\"\n" + "        ]\n" + "      },\n"
				+ "      \"telecom\": [\n" + "        {\n" + "          \"system\": \"phone\",\n"
				+ "          \"value\": \"+33 (237) 998327\"\n" + "        }\n" + "      ],\n"
				+ "      \"address\": {\n" + "        \"use\": \"home\",\n" + "        \"type\": \"both\",\n"
				+ "        \"line\": [\n" + "          \"534 Erewhon St\"\n" + "        ],\n"
				+ "        \"city\": \"PleasantVille\",\n" + "        \"district\": \"Rainbow\",\n"
				+ "        \"state\": \"Vic\",\n" + "        \"postalCode\": \"3999\",\n" + "        \"period\": {\n"
				+ "          \"start\": \"1974-12-25\"\n" + "        }\n" + "      },\n"
				+ "      \"gender\": \"female\",\n" + "      \"period\": {\n" + "        \"start\": \"2012\"\n"
				+ "      }\n" + "    }\n" + "  ],\n" + "  \"managingOrganization\": {\n"
				+ "    \"reference\": \"Organization/1\"\n" + "  }\n" + "}";
		Patient patient = FhirContext.forDstu3().newJsonParser().parseResource(Patient.class, patientBlob);
		// Patient patient = new Patient();
		System.out.println("Executing patient create.. ");
		// System.out.println("The security context object.."+securityContext);

		if (patient != null) {
			System.out.println("The patient object received .." + patient.getName());
		} else {
			System.out.println("Patient object is null..");
		}
		System.out.println("Before Validation started ..");
		// Ask the context for a validator

		// Ask the context for a validator
		FhirContext ctx = FhirContext.forDstu3();
		FhirValidator validator = ctx.newValidator();
		ctx.setParserErrorHandler(new StrictErrorHandler());
		// validator.validateWithResult(arg0)(patient);

		ValidationResult result = validator.validateWithResult(patient);
		if (result.getMessages().size() > 0) {
			System.out.println("Validation failed ..");
			// The result object now contains the validation results
			for (SingleValidationMessage next : result.getMessages()) {
				System.out.println("Validation message : " + next.getLocationString() + " " + next.getMessage());
			}
			// return Response.status(Response.Status.BAD_REQUEST).build();
		}

	}
}
