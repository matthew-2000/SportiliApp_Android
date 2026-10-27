package com.matthew.sportiliapp.scheda

import android.graphics.Color as AndroidColor
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.MutableLiveData
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import coil.ImageLoader
import coil.compose.LocalImageLoader
import coil.decode.DataSource
import coil.intercept.Interceptor
import coil.request.SuccessResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import com.matthew.sportiliapp.S04PreviewActivity
import com.matthew.sportiliapp.model.Esercizio
import com.matthew.sportiliapp.model.Giorno
import com.matthew.sportiliapp.model.GruppoMuscolare
import com.matthew.sportiliapp.model.Scheda
import com.matthew.sportiliapp.ui.theme.SportiliAppTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Exercises the real day screen with distinct local images, without network access. */
@RunWith(AndroidJUnit4::class)
class DayExerciseImagesTest {
    @get:Rule val compose = createAndroidComposeRule<S04PreviewActivity>()

    private val names = (0..11).map { "Esercizio $it" }
    private val colors = names.mapIndexed { index, name ->
        name to AndroidColor.rgb(30 + index * 17, 220 - index * 13, 50 + index * 11)
    }.toMap()
    @Volatile private var blockedName: String? = null
    private val blockedLoad = CompletableDeferred<Unit>()

    private fun publishGroups(groups: Map<String, GruppoMuscolare>) {
        (compose.activity.fixtureModel.scheda as MutableLiveData).value = Scheda(
            dataInizio = "", durata = 0,
            giorni = mapOf("day-z" to Giorno("Giorno immagini", groups))
        )
    }

    private fun showDay(groups: Map<String, GruppoMuscolare>) {
        compose.runOnUiThread {
            val activity = compose.activity
            val images = ImageLoader.Builder(activity).components {
                add(Interceptor { chain ->
                    val name = names.single { exerciseImageUrl(it) == chain.request.data }
                    if (name == blockedName) blockedLoad.await()
                    delay(75)
                    val bitmap = Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888).apply {
                        eraseColor(colors.getValue(name))
                    }
                    SuccessResult(BitmapDrawable(activity.resources, bitmap), chain.request, DataSource.NETWORK)
                })
            }.build()
            publishGroups(groups)
            activity.setContent {
                CompositionLocalProvider(LocalImageLoader provides images) {
                    SportiliAppTheme {
                        GiornoScreen(rememberNavController(), "day-z", activity.fixtureModel)
                    }
                }
            }
        }
        compose.waitForIdle()
    }

    private fun imagePixelCount(rowName: String, imageName: String): Int {
        val pixels = compose.onNode(hasText(rowName).and(hasClickAction()))
            .captureToImage().toPixelMap()
        val expected = colors.getValue(imageName)
        var matchingPixels = 0
        for (y in 0 until pixels.height) {
            for (x in 0 until pixels.width) {
                if (pixels[x, y].toArgb() == expected) matchingPixels++
            }
        }
        return matchingPixels
    }

    private fun assertRowImage(name: String) {
        // Compose becoming idle does not imply that an asynchronous image has loaded.
        compose.waitUntil(timeoutMillis = 5_000) { imagePixelCount(name, name) > 100 }
    }

    @Test fun eachRowKeepsItsOwnImageWhenGroupsAreRecycledByScrolling() {
        showDay(names.mapIndexed { index, name ->
            "group-$index" to GruppoMuscolare("Gruppo $index", mapOf("exercise" to Esercizio(name, "3 × 10")))
        }.toMap())

        listOf(0, 3, 7, 11, 6, 2, 0, 11, 0).forEach { index ->
            compose.onNode(hasScrollToIndexAction()).performScrollToIndex(index)
            compose.waitForIdle()
            assertRowImage(names[index])
        }
    }

    @Test fun adjacentExercisesInTheSameGroupHaveDifferentImages() {
        showDay(mapOf("group" to GruppoMuscolare("Gruppo", names.take(2).mapIndexed { index, name ->
            "exercise-$index" to Esercizio(name, "3 × 10")
        }.toMap())))

        names.take(2).forEach(::assertRowImage)
    }

    @Test fun supersetPartsHaveDistinctImages() {
        showDay(mapOf("group" to GruppoMuscolare("Gruppo", mapOf(
            "exercise" to Esercizio(names.take(3).joinToString(" + "), "3 × 10")
        ))))

        names.take(3).forEach(::assertRowImage)
    }

    @Test fun changedExerciseDoesNotKeepThePreviousImageWhileLoading() {
        fun groups(name: String) = mapOf("group" to GruppoMuscolare("Gruppo", mapOf(
            "exercise" to Esercizio(name, "3 × 10")
        )))
        showDay(groups(names[0]))
        assertRowImage(names[0])

        blockedName = names[1]
        compose.runOnUiThread { publishGroups(groups(names[1])) }
        compose.waitForIdle()
        assertTrue("The previous exercise's image must disappear", imagePixelCount(names[1], names[0]) == 0)

        blockedLoad.complete(Unit)
        assertRowImage(names[1])
    }
}
