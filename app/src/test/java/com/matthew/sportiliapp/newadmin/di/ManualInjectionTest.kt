package com.matthew.sportiliapp.newadmin.di

import com.matthew.sportiliapp.newadmin.FakeFirebaseRepository
import org.junit.Assert.assertSame
import org.junit.Test

class ManualInjectionTest {

    @Test
    fun `override repository for testing is exposed through provider`() {
        val fakeRepository = FakeFirebaseRepository()

        ManualInjection.overrideRepositoryForTesting(fakeRepository)
        try {
            assertSame(fakeRepository, ManualInjection.firebaseRepository)
        } finally {
            ManualInjection.resetOverrides()
        }
    }
}
