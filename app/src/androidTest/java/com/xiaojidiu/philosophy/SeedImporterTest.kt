package com.xiaojidiu.philosophy
import androidx.test.core.app.ApplicationProvider
import com.xiaojidiu.philosophy.data.local.*
import com.xiaojidiu.philosophy.data.repository.LocalRepository
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.Test
import org.junit.Assert.*
class SeedImporterTest: DatabaseTestBase() {
    @Test fun importsCompleteContentAndUpdatePreservesHistory() = runBlocking {
        val context=ApplicationProvider.getApplicationContext<android.content.Context>()
        val importer=SeedImporter(context,db,PreferencesRepository(context));importer.import()
        val repo=LocalRepository(db);val library=repo.library.first()
        assertTrue(library.questions.size>=50);assertTrue(library.philosophers.size>=20);assertEquals(15,library.schools.size)
        val seeded=library.questions.filter {it.id!="q"}
        val q=seeded.first();val id=repo.answer(q,q.options.first().id)
        importer.import();assertEquals(id,repo.journal.first().answers.first().id)
        assertTrue(seeded.all {it.options.size in 2..5 && it.perspectives.isNotEmpty()})
        assertEquals(13,library.questions.flatMap {it.options}.flatMap {it.weights.keys}.distinct().size)
    }
}
