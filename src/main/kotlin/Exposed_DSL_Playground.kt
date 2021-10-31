
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction


object Users: Table(){
    val id:Column<Int> = integer("id").autoIncrement()
    val name: Column<String> = varchar("name", 250)

    override  val primaryKey = PrimaryKey(id)
}

object Commits: Table(){
    val id: Column<Int> = integer("id").autoIncrement()
    val message: Column<String> = varchar("message", 250)
    val description: Column<String> = text("description")
    val userId:Column<Int> = integer("user_id") references Users.id
    override val primaryKey = PrimaryKey(id)
}

object Files: Table(){
    val id: Column<Int> = integer("id").autoIncrement()
    val name: Column<String> = varchar("name", 250)

    override val primaryKey = PrimaryKey(id)
}

object FilesCommits: Table(){
    val id:Column<Int> = integer("id").autoIncrement()
    val commitId: Column<Int> = integer("commit_id") references Commits.id
    val fileId: Column<Int> = integer("file_id") references Files.id
    override val primaryKey: PrimaryKey = PrimaryKey(arrayOf(commitId,fileId, id))
}

fun main(){
    Database.connect("jdbc:h2:mem:regular", driver = "org.h2.Driver", user = "root", password = "")

    transaction {
        // addLogger(StdOutSqlLogger)

        SchemaUtils.create (Users,Files,Commits,FilesCommits)
        // Users setup
        val daliId = Users.insert{
            it[name] = "Dali"
        }[Users.id]

        // Files Setup
        val firstFileId= Files.insert {
            it[name]= "Kotlin_playground.kt"
        }[Files.id]
        val secondFileId= Files.insert {
            it[name]= "exposed_playground.kt"
        }[Files.id]

        // Commit setup
        val initialCommitId = Commits.insert {
            it[message]= "Initial commit"
            it[description] = "the initial commit of two playground files"
            it[userId] = daliId
        }[Commits.id]
        val secondCommitId = Commits.insert {
            it[message] = "Changed a file title"
            it[description] = "Updated a file title for better visibility"
            it[userId] = daliId
        }[Commits.id]

        // Setup links between commits and file
        FilesCommits.insert {
            it[fileId] = firstFileId
            it[commitId] = initialCommitId
        }
        FilesCommits.insert {
            it[fileId] = secondFileId
            it[commitId] = initialCommitId
        }
        FilesCommits.insert {
            it[fileId] = secondFileId
            it[commitId] = secondCommitId
        }

        println("Users: ")
        for (user in Users.selectAll()) {
            println("\t ${user[Users.name]}")
        }
        println("Commits: ")
        for (commit in Commits.selectAll()) {
            println("\t ${commit[Commits.message]}: ${commit[Commits.description]}")
        }

        println("Files: ")
        for (file in Files.selectAll()) {
            println("\t ${file[Files.id]}: ${file[Files.name]}")
        }

        println("Files Commits: ")
        for (fileCommit in FilesCommits.selectAll()) {
            println("\t ${fileCommit[FilesCommits.fileId]}: ${fileCommit[FilesCommits.commitId]}")
        }

        // Where expression
        println("Files that starts with keyword = 'exposed' ")
        for(file in Files.select{ Files.name like "exposed%"}){
            println("\t ${file[Files.id]}: ${file[Files.name]}")
        }
        // Count expr
        println("Commits number : ${Commits.selectAll().count()}")
        // Order by
        println("Commits ordered by description:")
        for (commit in Commits.selectAll().orderBy(Commits.description)) {
            println("\t ${commit[Commits.message]}: ${commit[Commits.description]}")
        }
        // Group by
        println("Commits counted and grouped by userId")
        for (commit in Commits.slice(Commits.id.count(), Commits.userId).selectAll().groupBy(Commits.userId)) {
            println("\t UserId ${commit[Commits.userId]} -> ${commit[Commits.id.count()]} Commits")
        }

        // Join expr

        println("Commits messages with correspondent username:")
        for (commit in Commits.join(Users, JoinType.INNER, additionalConstraint = {Users.id eq Commits.userId}).selectAll()){
            println(commit)
            println("\t ${commit[Users.name]}: ${commit[Commits.message]}")
        }
        // Union

        println("Union between file name and commit messages")
        for( record in (Files.slice(Files.name).selectAll().unionAll(Commits.slice(Commits.message).selectAll()))){
            println("\t ${record[Files.name]}")
        }
        SchemaUtils.drop (Users, Commits, Files, FilesCommits)
    }
}