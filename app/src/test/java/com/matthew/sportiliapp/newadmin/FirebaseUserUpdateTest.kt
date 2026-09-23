package com.matthew.sportiliapp.newadmin

import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.matthew.sportiliapp.model.Scheda
import com.matthew.sportiliapp.model.Utente
import kotlinx.coroutines.test.runTest
import newadmin.data.FirebaseRepositoryImpl
import org.junit.Assert.*
import org.junit.Test
import org.mockito.ArgumentMatchers.anyMap
import org.mockito.Mockito.*

class FirebaseUserUpdateTest {
    @Test
    fun `profile update preserves history current workout and unknown fields`() = runTest {
        val database = mock(FirebaseDatabase::class.java)
        val users = mock(DatabaseReference::class.java)
        val user = mock(DatabaseReference::class.java)
        `when`(database.getReference("users")).thenReturn(users)
        `when`(database.getReference("alerts")).thenReturn(mock(DatabaseReference::class.java))
        `when`(database.getReference("workoutIssueReports")).thenReturn(mock(DatabaseReference::class.java))
        `when`(users.child("abc")).thenReturn(user)

        val history = mapOf("squat" to mapOf("noteUtente" to "Keep", "weightLogs" to mapOf("log1" to 42)))
        val currentWorkout = mapOf("dataInizio" to "2026-09-01T00:00:00+0000", "cambioRichiesto" to true)
        val stored = mutableMapOf<String, Any?>(
            "nome" to "Old", "cognome" to "Name", "exerciseData" to history,
            "scheda" to currentWorkout, "futureField" to "keep"
        )
        @Suppress("UNCHECKED_CAST")
        val task = mock(Task::class.java) as Task<Void>
        doAnswer { invocation ->
            stored.putAll(invocation.getArgument<Map<String, Any?>>(0))
            task
        }.`when`(user).updateChildren(anyMap<String, Any>())
        doAnswer { invocation ->
            invocation.getArgument<OnSuccessListener<Void>>(0).onSuccess(null)
            task
        }.`when`(task).addOnSuccessListener(any())
        `when`(task.addOnFailureListener(any())).thenReturn(task)

        val result = FirebaseRepositoryImpl(database).updateUser(
            Utente("abc", "Corrected", "New", Scheda("old snapshot", 1))
        )

        assertTrue(result.isSuccess)
        assertEquals("New", stored["nome"])
        assertEquals("Corrected", stored["cognome"])
        assertEquals(history, stored["exerciseData"])
        assertEquals(currentWorkout, stored["scheda"])
        assertEquals("keep", stored["futureField"])
        verify(user).updateChildren(mapOf("nome" to "New", "cognome" to "Corrected"))
        verify(user, never()).setValue(any())
    }

    @Test
    fun `profile write failure is returned to the caller`() = runTest {
        val database = mock(FirebaseDatabase::class.java)
        val users = mock(DatabaseReference::class.java)
        val user = mock(DatabaseReference::class.java)
        `when`(database.getReference("users")).thenReturn(users)
        `when`(database.getReference("alerts")).thenReturn(mock(DatabaseReference::class.java))
        `when`(database.getReference("workoutIssueReports")).thenReturn(mock(DatabaseReference::class.java))
        `when`(users.child("abc")).thenReturn(user)
        @Suppress("UNCHECKED_CAST")
        val task = mock(Task::class.java) as Task<Void>
        `when`(user.updateChildren(anyMap<String, Any>())).thenReturn(task)
        `when`(task.addOnSuccessListener(any())).thenReturn(task)
        val error = IllegalStateException("Permission denied")
        doAnswer { invocation ->
            invocation.getArgument<OnFailureListener>(0).onFailure(error)
            task
        }.`when`(task).addOnFailureListener(any())

        val result = FirebaseRepositoryImpl(database).updateUser(Utente("abc", "Surname", "Name"))

        assertSame(error, result.exceptionOrNull())
    }
}
