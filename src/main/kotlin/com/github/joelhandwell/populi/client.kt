package com.github.joelhandwell.populi

import org.slf4j.Logger
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.jaxb.JaxbConverterFactory
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import org.slf4j.LoggerFactory
import retrofit2.Response
import retrofit2.converter.scalars.ScalarsConverterFactory

inline fun <reified R : Any> R.logger(): Logger =
    LoggerFactory.getLogger(this::class.java.name.substringBefore("\$Companion"))

class Populi(
    private val accessKey: String, private val api: PopuliApi
) {
    companion object {
        private val log = logger()
    }

    class Builder {
        private var username: String? = null
        private var password: String? = null
        private var accessKey: String? = null
        private var baseUrl: String? = null
        private var debugFlag: Boolean = false

        fun withUsername(username: String) = apply { this.username = username }
        fun withPassword(password: String) = apply { this.password = password }
        fun withAccessKey(accessKey: String) = apply { this.accessKey = accessKey }
        fun withBaseUrl(baseUrl: String) = apply { this.baseUrl = baseUrl }
        fun withDebugFlag(debugFlag: Boolean) = apply { this.debugFlag = debugFlag }

        fun build(): Populi {

            val builder = Retrofit.Builder().baseUrl(baseUrl ?: throw RuntimeException("baseUrl is null"))
                .addConverterFactory(JaxbConverterFactory.create())

            val api = if (debugFlag) {
                builder.addConverterFactory(ScalarsConverterFactory.create())
            } else {
                builder
            }.build().create(PopuliApi::class.java)

            if (accessKey == null) {
                log.info("fetching accessKey with username and password")
                val response = api.requestAccessKey(
                    username ?: throw RuntimeException("username null"), password
                        ?: throw RuntimeException("password null")
                )

                val body = sendRequest(response)

                if (body.access_key.isBlank()) {
                    throw RuntimeException("accessKey was blank")
                } else {
                    Populi.log.info("We got accessKey!")
                }

                accessKey = body.access_key.trim()
            } else {
                log.info("using existing accessKey")
            }

            return Populi(accessKey!!, api)
        }
    }

    /**
     * Method to confirm xml from real populi server
     */
    fun getRaw(task: String): String = this.api.getRaw(accessKey, task).execute().body().toString()

    /**
     * Returns information about each degree configured at the school. [ref](https://support.populiweb.com/hc/en-us/articles/223798747-API-Reference#getCountries)
     */
    fun getDegrees(): MutableList<Degree>? = sendRequest(this.api.getDegrees(accessKey)).degree

    /**
     * Returns all users. [ref](https://support.populiweb.com/hc/en-us/articles/223798747-API-Reference#getUsers)
     */
    fun getUsers(): MutableList<User> = sendRequest(this.api.getUsers(accessKey)).person

    /**
     * Returns all campuses. [ref](https://support.populiweb.com/hc/en-us/articles/223798747-API-Reference#getCampuses)
     */
    fun getCampuses(): MutableList<Campus> = sendRequest(this.api.getCampuses(accessKey)).campus

    /**
     * Returns information about each program configured at the school. [ref](https://support.populiweb.com/hc/en-us/articles/223798747-API-Reference#getPrograms)
     */
    fun getPrograms(): MutableList<Program> = sendRequest(this.api.getPrograms(accessKey)).program

    /**
     * Returns all academic year IDs, as well as calendar years associated with each. [ref](https://support.populiweb.com/hc/en-us/articles/223798747-API-Reference#getAcademicYears)
     */
    fun getAcademicYears(): MutableList<AcademicYear> = sendRequest(this.api.getAcademicYears(accessKey)).academic_year

    /**
     * Returns all academic terms. [ref](https://support.populiweb.com/hc/en-us/articles/223798747-API-Reference#getAcademicTerms)
     */
    fun getAcademicTerms(): MutableList<AcademicTerm> = sendRequest(this.api.getAcademicTerms(accessKey)).academic_term

    /**
     * Returns courses from your catalog (only active courses are returned by default). [ref](https://support.populiweb.com/hc/en-us/articles/223798747-API-Reference#getCourseCatalog)
     * @param include_retired If set to true, retired courses will be returned as well. Not required.
     */
    fun getCourseCatalog(include_retired: Boolean = false): MutableList<Course> =
        sendRequest(this.api.getCourseCatalog(accessKey, include_retired = if (include_retired) 1 else null)).course

    /**
     * Returns a list of course groups. [ref](https://support.populiweb.com/hc/en-us/articles/223798747-API-Reference#getCourseGroups)
     */
    fun getCourseGroups(): MutableList<CourseGroup> = sendRequest(this.api.getCourseGroups(accessKey)).course_group

    /**
     * Returns information about a course group.[ref](https://support.populiweb.com/hc/en-us/articles/223798747-API-Reference#getCourseGroupInfo)
     * @param course_group_id The numeric ID of the course group you're interested in. Required.
     * @param academic_year_id The numeric ID of the academic year you're interested in. Defaults to the current academic year ID. Not required.
     */
    fun getCourseGroupInfo(course_group_id: Int, academic_year_id: Int? = null) = sendRequest(this.api.getCourseGroupInfo(accessKey, course_group_id = course_group_id, academic_year_id = academic_year_id))

    /**
     * Returns course instances for a given term (only active course instances are returned by default). [ref](https://support.populiweb.com/hc/en-us/articles/223798747-API-Reference#getCourseInstance)
     * @param term_id The numeric ID of the term you're interested in. Required.
     */
    fun getTermCourseInstances(term_id: Int) = sendRequest(this.api.getTermCourseInstances(accessKey, term_id = term_id)).course_instance

    /**
     * Returns term students. [ref](https://support.populiweb.com/hc/en-us/articles/223798747-API-Reference#getTermStudents)
     * You must have the Registrar or Academic Admin role to call this task.
     * If the person has a profile picture, the <image> element will contain base64 encoded binary data. The <image> element won't be returned unless the person has a profile picture set.
     * There is a limit of 1000 results in the response.
     * The "num_results" attribute (in the <response> element) indicates the total number of possible results (regardless of the limit or the page parameter).
     * @param term_id Numeric ID of the term you're interested in. Defaults to the current academic term_id. Not Required.
     * @param program_id Possible values: ALL (default), NONE, or any numeric program_id. Not Required.
     * @param campus_id Possible values: ALL (default), 0 (None), or any numeric campus_id. Not Required.
     * @param return_image_data Boolean (e.g. 1 or 0). Returning binary image data will result in slower response times. Defaults to 0. Not Required.
     * @param page We limit the number of results returned (see comments), so which "page" would you like (e.g. page=1, page=2, page=3). Not Required.
     */
    fun getTermStudents(term_id: Int? = null, program_id: Int? = null, campus_id: Int? = null, return_image_data: Boolean = false, page: Int? = null) = sendRequest(this.api.getTermStudents(accessKey, term_id = term_id, program_id = program_id, campus_id = campus_id, return_image_data = if(return_image_data) 1 else 0, page = page))

    /**
     * Returns term enrollment for a particular academic term.
     * @param term_id Numeric ID of the academic term. Required.
     */
    fun getTermEnrollment(term_id: Int) = sendRequest(this.api.getTermEnrollment(accessKey, term_id = term_id)).enrollment
}

interface PopuliApi {
    @FormUrlEncoded @POST(API_URI) fun requestAccessKey(@Field("username") username: String, @Field("password") password: String): Call<AccessKeyResponse>
    @FormUrlEncoded @POST(API_URI) fun getDegrees(@Field(FIELD_ACCESS_KEY) accessKey: String, @Field(FIELD_TASK) task: String = "getDegrees"): Call<DegreeResponse>
    @FormUrlEncoded @POST(API_URI) fun getUsers(@Field(FIELD_ACCESS_KEY) accessKey: String, @Field(FIELD_TASK) task: String = "getUsers"): Call<UserResponse>
    @FormUrlEncoded @POST(API_URI) fun getCampuses(@Field(FIELD_ACCESS_KEY) accessKey: String, @Field(FIELD_TASK) task: String = "getCampuses"): Call<CampusResponse>
    @FormUrlEncoded @POST(API_URI) fun getPrograms(@Field(FIELD_ACCESS_KEY) accessKey: String, @Field(FIELD_TASK) task: String = "getPrograms"): Call<ProgramResponse>
    @FormUrlEncoded @POST(API_URI) fun getAcademicYears(@Field(FIELD_ACCESS_KEY) accessKey: String, @Field(FIELD_TASK) task: String = "getAcademicYears"): Call<AcademicYearResponse>
    @FormUrlEncoded @POST(API_URI) fun getAcademicTerms(@Field(FIELD_ACCESS_KEY) accessKey: String, @Field(FIELD_TASK) task: String = "getAcademicTerms"): Call<AcademicTermResponse>
    @FormUrlEncoded @POST(API_URI) fun getCourseCatalog(@Field(FIELD_ACCESS_KEY) accessKey: String, @Field(FIELD_TASK) task: String = "getCourseCatalog", @Field("include_retired") include_retired: Int? = null): Call<CourseResponse>
    @FormUrlEncoded @POST(API_URI) fun getCourseGroups(@Field(FIELD_ACCESS_KEY) accessKey: String, @Field(FIELD_TASK) task: String = "getCourseGroups"): Call<CourseGroupResponse>
    @FormUrlEncoded @POST(API_URI) fun getCourseGroupInfo(@Field(FIELD_ACCESS_KEY) accessKey: String, @Field(FIELD_TASK) task: String = "getCourseGroupInfo", @Field("course_group_id") course_group_id: Int, @Field("academic_year_id") academic_year_id: Int? = null): Call<CourseGroupInfoResponse>
    @FormUrlEncoded @POST(API_URI) fun getTermCourseInstances(@Field(FIELD_ACCESS_KEY) accessKey: String, @Field(FIELD_TASK) task: String = "getTermCourseInstances", @Field("term_id") term_id: Int): Call<TermCourseInstanceResponse>
    @FormUrlEncoded @POST(API_URI) fun getTermStudents(@Field(FIELD_ACCESS_KEY) accessKey: String, @Field(FIELD_TASK) task: String = "getTermStudents", @Field("term_id") term_id: Int? = null, @Field("program_id") program_id: Int? = null, @Field("campus_id") campus_id: Int? = null, @Field("return_image_data") return_image_data: Int? = null, @Field("page") page: Int? = null): Call<TermStudentResponse>
    @FormUrlEncoded @POST(API_URI) fun getTermEnrollment(@Field(FIELD_ACCESS_KEY) accessKey: String, @Field(FIELD_TASK) task: String = "getTermEnrollment", @Field("term_id") term_id: Int): Call<TermEnrollmentResponse>

    //for debug
    @FormUrlEncoded @POST(API_URI) fun getRaw(@Field(FIELD_ACCESS_KEY) accessKey: String, @Field(FIELD_TASK) task: String): Call<String>
}

private const val API_URI = "api/"
private const val FIELD_ACCESS_KEY = "access_key"
private const val FIELD_TASK = "task"

private fun <T> sendRequest(call: Call<T>): T {
    val response: Response<T> = call.execute()
    if (!response.isSuccessful) {
        throw RuntimeException("request not success, error body: ${response.errorBody()}")
    }
    return response.body() ?: throw RuntimeException("accessKey response body was null")
}
