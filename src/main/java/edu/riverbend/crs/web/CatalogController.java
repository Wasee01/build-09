package edu.riverbend.crs.web;

import edu.riverbend.crs.domain.Course;
import edu.riverbend.crs.domain.Section;
import edu.riverbend.crs.domain.Student;
import edu.riverbend.crs.domain.Term;
import edu.riverbend.crs.exceptions.ResourceNotFoundException;
import edu.riverbend.crs.repository.CourseRepository;
import edu.riverbend.crs.repository.EnrollmentRepository;
import edu.riverbend.crs.repository.SectionRepository;
import edu.riverbend.crs.repository.StudentRepository;
import edu.riverbend.crs.repository.TermRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class CatalogController {
    private final CourseRepository courses;
    private final TermRepository terms;
    private final StudentRepository students;
    private final SectionRepository sections;
    private final EnrollmentRepository enrollments;

    public CatalogController(CourseRepository courses, TermRepository terms,
                             StudentRepository students, SectionRepository sections,
                             EnrollmentRepository enrollments) {
        this.courses = courses;
        this.terms = terms;
        this.students = students;
        this.sections = sections;
        this.enrollments = enrollments;
    }

    public record CourseInput(@NotBlank @Size(max = 255) String code, List<Long> prerequisiteIds) { }
    public record CourseView(Long id, String code, List<Long> prerequisiteIds) { }
    public record TermInput(@NotBlank String name, @NotNull LocalDateTime registrationOpens,
                            @NotNull LocalDateTime registrationCloses) { }
    public record TermView(Long id, String name, LocalDateTime registrationOpens,
                           LocalDateTime registrationCloses) { }
    public record StudentInput(@NotBlank String name) { }
    public record StudentView(Long id, String name) { }
    public record SectionInput(@NotNull Long courseId, @NotNull Long termId, @Min(1) int capacity) { }
    public record SectionView(Long id, Long courseId, Long termId, int capacity,
                              int seatsTaken, Long version) { }
    public record StudentEnrollmentView(Long id, Long sectionId, String status) { }

    private CourseView view(Course c) {
        return new CourseView(c.id, c.code, c.prerequisites.stream().map(p -> p.id).sorted().toList());
    }
    private TermView view(Term t) {
        return new TermView(t.id, t.name, t.registrationOpens, t.registrationCloses);
    }
    private StudentView view(Student s) { return new StudentView(s.id, s.name); }
    private SectionView view(Section s) {
        return new SectionView(s.id, s.course.id, s.term.id, s.capacity, s.seatsTaken, s.version);
    }

    @GetMapping("/courses")
    public List<CourseView> courses() { return courses.findAll().stream().map(this::view).toList(); }

    @GetMapping("/courses/{id}")
    public CourseView course(@PathVariable Long id) {
        return view(courses.findById(id).orElseThrow(() -> new ResourceNotFoundException("Course not found.")));
    }

    @PostMapping("/courses") @ResponseStatus(HttpStatus.CREATED)
    public CourseView createCourse(@Valid @RequestBody CourseInput input) {
        Course course = new Course();
        course.code = input.code();
        if (input.prerequisiteIds() != null) {
            for (Long id : input.prerequisiteIds()) {
                course.prerequisites.add(courses.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Prerequisite course not found.")));
            }
        }
        return view(courses.save(course));
    }

    @GetMapping("/terms")
    public List<TermView> terms() { return terms.findAll().stream().map(this::view).toList(); }

    @GetMapping("/terms/{id}")
    public TermView term(@PathVariable Long id) {
        return view(terms.findById(id).orElseThrow(() -> new ResourceNotFoundException("Term not found.")));
    }

    @PostMapping("/terms") @ResponseStatus(HttpStatus.CREATED)
    public TermView createTerm(@Valid @RequestBody TermInput input) {
        if (!input.registrationOpens().isBefore(input.registrationCloses())) {
            throw new IllegalArgumentException("Registration opens must precede registration closes.");
        }
        Term term = new Term();
        term.name = input.name();
        term.registrationOpens = input.registrationOpens();
        term.registrationCloses = input.registrationCloses();
        return view(terms.save(term));
    }

    @GetMapping("/students")
    public List<StudentView> students() { return students.findAll().stream().map(this::view).toList(); }

    @GetMapping("/students/{id}")
    public StudentView student(@PathVariable Long id) {
        return view(students.findById(id).orElseThrow(() -> new ResourceNotFoundException("Student not found.")));
    }

    @PostMapping("/students") @ResponseStatus(HttpStatus.CREATED)
    public StudentView createStudent(@Valid @RequestBody StudentInput input) {
        Student student = new Student();
        student.name = input.name();
        return view(students.save(student));
    }

    @GetMapping("/sections")
    public List<SectionView> sections() { return sections.findAll().stream().map(this::view).toList(); }

    @GetMapping("/sections/{id}")
    public SectionView section(@PathVariable Long id) {
        return view(sections.findById(id).orElseThrow(() -> new ResourceNotFoundException("Section not found.")));
    }

    @PostMapping("/sections") @ResponseStatus(HttpStatus.CREATED)
    public SectionView createSection(@Valid @RequestBody SectionInput input) {
        Section section = new Section();
        section.course = courses.findById(input.courseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course not found."));
        section.term = terms.findById(input.termId())
                .orElseThrow(() -> new ResourceNotFoundException("Term not found."));
        section.capacity = input.capacity();
        return view(sections.save(section));
    }

    @GetMapping("/students/{id}/enrollments")
    public List<StudentEnrollmentView> studentEnrollments(@PathVariable Long id) {
        if (!students.existsById(id)) throw new ResourceNotFoundException("Student not found.");
        return enrollments.findByStudentId(id).stream()
                .map(e -> new StudentEnrollmentView(e.id, e.section.id, e.status.name())).toList();
    }
}
