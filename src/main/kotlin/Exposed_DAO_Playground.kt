import org.jetbrains.exposed.dao.*
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.transactions.transactionManager

// Levels table and dao class
object Levels : IntIdTable() {
    val name: Column<String> = varchar("name", 50)
}

class Level(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Level>(Levels)

    var name by Levels.name
    val students by Student referrersOn Students.level
}


// Student table and dao class
object Students : IntIdTable() {
    val name: Column<String> = varchar("name", 250)
    val level = reference("level", Levels)
}

class Student(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Student>(Students)

    var name by Students.name
    var level by Level referencedOn Students.level
    var courses by Course via CoursesStudents
}


// Courses table and dao class

object Courses : IntIdTable() {
    val name: Column<String> = varchar("name", 300)
    val description: Column<String> = varchar("description", 500)
    val lecturer: Column<String> = varchar("lecturer", 250)

}

class Course(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Course>(Courses)

    var name by Courses.name
    var description by Courses.description
    var lecturer by Courses.lecturer
    var students by Student via CoursesStudents

}

// many to many relation
// CoursesStudents table and dao class
object CoursesStudents : Table() {
    var course = reference("course", Courses)
    var student = reference("student", Students)

    override val primaryKey: PrimaryKey = PrimaryKey(course, student)
}


fun main() {
    val db = Database.connect(
        "jdbc:h2:mem:regular",
        driver = "org.h2.Driver",
        user = "root",
        password = "",
        databaseConfig = DatabaseConfig {
            useNestedTransactions = true
        })
    transaction {
        SchemaUtils.create(Levels, Students, Courses, CoursesStudents)
        val level1 = transaction {
            // Level setup
            Level.new {
                name = "Undergrad CS"
            }
        }
        // Students setup
        val student1 = transaction {
            Student.new {
                name = "Dali"
                level = level1
            }
        }

        val student2 = transaction {
            Student.new {
                name = "Hama"
                level = level1
            }
        }

        // Courses setup
        val course1 = transaction {
            Course.new {
                name = "Cryptography"
                lecturer = "John doe"
                description= "Learning symmetric and asymmetric crypto systems"
            }
        }
        course1.students = SizedCollection(listOf<Student>(student1, student2))
        val course2 = transaction {
            Course.new {
                name = "Koltin"
                lecturer = "Jetbrain"
                description= "Learning in depth the Kotlin language"
            }
        }
        course2.students = SizedCollection(listOf<Student>(student1))
        val course3 = transaction {
            Course.new {
                name = "Jetbrains - Exposed"
                lecturer = "Tapac"
                description= "Hands-on experience with the Exposed ORM"
            }
        }
        course3.students = SizedCollection(listOf<Student>(student2))

        println("Levels: ")
        for (level in Level.all()) {
            println("${level.id}: ${level.name} with ${level.students.count()} students (${level.students.joinToString{student -> "${student.name}"  }})")
        }

        println("Students: ")
        for (student in Student.all()) {
            println("${student.id}: ${student.name} with ${student.courses.count()} courses (${student.courses.joinToString{course -> "${course.name}"  }})")
        }

        println("Courses: ")
        for (course in Course.all()) {
            println("${course.id}: ${course.name} with ${course.students.count()} students (${course.students.joinToString{student -> "${student.name}"  }})" )
        }
        //SchemaUtils.drop(Levels, Students, Courses, CoursesStudents)

    }
}