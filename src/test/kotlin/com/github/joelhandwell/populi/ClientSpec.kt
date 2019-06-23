package com.github.joelhandwell.populi

import ch.qos.logback.classic.Level
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import org.slf4j.LoggerFactory
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.nio.file.Paths
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

object ClientSpec : Spek({

    describe("Client") {
        (LoggerFactory.getLogger("org.eclipse.jetty") as ch.qos.logback.classic.Logger).level = Level.INFO
        val wireMockServer = WireMockServer()
        val populi = Populi.Builder()
            .withBaseUrl("http://localhost:$WIREMOCK_PORT/")
            .withAccessKey(TEST_API_ACCESS_KEY)
            .build()
        beforeGroup { wireMockServer.start() }

        it("creates api client with username and password") {
            WireMock.stubFor(
                WireMock.post("/api/").withRequestBody(WireMock.containing("username=john&password=pass")).willReturn(
                    WireMock.aResponse().withBodyFile("accessKeyRequest.xml")
                )
            )

            val populiWithUsernamePassword = Populi.Builder()
                .withBaseUrl("http://localhost:$WIREMOCK_PORT/")
                .withUsername("john")
                .withPassword("pass")
                .build()

            stubForPopuli("getDegrees", getDegreesXml)

            val degrees = populiWithUsernamePassword.getDegrees()
            assertNotNull(degrees)
            assertDegrees(degrees)
        }

        it("send request, receive response and parse it into Degrees") {
            stubForPopuli("getDegrees", getDegreesXml)
            val degrees = populi.getDegrees()
            assertNotNull(degrees)
            assertDegrees(degrees)
        }

        it("send request, receive response and parse it into Users") {
            stubForPopuli("getUsers", getUsersXml)
            assertUsers(populi.getUsers())
        }

        it("send request, receive response and parse it into Campuses") {
            stubForPopuli("getCampuses", getCampusesXml)
            assertCampuses(populi.getCampuses())
        }

        it("send request, receive response and parse it into Programs") {
            stubForPopuli("getPrograms", getProgramsXml)
            assertPrograms(populi.getPrograms())
        }

        it("send request, receive response and parse it into AcademicYear") {
            stubForPopuli("getAcademicYears", getAcademicYearsXml)
            assertAcademicYears(populi.getAcademicYears())
        }

        it("send request, receive response and parse it into AcademicTerm") {
            stubForPopuli("getAcademicTerms", getAcademicTermsXml)
            assertAcademicTerms(populi.getAcademicTerms())
        }

        it("send request, receive response and parse it into current AcademicTerm") {
            stubForPopuli("getCurrentAcademicTerm", getCurrentAcademicTermXml)
            assertAcademicTerm(populi.getCurrentAcademicTerm())
        }

        it("send request, receive response and parse it into Course") {
            stubForPopuli("getCourseCatalog", getCourseCatalogXml)
            assertCourses(populi.getCourseCatalog())
            assertCourses(populi.getCourseCatalog(true)) //better to test with retired courses
        }

        it("send request, receive response and parse it into CourseGroup") {
            stubForPopuli("getCourseGroups", getCourseGroupsXml)
            assertCourseGroups(populi.getCourseGroups())
        }

        it("send request, receive response and parse it into CourseGroupInfo") {
            stubForPopuli("getCourseGroupInfo", getCourseGroupInfoXml)
            assertCourseGroupInfo(populi.getCourseGroupInfo(1111))
            assertCourseGroupInfo(populi.getCourseGroupInfo(1111, 2222))
        }

        it("send request, receive response and parse it into CourseInstance") {
            stubForPopuli("getTermCourseInstances", getTermCourseInstancesXml)
            assertTermCourseInstanceResponses(populi.getTermCourseInstances(1111))
        }

        it("send request, receive response and parse it into Student") {
            stubForPopuli("getTermStudents", getTermStudentsXml)
            assertTermStudentResponse(populi.getTermStudents())
        }

        it("send request, receive response and parse it into Enrollment") {
            stubForPopuli("getTermEnrollment", getTermEnrollmentXml)
            assertTermEnrollments(populi.getTermEnrollment(1111))
        }

        it("send request, receive response and parse it into TuitionSchedule") {
            stubForPopuli("getTuitionSchedules", getTuitionSchedulesXml)
            assertTuitionSchedules(populi.getTuitionSchedules())
        }

        it("send request, receive response and parse it into StudentTermTuitionSchedule") {
            stubForPopuli("getStudentTermTuitionSchedules", getStudentTermTuitionSchedulesXml)
            assertStudentTermTuitionSchedules(populi.getStudentTermTuitionSchedules(1111, 2222))
        }

        it("send request, receive response and parse it into CourseInstance") {
            stubForPopuli("getCourseInstance", getCourseInstanceXml)
            assertCourseInstanceResponse(populi.getCourseInstance(1111))
        }

        it("send request, receive response and parse it into AssignmentGroups for a CourseInstance") {
            stubForPopuli("getCourseInstanceAssignmentGroups", getCourseInstanceAssignmentGroupsXml)
            assertCourseInstanceAssignmentGroups(populi.getCourseInstanceAssignmentGroups(1111))
        }

        it("send request, receive response and parse it into Assignments for a CourseInstance") {
            stubForPopuli("getCourseInstanceAssignments", getCourseInstanceAssignmentsXml)
            assertAssignments(populi.getCourseInstanceAssignments(1111))
        }

        it("send request, receive response and parse it into Submissions of Assignments for a Student") {
            stubForPopuli("getStudentAssignmentSubmissions", getStudentAssignmentSubmissionsXml)
            assertStudentAssignmentSubmissions(populi.getStudentAssignmentSubmissions(1111, 2222))
        }

        it("send request, receive response and parse it into Files for a CourseInstance") {
            stubForPopuli("getCourseInstanceFiles", getCourseInstanceFilesXml)
            assertCourseInstanceFiles(populi.getCourseInstanceFiles(1111))
        }

        it("send request, receive response and parse it into Links for a CourseOffering") {
            stubForPopuli("getCourseOfferingLinks", getCourseOfferingLinksXml)
            assertCourseOfferingLinks(populi.getCourseOfferingLinks(1111))
        }

        it("send request, receive response and parse it into Lessons for a CourseInstance") {
            stubForPopuli("getCourseInstanceLessons", getCourseInstanceLessonsXml)
            assertCourseInstanceLessons(populi.getCourseInstanceLessons(1111))
        }

        it("send request, receive response and parse it into LessonContent") {
            val html = "<html><body>Lesson Content</body></html>"
            stubForPopuli("getLessonContent", html)
            assertEquals(html, populi.getLessonContent(1111, 2222))
        }

        it("send request, receive response and parse it into Meetings for a CourseInstance") {
            stubForPopuli("getCourseInstanceMeetings", getCourseInstanceMeetingsXml)
            assertCourseInstanceMeetings(populi.getCourseInstanceMeetings(1111))
        }

        it("send request, receive response and parse it into MeetingAttendances for a CourseInstance") {
            stubForPopuli("getCourseInstanceMeetingAttendance", getCourseInstanceMeetingAttendanceXml)
            assertCourseInstanceMeetingAttendances(populi.getCourseInstanceMeetingAttendance(1111, 2222))
        }

        it("send request, receive response and parse it into Students for a CourseInstance") {
            stubForPopuli("getCourseInstanceStudents", getCourseInstanceStudentsXml)
            assertCourseInstanceStudents(populi.getCourseInstanceStudents(1111))
        }

        it("send request, receive response and parse it into Students for a CourseInstance") {
            stubForPopuli("getCourseInstanceStudent", courseInstanceStudentXml)
            assertCourseInstanceStudent(populi.getCourseInstanceStudent(1111, 2222))
        }

        it("send request, receive response and parse it into a Student's Attendances for a CourseInstance for a Term") {
            stubForPopuli("getCourseInstanceStudentAttendance", getCourseInstanceStudentAttendanceXml)
            assertCourseInstanceStudentAttendances(populi.getCourseInstanceStudentAttendance(1111, 2222))
        }

        it("send request, receive response and parse it into a Student's Degree Audit") {
            stubForPopuli("getDegreeAudit", getDegreeAuditXml)
            assertDegreeAudit(populi.getDegreeAudit(1111, 2222, 3333, 4444))
        }

        it("send request, receive response and parse it into a StudentDiscipline") {
            stubForPopuli("getStudentDiscipline", getStudentDisciplineXml)
            assertStudentDisciplines(populi.getStudentDiscipline(1111))
        }

        it("send request, receive response and parse it into Programs of a Student") {
            stubForPopuli("getStudentPrograms", getStudentProgramsXml)
            assertStudentPrograms(populi.getStudentPrograms(1111))
        }

        it("send request, receive response and parse it into StudentInfo") {
            stubForPopuli("getStudentInfo", getStudentInfoXml)
            assertStudentInfo(populi.getStudentInfo(1111))
        }

        xit("real") {
            val input = Paths.get("${System.getProperty("user.dir")}\\local.properties")
                .toFile()
                .inputStream()
            val p = Properties()
            p.load(input)

            val real = Populi.Builder()
                .withBaseUrl(p.getProperty("real.baseurl"))
                .withUsername(p.getProperty("real.username"))
                .withPassword(p.getProperty("real.password"))
                .build()

            //val courseInstanceId = p.getProperty("real.course_instance_id").toInt()
            //val courseInstanceAssignmentId = p.getProperty("real.course_instance_assignment_id").toInt()
            //val yearId = p.getProperty("real.year_id").toInt()
            //val termId = p.getProperty("real.term_id").toInt()
            val personId = p.getProperty("real.person_id").toInt()
            //val degreeId = p.getProperty("real.degree_id").toInt()

            // test with your real populi account info
            //println(real.getDegrees())
            //println(real.getPrograms())
            //println(real.getAcademicYears())
            //println(real.getAcademicTerms())
            //println(real.getTermCourseInstances(termId))
            //println(real.getTermStudents(term_id = termId))
            //println(real.getTermEnrollment(termId))
            //println(real.getTuitionSchedules())
            //println(real.getCourseGroupInfo(course_group_id = p.getProperty("real.course_group_id").toInt()))
            //println(real.getTermCourseInstances(termId))
            //println(real.getCourseInstance(courseInstanceId))
            //println(real.getCourseInstanceAssignmentGroups(courseInstanceId))
            //println(real.getCourseInstanceAssignments(courseInstanceId))
            //println(real.getStudentAssignmentSubmissions(courseInstanceAssignmentId, personId))
            //println(real.getCourseInstanceFiles(courseInstanceId))
            //println(real.getRaw("getCourseInstanceLessons", courseInstanceId))
            //val lessonId = p.getProperty("real.lesson_id").toInt()
            //println(real.getLessonContent(courseInstanceId, lessonId)) // got: <?xml version="1.0" encoding="UTF-8"?><error><code>OTHER_ERROR</code><message>This task is no longer supported</message></error>
            //println(real.getCourseInstanceStudents(courseInstanceId))
            //println(real.getRaw("getCourseInstanceStudent", courseInstanceId, personId))
            //println(real.getRaw("getCourseInstanceMeetings", courseInstanceId))
            //println(real.getCourseInstanceMeetings(courseInstanceId))
            //println(real.getRaw("getCourseInstanceStudentAttendance", courseInstanceId, personId))
            //println(real.getRaw("getCurrentAcademicTerm"))
            //println(real.getCurrentAcademicTerm())
            //println(real.getCourseOfferingLinks(courseInstanceId))
            //println(real.getDegreeAudit(personId, degreeId, yearId))
            //println(real.getStudentDiscipline(personId))
            //println(real.getStudentPrograms(personId))
            println(real.getStudentInfo(personId))
        }

        afterGroup { wireMockServer.stop() }
    }
})

fun stubForPopuli(task: String, xml: String) {
    WireMock.stubFor(
        WireMock.post("/api/").withRequestBody(WireMock.containing("access_key=$TEST_API_ACCESS_KEY&task=$task")).willReturn(
            WireMock.aResponse().withBody(xml)
        )
    )
}
