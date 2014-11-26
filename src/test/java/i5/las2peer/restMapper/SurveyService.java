/*
Copyright (c) 2014 Dominik Renzel, Advanced Community Information Systems (ACIS) Group, 
Chair of Computer Science 5 (Databases & Information Systems), RWTH Aachen University, Germany
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

 * Redistributions of source code must retain the above copyright notice, this
  list of conditions and the following disclaimer.

 * Redistributions in binary form must reproduce the above copyright notice,
  this list of conditions and the following disclaimer in the documentation
  and/or other materials provided with the distribution.

 * Neither the name of the {organization} nor the names of its
  contributors may be used to endorse or promote products derived from
  this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package i5.las2peer.restMapper;

import i5.las2peer.restMapper.annotations.Consumes;
import i5.las2peer.restMapper.annotations.ContentParam;
import i5.las2peer.restMapper.annotations.DELETE;
import i5.las2peer.restMapper.annotations.GET;
import i5.las2peer.restMapper.annotations.HeaderParam;
import i5.las2peer.restMapper.annotations.POST;
import i5.las2peer.restMapper.annotations.PUT;
import i5.las2peer.restMapper.annotations.Path;
import i5.las2peer.restMapper.annotations.PathParam;
import i5.las2peer.restMapper.annotations.Produces;
import i5.las2peer.restMapper.annotations.QueryParam;
import i5.las2peer.restMapper.annotations.Version;
import i5.las2peer.restMapper.annotations.swagger.ApiInfo;
import i5.las2peer.restMapper.annotations.swagger.ApiResponse;
import i5.las2peer.restMapper.annotations.swagger.ApiResponses;
import i5.las2peer.restMapper.annotations.swagger.Notes;
import i5.las2peer.restMapper.annotations.swagger.ResourceListApi;
import i5.las2peer.restMapper.annotations.swagger.Summary;

/**
 * Example Swagger-annotated las2peer service class.
 * 
 * @author Dominik Renzel
 *
 */
@Path("mobsos-surveys")
@Version("0.1")
@ApiInfo(
		title="MobSOS Surveys",
		description="<p>A simple RESTful service for online survey management.</p><p>MobSOS Surveys is part of the MobSOS "
				+ "Tool Set dedicated to exploring, modeling, and measuring Community Information System (CIS) "
				+ "Success as a complex construct established by multiple dimensions and factors. As part of "
				+ "MobSOS, this service enables to collect subjective data enabling qualitative and quantitative "
				+ "measurements of CIS Success.</p>", 
		termsOfServiceUrl="",
		contact="renzel@dbis.rwth-aachen.de",
		license="MIT",
		licenseUrl="https://github.com/rwth-acis/mobsos-survey/blob/master/LICENSE"
		)
public class SurveyService {

	public SurveyService(){

	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("surveys")
	@ResourceListApi(description = "Manage surveys")
	@Summary("search or list questionnaires.")
	@Notes("query parameter matches questionnaire name, description.")
	@ApiResponses(value={
			@ApiResponse(code = 200, message = "Questionnaires data (TODO: introduce Swagger models)"),
	})
	public HttpResponse getSurveys(@QueryParam(defaultValue = "1", name = "full") int full, @QueryParam(defaultValue = "", name="q") String query)
	{
		return new HttpResponse("");
	}

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Path("surveys")
	@Summary("create new survey.")
	@Notes("Requires authentication.")
	@ApiResponses(value={
			@ApiResponse(code = 201, message = "Survey URL & ID (TODO: introduce Swagger models)"),
			@ApiResponse(code = 400, message = "Survey data invalid."),
			@ApiResponse(code = 401, message = "Survey creation requires authentication."),
			@ApiResponse(code = 409, message = "Survey already exists.")	
	})
	public HttpResponse createSurvey(@ContentParam String data)
	{
		return new HttpResponse("");
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("surveys/{id}")
	@Summary("retrieve given survey.")
	@Notes("Use <b>/surveys</b> to retrieve list of existing surveys. ")
	@ApiResponses(value={
			@ApiResponse(code = 200, message = "Survey data (TODO: introduce Swagger models)"),
			@ApiResponse(code = 404, message = "Survey does not exist.")	
	})
	public HttpResponse getSurvey(@PathParam("id") int id){
		return new HttpResponse("");
	}

	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("surveys/{id}")
	@Summary("update given survey.")
	@Notes("Requires authentication. Use parent resource to retrieve list of existing surveys.")
	@ApiResponses(value={
			@ApiResponse(code = 200, message = "Survey updated successfully."),
			@ApiResponse(code = 400, message = "Survey data invalid."),
			@ApiResponse(code = 401, message = "Survey may only be updated by its owner."),
			@ApiResponse(code = 404, message = "Survey does not exist.")	
	})
	public HttpResponse updateSurvey(@PathParam("id") int id, @ContentParam String content){
		return new HttpResponse("");
	}

	@DELETE
	@Path("surveys/{id}")
	@Summary("delete given survey.")
	@Notes("Requires authentication. Use parent resource to retrieve list of existing surveys.")
	@ApiResponses(value={
			@ApiResponse(code = 200, message = "Survey deleted successfully."),
			@ApiResponse(code = 401, message = "Survey may only be deleted by its owner."),
			@ApiResponse(code = 404, message = "Survey does not exist.")	
	})
	public HttpResponse deleteSurvey(@PathParam("id") int id){
		return new HttpResponse("");
	}

	@GET
	@Produces(MediaType.TEXT_HTML)
	@Path("surveys/{id}/questionnaire")
	@Summary("Download questionnaire form for given survey. Enables response submission.")
	@Notes("Can be used with or without authentication, including response submission.")
	@ApiResponses(value={
			@ApiResponse(code = 200, message = "Survey questionnaire HTML representation."),
			@ApiResponse(code = 400, message = "Survey questionnaire form invalid. Cause: ..."),
			@ApiResponse(code = 404, message = "Questionnaire does not exist. <b>-or-</b> Survey questionnaire not set. <b>-or-</b> Survey questionnaire does not define form.")
			})
	public HttpResponse getSurveyQuestionnaireFormHTML(@HeaderParam(name="accept-language", defaultValue="") String lang, @PathParam("id") int id){
		return new HttpResponse("");
	}

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("surveys/{id}/questionnaire")
	@Summary("Download questionnaire form for given survey. Enables response submission.")
	@Notes("HT be used with or without authentication, including response submission.")
	@ApiResponses(value={
			@ApiResponse(code = 200, message = "Survey questionnaire HTML representation."),
			@ApiResponse(code = 400, message = "Survey questionnaire form invalid. Cause: ..."),
			@ApiResponse(code = 404, message = "Questionnaire does not exist. <b>-or-</b> Survey questionnaire not set. <b>-or-</b> Survey questionnaire does not define form.")
			})
	public HttpResponse setSurveyQuestionnaire(@PathParam("id") int id, @ContentParam String content){
		return new HttpResponse("");
	}

	@GET
	@Produces(MediaType.TEXT_CSV)
	@Path("surveys/{id}/responses")
	@Summary("retrieve response data for given survey.")
	@Notes("Use resource <i>/surveys</i> to retrieve list of existing surveys.")
	@ApiResponses(value={
			@ApiResponse(code = 200, message = "Survey response data in CSV format."),
			@ApiResponse(code = 404, message = "Survey does not exist -or- No questionnaire defined for survey.")	
	})
	public HttpResponse getSurveyResponses(@PathParam("id") int id){
		return new HttpResponse("");
	}

}
